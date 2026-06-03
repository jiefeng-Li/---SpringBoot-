package com.cuit.interviewsystem.model.dto.interview;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewNoticeAddDto {
    @NotNull(message = "投递记录ID不能为空")
    private Long jobApplicationId;

    @NotNull(message = "面试类型不能为空")
    private Integer interviewType;

    @NotNull(message = "面试开始时间不能为空")
    private LocalDateTime interviewStartTime;

    @NotNull(message = "面试结束时间不能为空")
    private LocalDateTime interviewEndTime;

    private String interviewAddress;

    private Integer rtcPlatform;

    private String rtcRoomId;

    private String rtcRoomName;

    private String rtcJoinUrl;

    private String rtcPassword;

    @Length(max = 500, message = "备注过长")
    private String comment;

    @NotNull(message = "面试官不能为空")
    private List<Long> interviewerIds;
}
