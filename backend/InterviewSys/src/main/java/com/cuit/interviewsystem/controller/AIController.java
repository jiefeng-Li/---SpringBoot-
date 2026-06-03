package com.cuit.interviewsystem.controller;


import com.cuit.interviewsystem.annotation.AuthCheck;
import com.cuit.interviewsystem.common.Result;
import com.cuit.interviewsystem.model.dto.ai.MockInterviewRequestDto;
import com.cuit.interviewsystem.model.dto.ai.MockInterviewResponseDto;
import com.cuit.interviewsystem.model.dto.ai.ResumeOptimizeRequestDto;
import com.cuit.interviewsystem.model.dto.ai.ResumeOptimizeResponseDto;
import com.cuit.interviewsystem.service.MockInterviewAiService;
import com.cuit.interviewsystem.service.ResumeAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@Tag(name = "ai聊天接口")
public class AIController {
    @Resource
    private ChatClient dashScopeChatClient;
    @Resource
    private ResumeAiService resumeAiService;
    @Resource
    private MockInterviewAiService mockInterviewAiService;

    @GetMapping("/test")
    @Operation(summary = "测试接口(非响应)")
    public String chatTest(String prompt) {
        return dashScopeChatClient.prompt(prompt).call().content();
    }

    @GetMapping("/test/stream/chat")
    @Operation(summary = "测试接口(响应式)")
    public Flux<String> streamChat(HttpServletResponse response, String prompt) {
        response.setCharacterEncoding("UTF-8");
        return dashScopeChatClient.prompt(prompt).stream().content();
    }

    @PostMapping("/resume/optimize")
    @AuthCheck
    @Operation(summary = "优化简历模块(同步)")
    public Result<ResumeOptimizeResponseDto> optimizeResumeModule(@Valid @RequestBody ResumeOptimizeRequestDto requestDto) {
        return Result.success(resumeAiService.optimizeResumeModule(requestDto));
    }

    @PostMapping(value = "/resume/optimize/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuthCheck
    @Operation(summary = "优化简历模块(流式)")
    public Flux<String> optimizeResumeModuleStream(@Valid @RequestBody ResumeOptimizeRequestDto requestDto) {
        return resumeAiService.optimizeResumeModuleStream(requestDto);
    }

    @PostMapping("/mock-interview")
    @AuthCheck
    @Operation(summary = "模拟面试问答/评分")
    public Result<MockInterviewResponseDto> handleMockInterview(@Valid @RequestBody MockInterviewRequestDto requestDto) {
        return Result.success(mockInterviewAiService.handleMockInterview(requestDto));
    }
}
