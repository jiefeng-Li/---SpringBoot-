package com.cuit.interviewsystem.service.impl;

import com.cuit.interviewsystem.exception.BusinessException;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.model.dto.ai.MockInterviewRequestDto;
import com.cuit.interviewsystem.model.dto.ai.MockInterviewResponseDto;
import com.cuit.interviewsystem.model.dto.ai.MockInterviewTurnDto;
import com.cuit.interviewsystem.service.MockInterviewAiService;
import com.cuit.interviewsystem.utils.ThrowUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class MockInterviewAiServiceImpl implements MockInterviewAiService {

    @Resource
    private ChatClient dashScopeChatClient;

/**
 * 处理模拟面试请求的方法
 * 根据请求DTO生成响应DTO，包括回答数量、题目总数、是否完成以及下一题或分数和总结
 *
 * @param requestDto 模拟面试请求DTO，包含面试记录和题目总数等信息
 * @return MockInterviewResponseDto 模拟面试响应DTO，包含面试状态和结果信息
 */
    @Override
    public MockInterviewResponseDto handleMockInterview(MockInterviewRequestDto requestDto) {
    // 验证请求参数的有效性
        validateRequest(requestDto);

    // 获取已回答问题数量和总问题数量
        int answeredCount = requestDto.getRecords().size();
        int totalQuestions = requestDto.getTotalQuestions();
    // 检查已回答数量是否超过总数量，若超过则抛出异常
        if (answeredCount > totalQuestions) {
            throw new BusinessException(ErrorEnum.PARAMS_ERROR, "问答记录超过题目总数");
        }

    // 创建响应对象并设置基本属性
        MockInterviewResponseDto response = new MockInterviewResponseDto();
        response.setAnsweredCount(answeredCount);
        response.setTotalQuestions(totalQuestions);

    // 判断是否所有问题都已回答
        if (answeredCount >= totalQuestions) {
        // 设置面试完成状态，填充分数和总结
            response.setFinished(true);
            fillScoreAndSummary(response, requestDto);
            return response;
        }

    // 设置面试未完成状态，获取下一题
        response.setFinished(false);
    // 通过聊天客户端获取下一题内容
        String nextQuestion = dashScopeChatClient.prompt()
                .system(buildQuestionSystemPrompt())
                .user(buildQuestionUserPrompt(requestDto))
                .call()
                .content();
    // 设置下一题内容，若为空则使用默认问题
        response.setNextQuestion(StringUtils.hasText(nextQuestion) ? nextQuestion.trim() : "请你介绍一个你最有成就感的项目，并说明你的具体贡献。");
        return response;
    }

    private void fillScoreAndSummary(MockInterviewResponseDto response, MockInterviewRequestDto requestDto) {
        String scoreResult = dashScopeChatClient.prompt()
                .system(buildScoreSystemPrompt())
                .user(buildScoreUserPrompt(requestDto))
                .call()
                .content();

        int score = extractScore(scoreResult);
        response.setScore(score);
        response.setSummary(buildSummary(scoreResult, score));
    }

    private void validateRequest(MockInterviewRequestDto requestDto) {
        ThrowUtil.throwIfTrue(requestDto == null, ErrorEnum.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtil.throwIfTrue(!StringUtils.hasText(requestDto.getDifficulty()), ErrorEnum.PARAMS_ERROR, "难度不能为空");
        ThrowUtil.throwIfTrue(requestDto.getTotalQuestions() == null, ErrorEnum.PARAMS_ERROR, "题目总数不能为空");
        ThrowUtil.throwIfTrue(requestDto.getRecords() == null, ErrorEnum.PARAMS_ERROR, "问答记录不能为空");

        String difficulty = requestDto.getDifficulty().trim().toUpperCase();
        int totalQuestions = requestDto.getTotalQuestions();
        boolean invalidRange = switch (difficulty) {
            case "EASY" -> totalQuestions < 1 || totalQuestions > 2;
            case "MEDIUM" -> totalQuestions < 3 || totalQuestions > 4;
            case "HARD" -> totalQuestions < 5 || totalQuestions > 6;
            default -> true;
        };
        ThrowUtil.throwIfTrue(invalidRange, ErrorEnum.PARAMS_ERROR, "难度与题目总数不匹配");
    }

    private String buildQuestionSystemPrompt() {
        return "你是专业技术面试官。你要进行模拟面试提问。规则：" +
                "1) 仅输出下一道面试问题，不要解释，不要附加标题；" +
                "2) 问题要和岗位信息相关，避免重复历史问题；" +
                "3) 问句简洁明确，中文输出。";
    }

    private String buildQuestionUserPrompt(MockInterviewRequestDto requestDto) {
        StringBuilder sb = new StringBuilder();
        sb.append("难度：").append(requestDto.getDifficulty()).append("\n")
                .append("总题数：").append(requestDto.getTotalQuestions()).append("\n")
                .append("公司：").append(nullSafe(requestDto.getCompanyName())).append("\n")
                .append("岗位：").append(nullSafe(requestDto.getJobTitle())).append("\n")
                .append("工作城市：").append(nullSafe(requestDto.getWorkCity())).append("\n")
                .append("职位描述：").append(nullSafe(requestDto.getDescription())).append("\n")
                .append("职位要求：").append(nullSafe(requestDto.getRequirement())).append("\n")
                .append("已完成问答：\n");

        appendRecords(sb, requestDto.getRecords());
        sb.append("请输出下一道面试问题。");
        return sb.toString();
    }

    private String buildScoreSystemPrompt() {
        return "你是面试评估官。你需要根据岗位信息和候选人的全部回答进行打分。" +
                "输出格式必须严格如下两行：\n" +
                "score: 0-100之间整数\n" +
                "summary: 2-4句中文评价，包含优点、短板和建议\n" +
                "不要输出其它任何内容。";
    }

    private String buildScoreUserPrompt(MockInterviewRequestDto requestDto) {
        StringBuilder sb = new StringBuilder();
        sb.append("难度：").append(requestDto.getDifficulty()).append("\n")
                .append("总题数：").append(requestDto.getTotalQuestions()).append("\n")
                .append("公司：").append(nullSafe(requestDto.getCompanyName())).append("\n")
                .append("岗位：").append(nullSafe(requestDto.getJobTitle())).append("\n")
                .append("工作城市：").append(nullSafe(requestDto.getWorkCity())).append("\n")
                .append("职位描述：").append(nullSafe(requestDto.getDescription())).append("\n")
                .append("职位要求：").append(nullSafe(requestDto.getRequirement())).append("\n")
                .append("完整问答：\n");

        appendRecords(sb, requestDto.getRecords());
        return sb.toString();
    }

    private void appendRecords(StringBuilder sb, List<MockInterviewTurnDto> records) {
        for (int i = 0; i < records.size(); i++) {
            MockInterviewTurnDto record = records.get(i);
            sb.append("Q").append(i + 1).append("：").append(nullSafe(record.getQuestion())).append("\n")
                    .append("A").append(i + 1).append("：").append(nullSafe(record.getAnswer())).append("\n");
        }
        if (records.isEmpty()) {
            sb.append("（暂无）\n");
        }
    }

    private int extractScore(String scoreResult) {
        if (!StringUtils.hasText(scoreResult)) {
            return 60;
        }
        String lower = scoreResult.toLowerCase();
        int idx = lower.indexOf("score:");
        if (idx < 0) {
            return 60;
        }
        String tail = scoreResult.substring(idx + "score:".length()).trim();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.length() == 0) {
            return 60;
        }
        int parsed = Integer.parseInt(digits.toString());
        return Math.max(0, Math.min(100, parsed));
    }

    private String buildSummary(String scoreResult, int score) {
        if (!StringUtils.hasText(scoreResult)) {
            return "本次模拟面试已结束。建议继续围绕岗位核心能力进行针对性练习。";
        }
        String lower = scoreResult.toLowerCase();
        int idx = lower.indexOf("summary:");
        if (idx < 0) {
            return "本次模拟面试已结束。建议继续围绕岗位核心能力进行针对性练习。";
        }
        String summary = scoreResult.substring(idx + "summary:".length()).trim();
        return StringUtils.hasText(summary) ? summary : "本次得分为" + score + "分，建议继续强化表达与案例细节。";
    }

    private String nullSafe(String value) {
        return StringUtils.hasText(value) ? value : "";
    }
}
