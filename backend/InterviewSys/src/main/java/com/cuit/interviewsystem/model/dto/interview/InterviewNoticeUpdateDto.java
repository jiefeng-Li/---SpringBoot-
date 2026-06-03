package com.cuit.interviewsystem.model.dto.interview;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InterviewNoticeUpdateDto extends InterviewNoticeAddDto {

    @NotNull(message = "面试记录ID不能为空")
    private Long id;
}