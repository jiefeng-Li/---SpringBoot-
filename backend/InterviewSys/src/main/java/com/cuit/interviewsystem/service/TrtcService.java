package com.cuit.interviewsystem.service;

import com.cuit.interviewsystem.model.vo.TrtcSigVo;

public interface TrtcService {

    /**
     * 获取TRTC房间信息
     * @param noticeId 面试记录id
     * @return 房间信息
     */
    TrtcSigVo getTrtcRoomInfo(Long noticeId);
}
