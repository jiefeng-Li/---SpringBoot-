package com.cuit.interviewsystem.model.dto.ai;

import lombok.Data;

@Data
public class ResumeOptimizeResponseDto {
    private Long resumeId;
    private Boolean draftMode;
    private String moduleType;
    private Long moduleId;
    private Integer moduleIndex;
    private String originalContent;
    private String optimizedContent;
}
