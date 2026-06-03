package com.cuit.interviewsystem.model.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MockInterviewTurnDto {
    @NotBlank(message = "问题不能为空")
    private String question;

    @NotBlank(message = "回答不能为空")
    private String answer;
}
