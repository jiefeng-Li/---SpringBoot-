package com.cuit.interviewsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.mapper.InterviewNoticeMapper;
import com.cuit.interviewsystem.mapper.InterviewNoticeParticipantMapper;
import com.cuit.interviewsystem.model.entity.InterviewNotice;
import com.cuit.interviewsystem.model.entity.InterviewNoticeParticipant;
import com.cuit.interviewsystem.model.vo.TrtcSigVo;
import com.cuit.interviewsystem.service.TrtcService;
import com.cuit.interviewsystem.utils.JWTUtil;
import com.cuit.interviewsystem.utils.TRTCUtil;
import com.cuit.interviewsystem.utils.ThrowUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class TrtcServiceImpl implements TrtcService {
    @Resource
    private InterviewNoticeMapper noticeMapper;
    @Resource
    private InterviewNoticeParticipantMapper participantMapper;
    @Resource
    private JWTUtil jwtUtil;
    @Resource
    private TRTCUtil trtcUtil;

    @Override
    /**
     * 获取TRTC房间信息
     * @param noticeId 面试通知ID
     * @return TrtcSigVo TRTC房间信息对象，包含用户ID、SDKAppID、用户签名、房间ID和房间名称
     */
    public TrtcSigVo getTrtcRoomInfo(Long noticeId) {
        // 获取面试记录
        InterviewNotice notice = noticeMapper.selectById(noticeId);
        ThrowUtil.throwIfTrue(notice == null, ErrorEnum.NOT_FOUND_ERROR); // 如果面试记录不存在，抛出未找到错误
        ThrowUtil.throwIfTrue(LocalDateTime.now().isAfter(notice.getInterviewEndTime()), ErrorEnum.PARAMS_ERROR, "面试已结束"); // 如果当前时间在面试结束时间之后，抛出参数错误，提示面试已结束
        ThrowUtil.throwIfTrue(LocalDateTime.now().isBefore(notice.getInterviewStartTime()), ErrorEnum.PARAMS_ERROR, "面试未开始"); // 如果当前时间在面试开始时间之前，抛出参数错误，提示面试未开始
        // 鉴权：检查用户是否有权限参加此次会议
        Long userId = Long.parseLong(jwtUtil.getLoginUserInfo(JWTUtil.ELEMENT.USER_ID)); // 从JWT中获取用户ID
        ThrowUtil.throwIfTrue(!participantMapper.exists(new LambdaQueryWrapper<InterviewNoticeParticipant>()
                        .eq(InterviewNoticeParticipant::getInterviewNoticeId, noticeId) // 查询条件：面试通知ID
                        .eq(InterviewNoticeParticipant::getIsDeleted, 0) // 查询条件：未删除
                        .eq(InterviewNoticeParticipant::getUserId, userId)), // 查询条件：用户ID
                ErrorEnum.UNAUTHORIZED, "无权限参加此次会议"); // 如果查询结果不存在，抛出未授权错误
        // 返回trtc房间信息
        TrtcSigVo res = new TrtcSigVo(); // 创建TRTC房间信息对象
        res.setUserId(String.valueOf(userId)); // 设置用户ID
        res.setSdkAppId(trtcUtil.getAppId()); // 设置SDK应用ID
        res.setUserSig(trtcUtil.genUserSig(String.valueOf(userId))); // 生成并设置用户签名
        res.setRoomId(notice.getRtcRoomId()); // 设置房间ID
        res.setRoomName(notice.getRtcRoomName()); // 设置房间名称
        return res; // 返回TRTC房间信息对象
    }
}
