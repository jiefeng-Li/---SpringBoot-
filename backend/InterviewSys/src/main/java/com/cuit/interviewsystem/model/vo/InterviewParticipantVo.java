package com.cuit.interviewsystem.model.vo;

import lombok.Data;

@Data
public class InterviewParticipantVo {
    private Long userId;
    private String username;
    private String role;
    private String avatarUrl;
    private Integer participantType;
    private Boolean primary;
}