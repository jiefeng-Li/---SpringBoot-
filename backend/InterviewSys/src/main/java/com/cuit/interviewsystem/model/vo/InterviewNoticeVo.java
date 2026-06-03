package com.cuit.interviewsystem.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewNoticeVo {
    private Long id;

    private Long jobApplicationId;

    private Long companyId;

    private String companyName;

    private Long jobSeekerId;

    private String jobSeekerName;

    private Long creatorId;

    private String creatorName;

    private Integer interviewType;

    private String interviewTypeText;

    private LocalDateTime interviewStartTime;

    private LocalDateTime interviewEndTime;

    private String interviewAddress;

    private Integer rtcPlatform;

    private String rtcPlatformText;

    private String rtcRoomId;

    private String rtcRoomName;

    private String rtcJoinUrl;

    private String rtcPassword;

    private String comment;

    private Integer status;

    private String statusText;

    private String candidateReplyReason;

    private LocalDateTime candidateReplyTime;

    private String cancelReason;

    private List<InterviewParticipantVo> interviewers;
}
