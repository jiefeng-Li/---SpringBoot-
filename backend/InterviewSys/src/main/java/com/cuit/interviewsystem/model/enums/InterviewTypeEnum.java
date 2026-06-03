package com.cuit.interviewsystem.model.enums;

import lombok.Getter;

@Getter
public enum InterviewTypeEnum {
    OFFLINE(0, "线下面试"),
    ONLINE(1, "线上面试");

    private final Integer type;
    private final String text;

    InterviewTypeEnum(Integer type, String text) {
        this.type = type;
        this.text = text;
    }

    public static InterviewTypeEnum getEnum(Integer type) {
        for (InterviewTypeEnum item : values()) {
            if (item.type.equals(type)) {
                return item;
            }
        }
        return null;
    }
}