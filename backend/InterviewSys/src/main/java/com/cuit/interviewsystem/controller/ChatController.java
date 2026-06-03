package com.cuit.interviewsystem.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cuit.interviewsystem.annotation.AuthCheck;
import com.cuit.interviewsystem.common.Result;
import com.cuit.interviewsystem.exception.BusinessException;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.handler.ChatSessionManager;
import com.cuit.interviewsystem.model.dto.ChatMessageDto;
import com.cuit.interviewsystem.model.dto.chat.SystemMessagePushDto;
import com.cuit.interviewsystem.model.entity.User;
import com.cuit.interviewsystem.model.enums.UserRoleEnum;
import com.cuit.interviewsystem.model.vo.ChatContactVo;
import com.cuit.interviewsystem.model.vo.ChatMessageVo;
import com.cuit.interviewsystem.mapper.UserMapper;
import com.cuit.interviewsystem.service.ChatMessageService;
import com.cuit.interviewsystem.utils.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController("chat")
public class ChatController {
    @Resource
    private ChatMessageService chatMessageService;
    @Resource
    private JWTUtil jwtUtil;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ChatSessionManager chatSessionManager;

    @PostMapping("/msg")
    @Operation(summary = "标记消息为已读")
    @AuthCheck(roles = {UserRoleEnum.JOB_SEEKER, UserRoleEnum.RECRUITER, UserRoleEnum.COMP_ADMIN, UserRoleEnum.SYS_ADMIN})
    public Result<Void> markMessagesAsRead(@RequestBody List<Long> messageIds) {
        chatMessageService.markMessagesAsRead(messageIds);
        return Result.success(null);
    }

    @GetMapping("/msg/contacts")
    @Operation(summary = "获取聊天会话列表")
    @AuthCheck(roles = {UserRoleEnum.JOB_SEEKER, UserRoleEnum.RECRUITER, UserRoleEnum.COMP_ADMIN, UserRoleEnum.SYS_ADMIN})
    public Result<List<ChatContactVo>> getChatContacts() {
        return Result.success(chatMessageService.getChatContacts());
    }

    @GetMapping("/msg/history/{targetUserId}")
    @Operation(summary = "获取会话历史消息")
    @AuthCheck(roles = {UserRoleEnum.JOB_SEEKER, UserRoleEnum.RECRUITER, UserRoleEnum.COMP_ADMIN, UserRoleEnum.SYS_ADMIN})
    public Result<List<ChatMessageVo>> getConversationMessages(@PathVariable Long targetUserId, Integer limit) {
        return Result.success(chatMessageService.getConversationMessages(targetUserId, limit));
    }

    @PostMapping("/msg/revoke/{id}")
    @Operation(summary = "撤回消息")
    @AuthCheck(roles = {UserRoleEnum.JOB_SEEKER, UserRoleEnum.RECRUITER, UserRoleEnum.COMP_ADMIN, UserRoleEnum.SYS_ADMIN})
    public Result<Void> revokeMessage(@PathVariable Long id) {
        chatMessageService.revokeMessage(id);
        return Result.success();
    }

    @PostMapping("/msg/system/push")
    @Operation(summary = "发送系统消息并推送")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN, UserRoleEnum.RECRUITER})
    public Result<Integer> pushSystemMessage(@Valid @RequestBody SystemMessagePushDto dto) {
        Long loginUserId = Long.parseLong(jwtUtil.getLoginUserInfo(JWTUtil.ELEMENT.USER_ID));
        UserRoleEnum role = UserRoleEnum.getRole(jwtUtil.getLoginUserInfo(JWTUtil.ELEMENT.ROLE));

        if (UserRoleEnum.RECRUITER.equals(role) && (dto.getSendToAll() == null || dto.getSendToAll())) {
            throw new BusinessException(ErrorEnum.UNAUTHORIZED, "招聘者仅支持定向发送系统消息");
        }

        List<Long> targetUserIds = resolveTargetUserIds(dto, loginUserId);
        if (targetUserIds.isEmpty()) {
            return Result.success(0, "没有可推送的目标用户");
        }

        int successCount = 0;
        for (Long targetUserId : targetUserIds) {
            ChatMessageDto msg = new ChatMessageDto();
            msg.setSendId(loginUserId);
            msg.setReceiveId(targetUserId);
            msg.setContent(dto.getContent().trim());
            msg.setMsgType(1);
            msg.setTimestamp(System.currentTimeMillis());
            ChatMessageVo saved = chatMessageService.saveChatMessage(msg);
            chatSessionManager.sendToUser(targetUserId, saved);
            successCount++;
        }
        return Result.success(successCount, "系统消息发送成功");
    }

    private List<Long> resolveTargetUserIds(SystemMessagePushDto dto, Long adminUserId) {
        boolean includeSelf = Boolean.TRUE.equals(dto.getIncludeSelf());
        boolean sendToAll = dto.getSendToAll() == null || dto.getSendToAll();

        Set<Long> targetSet = new LinkedHashSet<>();
        if (sendToAll) {
            List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getIsDeleted, 0));
            for (User user : users) {
                if (user == null || user.getUserId() == null) {
                    continue;
                }
                if (!includeSelf && user.getUserId().equals(adminUserId)) {
                    continue;
                }
                targetSet.add(user.getUserId());
            }
        } else {
            if (dto.getTargetUserIds() == null) {
                return new ArrayList<>();
            }
            for (Long id : dto.getTargetUserIds()) {
                if (id == null || id <= 0) {
                    continue;
                }
                if (!includeSelf && id.equals(adminUserId)) {
                    continue;
                }
                targetSet.add(id);
            }
        }

        return new ArrayList<>(targetSet);
    }


}
