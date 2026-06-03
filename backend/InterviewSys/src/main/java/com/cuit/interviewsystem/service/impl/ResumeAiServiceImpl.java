package com.cuit.interviewsystem.service.impl;

import com.cuit.interviewsystem.ai.common.SystemPrompt;
import com.cuit.interviewsystem.exception.BusinessException;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.model.dto.ai.ResumeOptimizeRequestDto;
import com.cuit.interviewsystem.model.dto.ai.ResumeOptimizeResponseDto;
import com.cuit.interviewsystem.model.dto.resume.PreviewResumeDto;
import com.cuit.interviewsystem.model.entity.User;
import com.cuit.interviewsystem.model.enums.UserRoleEnum;
import com.cuit.interviewsystem.model.vo.ResumeEducationVo;
import com.cuit.interviewsystem.model.vo.ResumeExperienceVo;
import com.cuit.interviewsystem.model.vo.ResumeProjectVo;
import com.cuit.interviewsystem.model.vo.ResumeVo;
import com.cuit.interviewsystem.service.ResumeAiService;
import com.cuit.interviewsystem.service.ResumeService;
import com.cuit.interviewsystem.utils.JWTUtil;
import com.cuit.interviewsystem.utils.ThrowUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Locale;
import java.util.Objects;

@Service
public class ResumeAiServiceImpl implements ResumeAiService {

    @Resource
    private ChatClient dashScopeChatClient;

    @Resource
    private ResumeService resumeService;

    @Resource
    private JWTUtil jwtUtil;

    @Override
    public ResumeOptimizeResponseDto optimizeResumeModule(ResumeOptimizeRequestDto requestDto) {
        OptimizeContext context = buildOptimizeContext(requestDto);
        String optimized = dashScopeChatClient.prompt()
                .system(SystemPrompt.RESUME_PROMPT)
                .user(context.prompt)
                .call()
                .content();

        ResumeOptimizeResponseDto res = new ResumeOptimizeResponseDto();
        res.setResumeId(requestDto.getResumeId());
        res.setDraftMode(context.draftMode);
        res.setModuleType(context.moduleType);
        res.setModuleId(context.moduleId);
        res.setModuleIndex(context.moduleIndex);
        res.setOriginalContent(context.moduleContent);
        res.setOptimizedContent(optimized);
        return res;
    }

    @Override
    public Flux<String> optimizeResumeModuleStream(ResumeOptimizeRequestDto requestDto) {
        OptimizeContext context = buildOptimizeContext(requestDto);
        return dashScopeChatClient.prompt()
                .system(SystemPrompt.RESUME_PROMPT)
                .user(context.prompt)
                .stream()
                .content();
    }

    private OptimizeContext buildOptimizeContext(ResumeOptimizeRequestDto requestDto) {
        ThrowUtil.throwIfTrue(requestDto == null, ErrorEnum.PARAMS_ERROR, "请求参数不能为空");
        User loginUser = jwtUtil.parseLoginUser();
        ThrowUtil.throwIfTrue(loginUser == null, ErrorEnum.NOT_LOGIN_ERROR, "用户未登录");

        boolean draftMode = requestDto.getResumeDraft() != null;
        ThrowUtil.throwIfTrue(!draftMode && requestDto.getResumeId() == null, ErrorEnum.PARAMS_ERROR, "请传入已保存简历ID或草稿简历信息");

        ResumeVo resume = null;
        if (!draftMode) {
            resume = resumeService.getResumeById(requestDto.getResumeId());
            ThrowUtil.throwIfTrue(resume == null, ErrorEnum.NOT_FOUND_ERROR, "简历不存在");
        }

        if (!draftMode) {
            boolean isOwner = Objects.equals(resume.getUserId(), loginUser.getUserId());
            boolean isAdmin = UserRoleEnum.SYS_ADMIN.getValue().equals(loginUser.getRole());
            ThrowUtil.throwIfTrue(!isOwner && !isAdmin, ErrorEnum.UNAUTHORIZED, "无权限操作该简历");
        } else {
            PreviewResumeDto draft = requestDto.getResumeDraft();
            ThrowUtil.throwIfTrue(draft.getUserId() != null && !Objects.equals(draft.getUserId(), loginUser.getUserId()),
                    ErrorEnum.UNAUTHORIZED, "无权限操作该草稿简历");
        }

        OptimizeContext context = draftMode
                ? resolveDraftModuleContext(requestDto.getResumeDraft(), requestDto)
                : resolveSavedModuleContext(resume, requestDto);
        context.draftMode = draftMode;
        context.prompt = buildPrompt(draftMode ? requestDto.getResumeDraft() : resume, requestDto, context);
        return context;
    }

