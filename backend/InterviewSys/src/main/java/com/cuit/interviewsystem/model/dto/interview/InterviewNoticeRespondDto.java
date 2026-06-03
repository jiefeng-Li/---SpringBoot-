package com.cuit.interviewsystem.model.dto.interview;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class InterviewNoticeRespondDto {

    @NotNull(message = "面试记录ID不能为空")
    private Long id;

    @NotNull(message = "处理状态不能为空")
    private Integer status;

    @Length(max = 500, message = "拒绝原因过长")
    private String rejectReason;
}