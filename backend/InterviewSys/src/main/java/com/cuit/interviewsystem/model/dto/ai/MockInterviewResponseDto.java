package com.cuit.interviewsystem.model.dto.ai;

import lombok.Data;

@Data
public class MockInterviewResponseDto {
    private boolean finished;
    private Integer answeredCount;
    private Integer totalQuestions;
    private String nextQuestion;
    private Integer score;
    private String summary;
}
