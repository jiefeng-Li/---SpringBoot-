package com.cuit.interviewsystem.model.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MockInterviewRequestDto {

    @NotBlank(message = "难度不能为空")
    private String difficulty;

    @Min(value = 1, message = "题目总数至少为1")
    @Max(value = 6, message = "题目总数最多为6")
    private Integer totalQuestions;

    private String companyName;

    private String jobTitle;

    private String workCity;

    private String description;

    private String requirement;

    @Valid
    @NotNull(message = "问答记录不能为空")
    private List<MockInterviewTurnDto> records = new ArrayList<>();
}
