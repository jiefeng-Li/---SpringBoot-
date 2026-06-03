package com.cuit.interviewsystem.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cuit.interviewsystem.exception.BusinessException;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.mapper.CompanyMapper;
import com.cuit.interviewsystem.mapper.JobApplicationMapper;
import com.cuit.interviewsystem.model.dto.job.UpdateJobDto;
import com.cuit.interviewsystem.model.dto.job.AddJobDto;
import com.cuit.interviewsystem.model.dto.job.JobSearchPageDto;
import com.cuit.interviewsystem.model.entity.Company;
import com.cuit.interviewsystem.model.entity.JobApplication;
import com.cuit.interviewsystem.model.entity.JobPosition;
import com.cuit.interviewsystem.model.entity.User;
import com.cuit.interviewsystem.model.enums.CompanyStatusEnum;
import com.cuit.interviewsystem.model.enums.JobApplicationStatusEnum;
import com.cuit.interviewsystem.model.enums.JobPositionStatusEnum;
import com.cuit.interviewsystem.model.enums.JobTypeEnum;
import com.cuit.interviewsystem.service.JobPositionService;
import com.cuit.interviewsystem.service.JobRecommendationService;
import com.cuit.interviewsystem.mapper.JobPositionMapper;
import com.cuit.interviewsystem.utils.JWTUtil;
import com.cuit.interviewsystem.utils.RedisUtil;
import com.cuit.interviewsystem.utils.ThrowUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
* @author jiefe
* @description 针对表【t_job_position(职位发布表)】的数据库操作Service实现
* @createDate 2026-02-10 21:44:35
*/
@Slf4j
@Service
public class JobPositionServiceImpl extends ServiceImpl<JobPositionMapper, JobPosition>
    implements JobPositionService{
    @Resource
    private JobPositionMapper jobPositionMapper;
    @Resource
    private JobApplicationMapper jobApplicationMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private JWTUtil jwtUtil;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private JobRecommendationService jobRecommendationService;

    @Override
    public JobPosition getJobPositionById(Long id) {
        ThrowUtil.throwIfTrue(id == null, ErrorEnum.PARAMS_ERROR);
        return jobPositionMapper.selectById(id);
    }

    @Override
    public Page<JobPosition> getJobPositionList(JobSearchPageDto dto) {
        Page<JobPosition> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<JobPosition> lqw = new LambdaQueryWrapper<JobPosition>()
                .eq(dto.getCompanyId() != null, JobPosition::getCompanyId, dto.getCompanyId())
                .eq(StrUtil.isNotBlank(dto.getWorkCity()), JobPosition::getWorkCity, dto.getWorkCity())
                .eq(dto.getJobType() != null, JobPosition::getJobType, dto.getJobType())
                .eq(dto.getStatus() != null, JobPosition::getStatus, dto.getStatus())
                .eq(dto.getExperience() != null, JobPosition::getExperience, dto.getExperience())
                .eq(dto.getHiringManagerId() != null, JobPosition::getHiringManagerId, dto.getHiringManagerId())
                .eq(dto.getEducation() != null, JobPosition::getEducation, dto.getEducation())
                .ge(dto.getMinSalary() != null, JobPosition::getMinSalary, dto.getMinSalary())
                .le(dto.getMaxSalary() != null, JobPosition::getMaxSalary, dto.getMaxSalary())
                .eq(JobPosition::getIsDeleted, 0);
        if (dto.getTags() != null)
            dto.getTags().forEach(tag -> lqw.like(JobPosition::getTags, "\"" + tag + "\""));
        if (dto.getKeyword() != null)
            lqw.and(lqw1 -> lqw1.like(JobPosition::getTitle, dto.getKeyword()).or().like(JobPosition::getTags, "\"" + dto.getKeyword()+ "\""));
        lqw.orderByDesc(JobPosition::getCreateTime, JobPosition::getUpdateTime);
        lqw.orderBy(true, !dto.getDescByCreateTime(), JobPosition::getCreateTime)
                        .orderBy(true, !dto.getDescByPublishTime(), JobPosition::getPublishTime)
                        .orderBy(dto.getDescByHeadCount() != null, Boolean.FALSE.equals(dto.getDescByHeadCount()), JobPosition::getHeadcount);
        jobPositionMapper.selectPage(page, lqw);
        return page;
    }

    @Override
    /**
     * 添加职位方法
     * @param addJobDto 添加职位的数据传输对象，包含职位的基本信息
     * @return 返回插入数据库后的职位ID
     * @throws ErrorEnum.PARAMS_ERROR 当参数不符合要求时抛出，包括以下情况：
     *      1. 用户未绑定公司
     *      2. 公司未认证
     *      3. 公司被封禁
     *      4. 工作性质设置错误
     *      5. 职位状态设置错误
     */
    public int addJobPosition(AddJobDto addJobDto) {
        // 从JWT令牌中解析出当前登录用户信息
        User recruiter = jwtUtil.parseLoginUser();
        // 检查用户是否绑定了公司，如果没有则抛出异常
        ThrowUtil.throwIfTrue(recruiter.getCompanyId() == null, ErrorEnum.PARAMS_ERROR, "请先绑定公司");
        // 根据公司ID查询公司信息
        Company comp = companyMapper.selectById(recruiter.getCompanyId());
        // 检查公司是否处于审核中状态，如果是则抛出异常
        ThrowUtil.throwIfTrue(comp.getStatus() == CompanyStatusEnum.REVIEWING.getStatus(),
                ErrorEnum.PARAMS_ERROR, "公司未认证，无法发布职位");
        // 检查公司是否被封禁状态，如果是则抛出异常
        ThrowUtil.throwIfTrue(comp.getStatus() == CompanyStatusEnum.BANED.getStatus(),
                ErrorEnum.PARAMS_ERROR, "公司已被封禁，无法发布职位");
        // 创建新的职位对象
        JobPosition jobPosition = new JobPosition();
        // 将数据传输对象的属性复制到职位对象
        BeanUtils.copyProperties(addJobDto, jobPosition);
        // 设置职位的公司ID为当前用户的公司ID
        jobPosition.setCompanyId(recruiter.getCompanyId());

        JobTypeEnum jobTypeEnum = JobTypeEnum.getEnum(addJobDto.getJobType());
        JobPositionStatusEnum status = JobPositionStatusEnum.getEnum(addJobDto.getStatus());
        // 检查职位类型是否有效，无效则抛出异常
        ThrowUtil.throwIfTrue(jobTypeEnum == null, ErrorEnum.PARAMS_ERROR, "工作性质设置错误");
        // 检查职位状态是否有效，只能为草稿或招聘中，否则抛出异常
        ThrowUtil.throwIfTrue(status == null || !status.equals(JobPositionStatusEnum.DRAFT) && !status.equals(JobPositionStatusEnum.RECRUITING), ErrorEnum.PARAMS_ERROR, "职位状态设置错误");
        // 如果职位标签不为空，则将标签列表转换为JSON字符串并设置
        if (addJobDto.getTags() != null) {
            jobPosition.setTags(JSONUtil.toJsonStr(addJobDto.getTags()));
        }
        // 设置职位的招聘HR为当前用户ID
        jobPosition.setHiringManagerId(recruiter.getUserId());
        // 将职位信息插入数据库
        int inserted = jobPositionMapper.insert(jobPosition);
        if (inserted > 0) {
            jobRecommendationService.syncJobEmbedding(jobPosition);
        }
        return inserted;
    }

    @Override
    public int updateJobStatus(Long id, Integer status) {
        JobPosition job = jobPositionMapper.selectById(id);
        User curUser = jwtUtil.parseLoginUser();
        ThrowUtil.throwIfTrue(job == null || job.getIsDeleted() == 1, ErrorEnum.NOT_FOUND_ERROR);
        ThrowUtil.throwIfTrue(!Objects.equals(job.getCompanyId(), curUser.getCompanyId()), ErrorEnum.UNAUTHORIZED);
        JobPositionStatusEnum statusEnum = JobPositionStatusEnum.getEnum(status);
        ThrowUtil.throwIfTrue(statusEnum == null, ErrorEnum.PARAMS_ERROR, "职位状态设置错误");
        ThrowUtil.throwIfTrue(job.getStatus().equals(JobPositionStatusEnum.CLOSED),
                ErrorEnum.PARAMS_ERROR, "职位已关闭，无法修改状态");
        if (job.getStatus().equals(JobPositionStatusEnum.RECRUITING)) {
            ThrowUtil.throwIfTrue(statusEnum.equals(JobPositionStatusEnum.DRAFT),
                    ErrorEnum.PARAMS_ERROR, "职位已发布，无法设置为草稿");
        }
        if (job.getStatus().equals(JobPositionStatusEnum.DRAFT)) {
            ThrowUtil.throwIfTrue(statusEnum.equals(JobPositionStatusEnum.CLOSED),
                    ErrorEnum.PARAMS_ERROR, "职位为草稿，无法设置为关闭");
        }
        job.setStatus(statusEnum);
        if (statusEnum.equals(JobPositionStatusEnum.RECRUITING) && job.getPublishTime() == null)
            job.setPublishTime(LocalDate.now());
        job.setUpdateTime(LocalDate.now());
        return jobPositionMapper.updateById(job);
    }

    @Override
    public int updateJobPosition(UpdateJobDto updateJobDto, Long id) {
        JobPosition target = jobPositionMapper.selectById(id);
        User curUser = jwtUtil.parseLoginUser();
        ThrowUtil.throwIfTrue(target == null || target.getIsDeleted() == 1, ErrorEnum.NOT_FOUND_ERROR);
        ThrowUtil.throwIfTrue(!Objects.equals(target.getCompanyId(), curUser.getCompanyId()), ErrorEnum.UNAUTHORIZED);
        BeanUtils.copyProperties(updateJobDto, target);
        JobTypeEnum jobTypeEnum = JobTypeEnum.getEnum(updateJobDto.getJobType());
        ThrowUtil.throwIfTrue(jobTypeEnum == null, ErrorEnum.PARAMS_ERROR, "工作性质设置错误");
        if (updateJobDto.getTags() != null) {
            target.setTags(JSONUtil.toJsonStr(updateJobDto.getTags()));
        }
        target.setJobType(jobTypeEnum);
        target.setUpdateTime(LocalDate.now());
        int updated = jobPositionMapper.updateById(target);
        if (updated > 0) {
            jobRecommendationService.syncJobEmbedding(target);
        }
        return updated;
    }

    @Override
    public int deleteJobPosition(Long id) {
        User curUser = jwtUtil.parseLoginUser();
        JobPosition target = jobPositionMapper.selectById(id);
        ThrowUtil.throwIfTrue(target == null || target.getIsDeleted() == 1,
                ErrorEnum.NOT_FOUND_ERROR, "职位不存在或已被删除");
        ThrowUtil.throwIfTrue(!Objects.equals(target.getCompanyId(), curUser.getCompanyId()), ErrorEnum.UNAUTHORIZED);
        if (jobApplicationMapper.exists(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getJobPositionId, id)
                .eq(JobApplication::getIsDeleted, 0)
                .notIn(JobApplication::getStatus,
                        JobApplicationStatusEnum.ELIMINATED.getStatus(),
                        JobApplicationStatusEnum.HIRED.getStatus(),
                        JobApplicationStatusEnum.PENDING.getStatus(),
                        JobApplicationStatusEnum.HIRED.getStatus()))) {
            throw new BusinessException(ErrorEnum.PARAMS_ERROR, "有待处理的申请，无法删除");
        }
        target.setIsDeleted(1);
        int updated = jobPositionMapper.updateById(target);
        if (updated > 0) {
            jobRecommendationService.deleteJobEmbedding(id);
        }
        return updated;
    }

    @Override
    /**
     * 查看职位，增加浏览次数
     * 使用Redis进行原子性计数，提高并发性能
     */
    public void viewJobPosition(Long jobId) {
        // 首先检查职位是否存在且状态合法
        JobPosition jobPosition = jobPositionMapper.selectById(jobId);
        ThrowUtil.throwIfTrue(jobPosition == null || jobPosition.getIsDeleted() == 1, ErrorEnum.NOT_FOUND_ERROR);
        ThrowUtil.throwIfTrue(jobPosition.getStatus() == JobPositionStatusEnum.DRAFT, ErrorEnum.NOT_FOUND_ERROR, "职位未发布");
        ThrowUtil.throwIfTrue(jobPosition.getStatus() == JobPositionStatusEnum.CLOSED, ErrorEnum.NOT_FOUND_ERROR, "职位已关闭");
        ThrowUtil.throwIfTrue(jobPosition.getStatus().equals(JobPositionStatusEnum.FULLY_RECRUITED), ErrorEnum.NOT_FOUND_ERROR, "职位已招满");
        
        // 使用Redis原子性递增操作，设置过期时间为24小时
        redisUtil.increment("viewCnt:" + jobId, 24, TimeUnit.HOURS);
        
        // 异步同步浏览次数到数据库
        syncViewCountToDatabase(jobId);
    }
    
    /**
     * 异步同步浏览次数到数据库
     * 使用@Async注解实现异步执行
     */
    @Async
    public void syncViewCountToDatabase(Long jobId) {
        try {
            // 获取Redis中的浏览次数
            Object viewCountObj = redisUtil.get("viewCnt:" + jobId);
            if (viewCountObj == null) {
                return;
            }
            
            Long redisViewCount = Long.valueOf(viewCountObj.toString());
            
            // 获取数据库中的当前浏览次数
            JobPosition jobPosition = jobPositionMapper.selectById(jobId);
            if (jobPosition == null) {
                return;
            }
            
            // 只有当Redis中的浏览次数大于数据库中的浏览次数时才更新
            if (redisViewCount > jobPosition.getViewCount()) {
                jobPosition.setViewCount(redisViewCount.intValue());
                jobPositionMapper.updateById(jobPosition);
            }
        } catch (Exception e) {
            // 记录日志，但不影响主流程
            log.error("同步职位浏览次数到数据库失败: jobId={}", jobId, e);
        }
    }
}




