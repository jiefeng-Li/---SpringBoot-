package com.cuit.interviewsystem.model.enums;

import lombok.Getter;

@Getter
public enum RtcPlatformEnum {
    TRTC(1, "腾讯云 TRTC"),
    ZEGO(2, "ZEGO 即构"),
    AGORA(3, "Agora"),
    JITSI(4, "Jitsi");

    private final Integer type;
    private final String text;

    RtcPlatformEnum(Integer type, String text) {
        this.type = type;
        this.text = text;
    }

    public static RtcPlatformEnum getEnum(Integer type) {
        for (RtcPlatformEnum item : values()) {
            if (item.type.equals(type)) {
                return item;
            }
        }
        return null;
    }
}