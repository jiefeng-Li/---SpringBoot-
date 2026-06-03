package com.cuit.interviewsystem.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 面试预约主表
 * @TableName t_interview_notice
 */
@TableName(value ="t_interview_notice")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterviewNotice {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 投递记录ID
     */
    private Long jobApplicationId;

    /**
        * 公司ID
     */
        private Long companyId;

    /**
        * 求职者用户ID
        */
        private Long jobSeekerId;

        /**
        * 创建人ID（招聘者）
        */
        private Long creatorId;

        /**
        * 面试类型(0线下面试,1线上面试)
        */
        private Integer interviewType;

    /**
        * 面试开始时间
     */
        private LocalDateTime interviewStartTime;

        /**
        * 面试结束时间
        */
        private LocalDateTime interviewEndTime;

    /**
        * 线下面试地址
     */
    private String interviewAddress;

        /**
        * 线上会议平台
        */
        private Integer rtcPlatform;

        /**
        * 会议房间号
        */
        private String rtcRoomId;

        /**
        * 会议名称
        */
        private String rtcRoomName;

        /**
        * 会议加入链接
        */
        private String rtcJoinUrl;

        /**
        * 会议密码/入会口令
        */
        private String rtcPassword;

    /**
     * 备注
     */
    private String comment;

        /**
        * 状态(0待确认,1已确认,2已拒绝,3已取消,4已结束)
        */
        private Integer status;

        /**
        * 求职者拒绝原因
        */
        private String candidateReplyReason;

        /**
        * 求职者回复时间
        */
        private LocalDateTime candidateReplyTime;

        /**
        * 招聘者取消原因
        */
        private String cancelReason;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}