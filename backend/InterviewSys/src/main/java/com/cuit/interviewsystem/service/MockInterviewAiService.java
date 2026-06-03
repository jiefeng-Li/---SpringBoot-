package com.cuit.interviewsystem.service;

import com.cuit.interviewsystem.model.dto.ai.MockInterviewRequestDto;
import com.cuit.interviewsystem.model.dto.ai.MockInterviewResponseDto;

public interface MockInterviewAiService {
    MockInterviewResponseDto handleMockInterview(MockInterviewRequestDto requestDto);
}
