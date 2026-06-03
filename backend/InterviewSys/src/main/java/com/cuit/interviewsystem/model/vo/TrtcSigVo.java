package com.cuit.interviewsystem.model.vo;

import lombok.Data;

@Data
public class TrtcSigVo {
    private long sdkAppId;
    private String userId;
    private String userSig;
    private String roomId;
    private String roomName;
}
