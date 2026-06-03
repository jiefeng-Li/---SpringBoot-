package com.cuit.interviewsystem.model.dto.interview;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class InterviewNoticeCancelDto {

    @NotNull(message = "面试记录ID不能为空")
    private Long id;

    @Length(max = 500, message = "取消原因过长")
    private String cancelReason;
}