    private OptimizeContext resolveSavedModuleContext(ResumeVo resume, ResumeOptimizeRequestDto requestDto) {
        String moduleType = requestDto.getModuleType().trim().toUpperCase(Locale.ROOT);
        OptimizeContext context = new OptimizeContext();

        switch (moduleType) {
            case "SUMMARY" -> {
                ThrowUtil.throwIfTrue(!StringUtils.hasText(resume.getSummary()), ErrorEnum.PARAMS_ERROR, "该简历暂无个人简介可优化");
                context.moduleType = "SUMMARY";
                context.moduleId = null;
                context.moduleIndex = null;
                context.moduleContent = resume.getSummary();
            }
            case "EDUCATION" -> {
                ThrowUtil.throwIfTrue(requestDto.getModuleId() == null, ErrorEnum.PARAMS_ERROR, "优化教育经历时 moduleId 不能为空");
                ThrowUtil.throwIfTrue(resume.getEducations() == null || resume.getEducations().isEmpty(),
                        ErrorEnum.PARAMS_ERROR, "该简历暂无教育经历可优化");
                ResumeEducationVo target = resume.getEducations().stream()
                        .filter(item -> Objects.equals(item.getId(), requestDto.getModuleId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorEnum.PARAMS_ERROR, "未找到对应的教育经历"));
                ThrowUtil.throwIfTrue(!StringUtils.hasText(target.getDescription()), ErrorEnum.PARAMS_ERROR, "该教育经历内容为空，无法优化");
                context.moduleType = "EDUCATION";
                context.moduleId = requestDto.getModuleId();
                context.moduleIndex = null;
                context.moduleContent = buildEducationContent(target.getSchool(), target.getMajor(), target.getDegree(), target.getDescription());
            }
            case "EXPERIENCE" -> {
                ThrowUtil.throwIfTrue(requestDto.getModuleId() == null, ErrorEnum.PARAMS_ERROR, "优化工作经历时 moduleId 不能为空");
                ThrowUtil.throwIfTrue(resume.getExperiences() == null || resume.getExperiences().isEmpty(),
                        ErrorEnum.PARAMS_ERROR, "该简历暂无工作经历可优化");
                ResumeExperienceVo target = resume.getExperiences().stream()
                        .filter(item -> Objects.equals(item.getId(), requestDto.getModuleId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorEnum.PARAMS_ERROR, "未找到对应的工作经历"));
                ThrowUtil.throwIfTrue(!StringUtils.hasText(target.getDescription()), ErrorEnum.PARAMS_ERROR, "该工作经历内容为空，无法优化");
                context.moduleType = "EXPERIENCE";
                context.moduleId = requestDto.getModuleId();
                context.moduleIndex = null;
                context.moduleContent = target.getDescription();
            }
            case "PROJECT" -> {
                ThrowUtil.throwIfTrue(requestDto.getModuleId() == null, ErrorEnum.PARAMS_ERROR, "优化项目经历时 moduleId 不能为空");
                ThrowUtil.throwIfTrue(resume.getProjects() == null || resume.getProjects().isEmpty(),
                        ErrorEnum.PARAMS_ERROR, "该简历暂无项目经历可优化");
                ResumeProjectVo target = resume.getProjects().stream()
                        .filter(item -> Objects.equals(item.getId(), requestDto.getModuleId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorEnum.PARAMS_ERROR, "未找到对应的项目经历"));
                ThrowUtil.throwIfTrue(!StringUtils.hasText(target.getDescription()), ErrorEnum.PARAMS_ERROR, "该项目经历内容为空，无法优化");
                context.moduleType = "PROJECT";
                context.moduleId = requestDto.getModuleId();
                context.moduleIndex = null;
                context.moduleContent = target.getDescription();
            }
            default -> throw new BusinessException(ErrorEnum.PARAMS_ERROR, "moduleType 仅支持 SUMMARY/EDUCATION/EXPERIENCE/PROJECT");
        }
        return context;
    }

