package com.cuit.interviewsystem.controller;

import com.cuit.interviewsystem.annotation.AuthCheck;
import com.cuit.interviewsystem.common.Result;
import com.cuit.interviewsystem.model.enums.UserRoleEnum;
import com.cuit.interviewsystem.model.vo.TrtcSigVo;
import com.cuit.interviewsystem.service.InterviewNoticeService;
import com.cuit.interviewsystem.service.TrtcService;
import com.cuit.interviewsystem.utils.JWTUtil;
import com.cuit.interviewsystem.utils.TRTCUtil;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trtc")
public class TrtcController {
    @Resource
    private TrtcService trtcService;


    @GetMapping("/sig")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER, UserRoleEnum.JOB_SEEKER})
    public Result<TrtcSigVo> getCurrentUserSig(Long noticeId) {
        TrtcSigVo res = trtcService.getTrtcRoomInfo(noticeId);
        return Result.success(res);
    }
}
