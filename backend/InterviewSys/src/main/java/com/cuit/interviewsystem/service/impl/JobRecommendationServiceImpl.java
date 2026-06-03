package com.cuit.interviewsystem.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.cuit.interviewsystem.mapper.JobPositionMapper;
import com.cuit.interviewsystem.model.entity.JobPosition;
import com.cuit.interviewsystem.model.entity.User;
import com.cuit.interviewsystem.model.enums.JobPositionStatusEnum;
import com.cuit.interviewsystem.model.vo.JobPositionVo;
import com.cuit.interviewsystem.model.vo.ResumeEducationVo;
import com.cuit.interviewsystem.model.vo.ResumeExperienceVo;
import com.cuit.interviewsystem.model.vo.ResumeProjectVo;
import com.cuit.interviewsystem.model.vo.ResumeVo;
import com.cuit.interviewsystem.repository.JobVectorRepository;
import com.cuit.interviewsystem.service.JobRecommendationService;
import com.cuit.interviewsystem.service.ResumeService;
import com.cuit.interviewsystem.utils.JWTUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Service
public class JobRecommendationServiceImpl implements JobRecommendationService {

    private static final String EMBEDDING_MODEL_NAME = "text-embedding-v3";

    @Resource
    private JWTUtil jwtUtil;
    @Resource
    private ResumeService resumeService;
    @Resource
    private JobPositionMapper jobPositionMapper;
    @Resource
    private JobVectorRepository jobVectorRepository;
    @Resource
    private EmbeddingModel dashScopeEmbeddingModel;

/**
 * 为当前用户推荐职位的方法
 * @param limit 返回的职位数量限制，如果为null或小于等于0则使用默认值8，最大限制为50
 * @return 返回推荐职位列表，如果出现任何错误则返回空列表
 */
    @Override
    public List<JobPositionVo> recommendJobsForCurrentUser(Integer limit) {
        // 设置每页大小，如果limit为null或小于等于0则使用默认值8，但不超过50
        int pageSize = limit == null || limit <= 0 ? 8 : Math.min(limit, 50);
        try {
            // 解析登录用户信息
            User loginUser = jwtUtil.parseLoginUser();
            // 如果用户未登录，返回空列表
            if (loginUser == null) {
                return Collections.emptyList();
            }

            // 获取用户的默认简历
            ResumeVo defaultResume = resumeService.getDefaultResumeByUserId(loginUser.getUserId());
            // 如果用户没有默认简历，返回空列表
            if (defaultResume == null) {
                return Collections.emptyList();
            }

            // 构建简历文本内容
            String resumeText = buildResumeText(defaultResume);
            // 如果简历文本为空，返回空列表
            if (!StringUtils.hasText(resumeText)) {
                return Collections.emptyList();
            }

            // 将简历文本转换为向量表示
            float[] queryEmbedding = dashScopeEmbeddingModel.embed(resumeText);
            // 如果向量转换失败，返回空列表
            if (queryEmbedding == null || queryEmbedding.length == 0) {
                return Collections.emptyList();
            }

            // 根据向量相似度查找候选职位ID，获取5倍的pageSize数量的候选职位
            List<Long> candidateIds = jobVectorRepository.findTopJobIdsByEmbedding(toVectorLiteral(queryEmbedding), pageSize * 5);
            // 如果没有找到候选职位，返回空列表
            if (candidateIds == null || candidateIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 根据候选ID批量查询职位信息
            List<JobPosition> jobs = jobPositionMapper.selectBatchIds(candidateIds);
            // 如果查询结果为空，返回空列表
            if (CollUtil.isEmpty(jobs)) {
                return Collections.emptyList();
            }

            // 创建职位ID到排名的映射
            Map<Long, Integer> rankMap = new LinkedHashMap<>();
            for (int i = 0; i < candidateIds.size(); i++) {
                rankMap.put(candidateIds.get(i), i);
            }

            // 过滤并排序职位：只保留未删除且正在招聘的职位，按相似度排序
            List<JobPosition> sortedJobs = jobs.stream()
                    .filter(job -> job != null && job.getIsDeleted() != null && job.getIsDeleted() == 0)
                    .filter(job -> job.getStatus() == JobPositionStatusEnum.RECRUITING)
                    .sorted(Comparator.comparingInt(job -> rankMap.getOrDefault(job.getId(), Integer.MAX_VALUE)))
                    .limit(pageSize)
                    .toList();

            // 将职位对象转换为VO对象
            List<JobPositionVo> result = new ArrayList<>(sortedJobs.size());
            for (JobPosition job : sortedJobs) {
                JobPositionVo vo = JobPositionVo.objToVo(job);
                if (vo != null) {
                    result.add(vo);
                }
            }
            return result;
        } catch (Exception e) {
            // 捕获所有异常并记录日志，返回空列表
            log.error("获取职位推荐失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void syncJobEmbedding(JobPosition jobPosition) {
        if (jobPosition == null || jobPosition.getId() == null) {
            return;
        }
        try {
            String content = buildJobText(jobPosition);
            if (!StringUtils.hasText(content)) {
                return;
            }
            float[] embedding = dashScopeEmbeddingModel.embed(content);
            if (embedding == null || embedding.length == 0) {
                return;
            }
            jobVectorRepository.upsert(
                    jobPosition.getId(),
                    jobPosition.getCompanyId(),
                    jobPosition.getTitle(),
                    content,
                    toVectorLiteral(embedding),
                    EMBEDDING_MODEL_NAME
            );
        } catch (Exception e) {
            log.error("同步职位向量失败, jobId={}", jobPosition.getId(), e);
        }
    }

    @Override
    public void deleteJobEmbedding(Long jobId) {
        if (jobId == null) {
            return;
        }
        try {
            jobVectorRepository.deleteByJobId(jobId);
        } catch (Exception e) {
            log.error("删除职位向量失败, jobId={}", jobId, e);
        }
    }

    private String buildResumeText(ResumeVo resume) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, "期望工作城市", resume.getCity());
        appendPart(builder, "个人简介", resume.getSummary());

        if (CollUtil.isNotEmpty(resume.getEducations())) {
            for (ResumeEducationVo education : resume.getEducations()) {
                builder.append("教育经历：")
                        .append(nullSafe(education.getSchool())).append("，")
                        .append(nullSafe(education.getMajor())).append("，")
                        .append(nullSafe(education.getDegree())).append("，")
                        .append(nullSafe(education.getDescription())).append('\n');
            }
        }

        if (CollUtil.isNotEmpty(resume.getExperiences())) {
            for (ResumeExperienceVo experience : resume.getExperiences()) {
                builder.append("工作经历：")
                        .append(nullSafe(experience.getCompany())).append("，")
                        .append(nullSafe(experience.getPosition())).append("，")
                        .append(nullSafe(experience.getDescription())).append('\n');
            }
        }

        if (CollUtil.isNotEmpty(resume.getProjects())) {
            for (ResumeProjectVo project : resume.getProjects()) {
                builder.append("项目经历：")
                        .append(nullSafe(project.getName())).append("，")
                        .append(nullSafe(project.getDescription())).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private String buildJobText(JobPosition jobPosition) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, "职位标题", jobPosition.getTitle());
        appendPart(builder, "职位描述", jobPosition.getDescription());
        appendPart(builder, "职位要求", jobPosition.getRequirement());
        appendPart(builder, "工作城市", jobPosition.getWorkCity());
        appendPart(builder, "经验要求", jobPosition.getExperience());
        appendPart(builder, "学历要求", jobPosition.getEducation());

        if (StringUtils.hasText(jobPosition.getTags())) {
            try {
                List<String> tags = JSONUtil.toList(jobPosition.getTags(), String.class);
                if (CollUtil.isNotEmpty(tags)) {
                    builder.append("职位标签：")
                            .append(String.join("，", tags))
                            .append('\n');
                }
            } catch (Exception e) {
                builder.append("职位标签：").append(jobPosition.getTags()).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private void appendPart(StringBuilder builder, String label, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(label).append('：').append(value.trim()).append('\n');
        }
    }

    private String nullSafe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String toVectorLiteral(float[] embedding) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float value : embedding) {
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }
}