    private OptimizeContext resolveDraftModuleContext(PreviewResumeDto draft, ResumeOptimizeRequestDto requestDto) {
        String moduleType = requestDto.getModuleType().trim().toUpperCase(Locale.ROOT);
        OptimizeContext context = new OptimizeContext();

        switch (moduleType) {
            case "SUMMARY" -> {
                ThrowUtil.throwIfTrue(!StringUtils.hasText(draft.getSummary()), ErrorEnum.PARAMS_ERROR, "该草稿暂无个人简介可优化");
                context.moduleType = "SUMMARY";
                context.moduleId = null;
                context.moduleIndex = null;
                context.moduleContent = draft.getSummary();
            }
            case "EDUCATION" -> {
                ThrowUtil.throwIfTrue(requestDto.getModuleIndex() == null, ErrorEnum.PARAMS_ERROR, "优化草稿教育经历时 moduleIndex 不能为空");
                ThrowUtil.throwIfTrue(draft.getEducations() == null || requestDto.getModuleIndex() < 0
                        || requestDto.getModuleIndex() >= draft.getEducations().size(), ErrorEnum.PARAMS_ERROR, "未找到对应的教育经历");
                var target = draft.getEducations().get(requestDto.getModuleIndex());
                ThrowUtil.throwIfTrue(target.getDescription() == null || target.getDescription().isBlank(), ErrorEnum.PARAMS_ERROR, "该教育经历内容为空，无法优化");
                context.moduleType = "EDUCATION";
                context.moduleId = null;
                context.moduleIndex = requestDto.getModuleIndex();
                context.moduleContent = buildEducationContent(target.getSchool(), target.getMajor(), target.getDegree(), target.getDescription());
            }
            case "EXPERIENCE" -> {
                ThrowUtil.throwIfTrue(requestDto.getModuleIndex() == null, ErrorEnum.PARAMS_ERROR, "优化草稿工作经历时 moduleIndex 不能为空");
                ThrowUtil.throwIfTrue(draft.getExperiences() == null || requestDto.getModuleIndex() < 0
                        || requestDto.getModuleIndex() >= draft.getExperiences().size(), ErrorEnum.PARAMS_ERROR, "未找到对应的工作经历");
                var target = draft.getExperiences().get(requestDto.getModuleIndex());
                ThrowUtil.throwIfTrue(target.getDescription() == null || target.getDescription().isBlank(), ErrorEnum.PARAMS_ERROR, "该工作经历内容为空，无法优化");
                context.moduleType = "EXPERIENCE";
                context.moduleId = null;
                context.moduleIndex = requestDto.getModuleIndex();
                context.moduleContent = buildExperienceContent(target.getCompany(), target.getPosition(), target.getDescription());
            }
            case "PROJECT" -> {
                ThrowUtil.throwIfTrue(requestDto.getModuleIndex() == null, ErrorEnum.PARAMS_ERROR, "优化草稿项目经历时 moduleIndex 不能为空");
                ThrowUtil.throwIfTrue(draft.getProjects() == null || requestDto.getModuleIndex() < 0
                        || requestDto.getModuleIndex() >= draft.getProjects().size(), ErrorEnum.PARAMS_ERROR, "未找到对应的项目经历");
                var target = draft.getProjects().get(requestDto.getModuleIndex());
                ThrowUtil.throwIfTrue(target.getDescription() == null || target.getDescription().isBlank(), ErrorEnum.PARAMS_ERROR, "该项目经历内容为空，无法优化");
                context.moduleType = "PROJECT";
                context.moduleId = null;
                context.moduleIndex = requestDto.getModuleIndex();
                context.moduleContent = buildProjectContent(target.getName(), target.getDescription());
            }
            default -> throw new BusinessException(ErrorEnum.PARAMS_ERROR, "moduleType 仅支持 SUMMARY/EDUCATION/EXPERIENCE/PROJECT");
        }

        return context;
    }

