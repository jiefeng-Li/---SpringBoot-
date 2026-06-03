package com.cuit.interviewsystem.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试参与人表
 * @TableName t_interview_notice_participant
 */
@TableName(value = "t_interview_notice_participant")
@Data
public class InterviewNoticeParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long interviewNoticeId;

    private Long userId;

    private Integer participantType;

    private Integer isPrimary;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}