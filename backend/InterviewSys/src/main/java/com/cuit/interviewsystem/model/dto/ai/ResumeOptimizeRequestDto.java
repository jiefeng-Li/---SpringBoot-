package com.cuit.interviewsystem.model.dto.ai;

import com.cuit.interviewsystem.model.dto.resume.PreviewResumeDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ResumeOptimizeRequestDto {

    private Long resumeId;

    private PreviewResumeDto resumeDraft;

    @NotBlank(message = "优化模块不能为空")
    @Length(max = 20, message = "优化模块参数过长")
    private String moduleType;

    private Long moduleId;

    private Integer moduleIndex;

    @Length(max = 100, message = "目标岗位长度不能超过100")
    private String targetJob;

    @Length(max = 500, message = "额外要求长度不能超过500")
    private String extraRequirement;
}
