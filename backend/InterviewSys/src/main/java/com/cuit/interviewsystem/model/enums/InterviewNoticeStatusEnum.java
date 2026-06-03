package com.cuit.interviewsystem.model.enums;

import lombok.Getter;

@Getter
public enum InterviewNoticeStatusEnum {
    PENDING(0, "待确认"),
    ACCEPTED(1, "已确认"),
    REJECTED(2, "已拒绝"),
    CANCELED(3, "已取消"),
    FINISHED(4, "已结束");

    private final Integer status;
    private final String text;

    InterviewNoticeStatusEnum(Integer status, String text) {
        this.status = status;
        this.text = text;
    }

    public static InterviewNoticeStatusEnum getEnum(Integer status) {
        for (InterviewNoticeStatusEnum item : values()) {
            if (item.status.equals(status)) {
                return item;
            }
        }
        return null;
    }

    public static InterviewNoticeStatusEnum getEnum(String text) {
        for (InterviewNoticeStatusEnum item : values()) {
            if (item.text.equals(text)) {
                return item;
            }
        }
        try {
            Integer status = Integer.parseInt(text);
            return getEnum(status);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}