package com.cuit.interviewsystem.utils;

import com.tencentyun.TLSSigAPIv2;
import lombok.Getter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TRTCUtil {

    @Getter
    @Value("${trtc.sdk-app-id}")
    private long appId;
    @Value("${trtc.secret-key}")
    private String secretKey;
    private TLSSigAPIv2 api;

    @PostConstruct
    public void init() {
        api = new TLSSigAPIv2(appId, secretKey);
    }

    public String genUserSig(String userId) {
        return api.genUserSig(userId, 604800); // 7天有效期
    }
}
