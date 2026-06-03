package com.cuit.interviewsystem.service;

import com.cuit.interviewsystem.model.dto.ai.ResumeOptimizeRequestDto;
import com.cuit.interviewsystem.model.dto.ai.ResumeOptimizeResponseDto;
import reactor.core.publisher.Flux;

public interface ResumeAiService {
    ResumeOptimizeResponseDto optimizeResumeModule(ResumeOptimizeRequestDto requestDto);

    Flux<String> optimizeResumeModuleStream(ResumeOptimizeRequestDto requestDto);
}