    private String buildPrompt(Object resumeSource, ResumeOptimizeRequestDto requestDto, OptimizeContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你将优化一段简历内容，禁止虚构未提供的信息。\n")
                .append("请遵循：保留事实、强化表达、突出结果、使用专业简洁中文。\n")
                .append("输出要求：只输出优化后的最终文本，不要额外解释，不要使用Markdown。\n\n")
                .append("【候选人背景】\n")
                .append("姓名：").append(nullSafe(extractName(resumeSource))).append("\n")
                .append("期望城市：").append(nullSafe(extractCity(resumeSource))).append("\n")
                .append("个人简介：").append(nullSafe(extractSummary(resumeSource))).append("\n");

        if (StringUtils.hasText(requestDto.getTargetJob())) {
            prompt.append("目标岗位：").append(requestDto.getTargetJob()).append("\n");
        }
        if (StringUtils.hasText(requestDto.getExtraRequirement())) {
            prompt.append("额外要求：").append(requestDto.getExtraRequirement()).append("\n");
        }

        prompt.append("\n【待优化模块】\n")
                .append("模块类型：").append(context.moduleType).append("\n")
                .append("模块ID：").append(context.moduleId == null ? "-" : context.moduleId).append("\n")
                .append("模块下标：").append(context.moduleIndex == null ? "-" : context.moduleIndex).append("\n")
                .append("原始内容：\n")
                .append(context.moduleContent);

        return prompt.toString();
    }

    private String buildExperienceContent(String company, String position, String description) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(company)) {
            builder.append("公司：").append(company).append("\n");
        }
        if (StringUtils.hasText(position)) {
            builder.append("职位：").append(position).append("\n");
        }
        builder.append("描述：").append(description);
        return builder.toString();
    }

    private String buildProjectContent(String name, String description) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(name)) {
            builder.append("项目名称：").append(name).append("\n");
        }
        builder.append("项目描述：").append(description);
        return builder.toString();
    }

    private String buildEducationContent(String school, String major, String degree, String description) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(school)) {
            builder.append("学校：").append(school).append("\n");
        }
        if (StringUtils.hasText(major)) {
            builder.append("专业：").append(major).append("\n");
        }
        if (StringUtils.hasText(degree)) {
            builder.append("学历：").append(degree).append("\n");
        }
        builder.append("描述：").append(description);
        return builder.toString();
    }

    private String extractName(Object resumeSource) {
        return resumeSource instanceof ResumeVo resumeVo ? resumeVo.getName() : ((PreviewResumeDto) resumeSource).getName();
    }

    private String extractCity(Object resumeSource) {
        return resumeSource instanceof ResumeVo resumeVo ? resumeVo.getCity() : ((PreviewResumeDto) resumeSource).getCity();
    }

    private String extractSummary(Object resumeSource) {
        return resumeSource instanceof ResumeVo resumeVo ? resumeVo.getSummary() : ((PreviewResumeDto) resumeSource).getSummary();
    }

    private String nullSafe(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private static class OptimizeContext {
        private boolean draftMode;
        private String moduleType;
        private Long moduleId;
        private Integer moduleIndex;
        private String moduleContent;
        private String prompt;
    }
}
