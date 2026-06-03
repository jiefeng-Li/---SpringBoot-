package com.cuit.interviewsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.mapper.InterviewNoticeMapper;
import com.cuit.interviewsystem.mapper.InterviewNoticeParticipantMapper;
import com.cuit.interviewsystem.mapper.JobApplicationMapper;
import com.cuit.interviewsystem.mapper.UserMapper;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeAddDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeCancelDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeListDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeRespondDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeUpdateDto;
import com.cuit.interviewsystem.model.entity.InterviewNotice;
import com.cuit.interviewsystem.model.entity.InterviewNoticeParticipant;
import com.cuit.interviewsystem.model.entity.JobApplication;
import com.cuit.interviewsystem.model.entity.User;
import com.cuit.interviewsystem.model.enums.InterviewNoticeStatusEnum;
import com.cuit.interviewsystem.model.enums.InterviewTypeEnum;
import com.cuit.interviewsystem.model.enums.RtcPlatformEnum;
import com.cuit.interviewsystem.model.enums.UserAccountStatusEnum;
import com.cuit.interviewsystem.model.enums.UserRoleEnum;
import com.cuit.interviewsystem.model.vo.InterviewNoticeVo;
import com.cuit.interviewsystem.service.InterviewNoticeService;
import com.cuit.interviewsystem.utils.JWTUtil;
import com.cuit.interviewsystem.utils.ThrowUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 面试预约主表 Service 实现
 */
@Service
public class InterviewNoticeServiceImpl extends ServiceImpl<InterviewNoticeMapper, InterviewNotice>
    implements InterviewNoticeService {

    @Resource
    private InterviewNoticeMapper interviewNoticeMapper;
    @Resource
    private InterviewNoticeParticipantMapper participantMapper;
    @Resource
    private JobApplicationMapper jobApplicationMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private JWTUtil jwtUtil;

    @Override
    @Transactional
    public Long addInterviewNotice(InterviewNoticeAddDto dto) {
        User currentUser = getCurrentRecruiter();
        validateCreateOrUpdateDto(dto);

        JobApplication jobApplication = jobApplicationMapper.selectById(dto.getJobApplicationId());
        ThrowUtil.throwIfTrue(jobApplication == null || jobApplication.getIsDeleted() == 1,
            ErrorEnum.PARAMS_ERROR, "投递记录不存在");
        ThrowUtil.throwIfTrue(!Objects.equals(jobApplication.getCompanyId(), currentUser.getCompanyId()),
            ErrorEnum.UNAUTHORIZED, "无权操作其他公司的投递记录");
        ThrowUtil.throwIfTrue(!Objects.equals(jobApplication.getStatus(), 2),
            ErrorEnum.PARAMS_ERROR, "仅允许对初筛通过的投递记录安排面试");

        List<Long> interviewerIds = normalizeInterviewerIds(dto.getInterviewerIds(), currentUser.getUserId());
        validateInterviewers(currentUser.getCompanyId(), interviewerIds);
        validateRtcInfo(dto);
        checkTimeConflict(null, jobApplication.getUserId(), interviewerIds,
            dto.getInterviewStartTime(), dto.getInterviewEndTime());

        InterviewNotice notice = buildNotice(dto, currentUser, jobApplication, null);
        interviewNoticeMapper.insert(notice);

        // 线上面试未传房间号时，使用面试记录ID作为默认房间号，避免前端手工输入冲突
        if (Objects.equals(dto.getInterviewType(), InterviewTypeEnum.ONLINE.getType())
            && (notice.getRtcRoomId() == null || notice.getRtcRoomId().isBlank())) {
            String generatedRoomId = String.valueOf(notice.getId());
            notice.setRtcRoomId(generatedRoomId);
            interviewNoticeMapper.update(null, new LambdaUpdateWrapper<InterviewNotice>()
                .eq(InterviewNotice::getId, notice.getId())
                .set(InterviewNotice::getRtcRoomId, generatedRoomId)
                .set(InterviewNotice::getUpdateTime, LocalDateTime.now()));
        }

        saveParticipants(notice.getId(), interviewerIds, jobApplication.getUserId());
        jobApplicationMapper.update(null, new LambdaUpdateWrapper<JobApplication>()
            .eq(JobApplication::getId, jobApplication.getId())
            .set(JobApplication::getStatus, 4)
            .set(JobApplication::getUpdateTime, LocalDateTime.now()));
        return notice.getId();
    }

    @Override
    @Transactional
    public void updateInterviewNotice(InterviewNoticeUpdateDto dto) {
        User currentUser = getCurrentRecruiter();
        validateCreateOrUpdateDto(dto);
        ThrowUtil.throwIfTrue(dto.getId() == null, ErrorEnum.PARAMS_ERROR, "面试记录ID不能为空");

        InterviewNotice notice = interviewNoticeMapper.selectById(dto.getId());
        ThrowUtil.throwIfTrue(notice == null || notice.getIsDeleted() == 1, ErrorEnum.PARAMS_ERROR, "面试记录不存在");
        ThrowUtil.throwIfTrue(!Objects.equals(notice.getCompanyId(), currentUser.getCompanyId()),
            ErrorEnum.UNAUTHORIZED, "无权修改其他公司的面试记录");
        ThrowUtil.throwIfTrue(!isEditableStatus(notice.getStatus()), ErrorEnum.OPTION_ERROR, "当前状态不允许修改面试信息");

        List<Long> interviewerIds = normalizeInterviewerIds(dto.getInterviewerIds(), currentUser.getUserId());
        validateInterviewers(currentUser.getCompanyId(), interviewerIds);
        validateRtcInfo(dto);
        checkTimeConflict(notice.getId(), notice.getJobSeekerId(), interviewerIds,
            dto.getInterviewStartTime(), dto.getInterviewEndTime());

        String resolvedRtcRoomId = dto.getRtcRoomId();
        if (Objects.equals(dto.getInterviewType(), InterviewTypeEnum.ONLINE.getType())
            && (resolvedRtcRoomId == null || resolvedRtcRoomId.isBlank())) {
            resolvedRtcRoomId = String.valueOf(notice.getId());
        }

        interviewNoticeMapper.update(null, new LambdaUpdateWrapper<InterviewNotice>()
            .eq(InterviewNotice::getId, notice.getId())
            .set(InterviewNotice::getCompanyId, notice.getCompanyId())
            .set(InterviewNotice::getJobSeekerId, notice.getJobSeekerId())
            .set(InterviewNotice::getCreatorId, currentUser.getUserId())
            .set(InterviewNotice::getInterviewType, dto.getInterviewType())
            .set(InterviewNotice::getInterviewStartTime, dto.getInterviewStartTime())
            .set(InterviewNotice::getInterviewEndTime, dto.getInterviewEndTime())
            .set(InterviewNotice::getInterviewAddress, dto.getInterviewAddress())
            .set(InterviewNotice::getRtcPlatform, dto.getRtcPlatform())
            .set(InterviewNotice::getRtcRoomId, resolvedRtcRoomId)
            .set(InterviewNotice::getRtcRoomName, dto.getRtcRoomName())
            .set(InterviewNotice::getRtcJoinUrl, dto.getRtcJoinUrl())
            .set(InterviewNotice::getRtcPassword, dto.getRtcPassword())
            .set(InterviewNotice::getComment, dto.getComment())
            .set(InterviewNotice::getStatus, InterviewNoticeStatusEnum.PENDING.getStatus())
            .set(InterviewNotice::getCandidateReplyReason, null)
            .set(InterviewNotice::getCandidateReplyTime, null)
            .set(InterviewNotice::getCancelReason, null)
            .set(InterviewNotice::getUpdateTime, LocalDateTime.now()));

        participantMapper.update(null, new LambdaUpdateWrapper<InterviewNoticeParticipant>()
            .set(InterviewNoticeParticipant::getIsDeleted, 1)
            .eq(InterviewNoticeParticipant::getInterviewNoticeId, notice.getId())
            .eq(InterviewNoticeParticipant::getIsDeleted, 0));
        saveParticipants(notice.getId(), interviewerIds, notice.getJobSeekerId());
        jobApplicationMapper.update(null, new LambdaUpdateWrapper<JobApplication>()
            .eq(JobApplication::getId, notice.getJobApplicationId())
            .set(JobApplication::getStatus, 4)
            .set(JobApplication::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void cancelInterviewNotice(InterviewNoticeCancelDto dto) {
        User currentUser = getCurrentRecruiter();
        ThrowUtil.throwIfTrue(dto.getId() == null, ErrorEnum.PARAMS_ERROR, "面试记录ID不能为空");

        InterviewNotice notice = interviewNoticeMapper.selectById(dto.getId());
        ThrowUtil.throwIfTrue(notice == null || notice.getIsDeleted() == 1, ErrorEnum.PARAMS_ERROR, "面试记录不存在");
        ThrowUtil.throwIfTrue(!Objects.equals(notice.getCompanyId(), currentUser.getCompanyId()),
            ErrorEnum.UNAUTHORIZED, "无权取消其他公司的面试记录");
        ThrowUtil.throwIfTrue(!isCancelableStatus(notice.getStatus()), ErrorEnum.OPTION_ERROR, "当前状态不允许取消面试");

        interviewNoticeMapper.update(null, new LambdaUpdateWrapper<InterviewNotice>()
            .eq(InterviewNotice::getId, notice.getId())
            .set(InterviewNotice::getStatus, InterviewNoticeStatusEnum.CANCELED.getStatus())
            .set(InterviewNotice::getCancelReason, dto.getCancelReason())
            .set(InterviewNotice::getUpdateTime, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public void respondInterviewNotice(InterviewNoticeRespondDto dto) {
        User currentUser = getCurrentJobSeeker();
        ThrowUtil.throwIfTrue(dto.getId() == null, ErrorEnum.PARAMS_ERROR, "面试记录ID不能为空");

        InterviewNotice notice = interviewNoticeMapper.selectById(dto.getId());
        ThrowUtil.throwIfTrue(notice == null || notice.getIsDeleted() == 1, ErrorEnum.PARAMS_ERROR, "面试记录不存在");
        ThrowUtil.throwIfTrue(!Objects.equals(notice.getJobSeekerId(), currentUser.getUserId()),
            ErrorEnum.UNAUTHORIZED, "无权处理其他人的面试通知");
        ThrowUtil.throwIfTrue(!Objects.equals(notice.getStatus(), InterviewNoticeStatusEnum.PENDING.getStatus()),
            ErrorEnum.OPTION_ERROR, "当前状态不允许回应面试通知");
        ThrowUtil.throwIfTrue(dto.getStatus() == null, ErrorEnum.PARAMS_ERROR, "处理状态不能为空");

        if (Objects.equals(dto.getStatus(), InterviewNoticeStatusEnum.ACCEPTED.getStatus())) {
            interviewNoticeMapper.update(null, new LambdaUpdateWrapper<InterviewNotice>()
                .eq(InterviewNotice::getId, notice.getId())
                .set(InterviewNotice::getStatus, InterviewNoticeStatusEnum.ACCEPTED.getStatus())
                .set(InterviewNotice::getCandidateReplyReason, null)
                .set(InterviewNotice::getCandidateReplyTime, LocalDateTime.now())
                .set(InterviewNotice::getUpdateTime, LocalDateTime.now()));
        } else if (Objects.equals(dto.getStatus(), InterviewNoticeStatusEnum.REJECTED.getStatus())) {
            ThrowUtil.throwIfTrue(dto.getRejectReason() == null || dto.getRejectReason().isBlank(),
                ErrorEnum.PARAMS_ERROR, "拒绝面试时请填写原因");
            interviewNoticeMapper.update(null, new LambdaUpdateWrapper<InterviewNotice>()
                .eq(InterviewNotice::getId, notice.getId())
                .set(InterviewNotice::getStatus, InterviewNoticeStatusEnum.REJECTED.getStatus())
                .set(InterviewNotice::getCandidateReplyReason, dto.getRejectReason())
                .set(InterviewNotice::getCandidateReplyTime, LocalDateTime.now())
                .set(InterviewNotice::getUpdateTime, LocalDateTime.now()));
        } else {
            ThrowUtil.throwIfTrue(true, ErrorEnum.PARAMS_ERROR, "仅支持同意或拒绝面试通知");
        }
    }

    @Override
    public Page<InterviewNoticeVo> getInterviewNoticeList(InterviewNoticeListDto dto) {
        User currentUser = getCurrentRecruiter();
        dto.setCompanyId(currentUser.getCompanyId());
        Page<InterviewNoticeVo> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        interviewNoticeMapper.getNoticeVoList(page, dto);
        enrichPage(page.getRecords());
        return page;
    }

    @Override
    public Page<InterviewNoticeVo> getOwnInterviewNoticeList(InterviewNoticeListDto dto) {
        User currentUser = getCurrentJobSeeker();
        dto.setJobSeekerId(currentUser.getUserId());
        Page<InterviewNoticeVo> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        interviewNoticeMapper.getNoticeVoList(page, dto);
        enrichPage(page.getRecords());
        return page;
    }

    @Override
    public InterviewNoticeVo getNoticeVoById(Long noticeId) {
        ThrowUtil.throwIfTrue(noticeId == null, ErrorEnum.PARAMS_ERROR, "面试通知id不能为空");
        InterviewNoticeVo notice = interviewNoticeMapper.selectNoticeVoById(noticeId);
        ThrowUtil.throwIfTrue(notice == null, ErrorEnum.PARAMS_ERROR, "面试记录不存在");
        authorizeNoticeView(notice);
        enrichNotice(notice);
        return notice;
    }

    @Override
    @Transactional
    public int autoFinishExpiredAcceptedNotices() {
        LocalDateTime now = LocalDateTime.now();
        return interviewNoticeMapper.update(null, new LambdaUpdateWrapper<InterviewNotice>()
            .eq(InterviewNotice::getIsDeleted, 0)
            .eq(InterviewNotice::getStatus, InterviewNoticeStatusEnum.ACCEPTED.getStatus())
            .le(InterviewNotice::getInterviewEndTime, now)
            .set(InterviewNotice::getStatus, InterviewNoticeStatusEnum.FINISHED.getStatus())
            .set(InterviewNotice::getUpdateTime, now));
    }

    private User getCurrentRecruiter() {
        User currentUser = jwtUtil.parseLoginUser();
        ThrowUtil.throwIfTrue(currentUser == null, ErrorEnum.NOT_LOGIN_ERROR);
        ThrowUtil.throwIfTrue(!Objects.equals(currentUser.getRole(), UserRoleEnum.RECRUITER.getValue()),
            ErrorEnum.UNAUTHORIZED, "仅招聘者可操作面试通知");
        ThrowUtil.throwIfTrue(currentUser.getCompanyId() == null, ErrorEnum.PARAMS_ERROR, "请先绑定公司");
        return currentUser;
    }

    private User getCurrentJobSeeker() {
        User currentUser = jwtUtil.parseLoginUser();
        ThrowUtil.throwIfTrue(currentUser == null, ErrorEnum.NOT_LOGIN_ERROR);
        ThrowUtil.throwIfTrue(!Objects.equals(currentUser.getRole(), UserRoleEnum.JOB_SEEKER.getValue()),
            ErrorEnum.UNAUTHORIZED, "仅求职者可操作面试通知");
        return currentUser;
    }

    private void validateCreateOrUpdateDto(InterviewNoticeAddDto dto) {
        ThrowUtil.throwIfTrue(dto.getInterviewStartTime() == null || dto.getInterviewEndTime() == null,
            ErrorEnum.PARAMS_ERROR, "面试时间不能为空");
        ThrowUtil.throwIfTrue(!dto.getInterviewEndTime().isAfter(dto.getInterviewStartTime()),
            ErrorEnum.PARAMS_ERROR, "面试结束时间必须晚于开始时间");
        ThrowUtil.throwIfTrue(dto.getInterviewerIds() == null || dto.getInterviewerIds().isEmpty(),
            ErrorEnum.PARAMS_ERROR, "至少选择一个面试官");
    }

    private void validateRtcInfo(InterviewNoticeAddDto dto) {
        if (Objects.equals(dto.getInterviewType(), InterviewTypeEnum.ONLINE.getType())) {
            ThrowUtil.throwIfTrue(dto.getRtcPlatform() == null, ErrorEnum.PARAMS_ERROR, "线上面试平台不能为空");
        } else if (Objects.equals(dto.getInterviewType(), InterviewTypeEnum.OFFLINE.getType())) {
            ThrowUtil.throwIfTrue(dto.getInterviewAddress() == null || dto.getInterviewAddress().isBlank(), ErrorEnum.PARAMS_ERROR, "线下面试地址不能为空");
        } else {
            ThrowUtil.throwIfTrue(true, ErrorEnum.PARAMS_ERROR, "面试类型参数错误");
        }
    }

    private List<Long> normalizeInterviewerIds(List<Long> interviewerIds, Long currentUserId) {
        Set<Long> uniqueIds = interviewerIds.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        ThrowUtil.throwIfTrue(uniqueIds.contains(currentUserId), ErrorEnum.PARAMS_ERROR, "面试官不能选择自己");
        return new ArrayList<>(uniqueIds);
    }

    private void validateInterviewers(Long companyId, List<Long> interviewerIds) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
            .in(User::getUserId, interviewerIds)
            .eq(User::getCompanyId, companyId)
            .eq(User::getRole, UserRoleEnum.RECRUITER.getValue())
            .eq(User::getIsDeleted, 0)
            .eq(User::getAccountStatus, UserAccountStatusEnum.NORMAL.getStatus()));
        ThrowUtil.throwIfTrue(users.size() != interviewerIds.size(), ErrorEnum.PARAMS_ERROR, "请选择同公司正常状态的招聘者作为面试官");
    }

    private void checkTimeConflict(Long excludeNoticeId, Long jobSeekerId, List<Long> interviewerIds,
                                   LocalDateTime startTime, LocalDateTime endTime) {
        List<Long> userIds = new ArrayList<>();
        userIds.add(jobSeekerId);
        userIds.addAll(interviewerIds);
        for (Long userId : userIds) {
            Integer count = interviewNoticeMapper.countUserConflict(userId, startTime, endTime, excludeNoticeId);
            ThrowUtil.throwIfTrue(count != null && count > 0, ErrorEnum.PARAMS_ERROR, "所选时间段与已有面试安排冲突");
        }
    }

    private InterviewNotice buildNotice(InterviewNoticeAddDto dto, User currentUser, JobApplication jobApplication, Long noticeId) {
        InterviewNotice notice = new InterviewNotice();
        notice.setId(noticeId);
        notice.setJobApplicationId(jobApplication == null ? null : jobApplication.getId());
        notice.setCompanyId(currentUser.getCompanyId());
        notice.setJobSeekerId(jobApplication == null ? null : jobApplication.getUserId());
        notice.setCreatorId(currentUser.getUserId());
        notice.setInterviewType(dto.getInterviewType());
        notice.setInterviewStartTime(dto.getInterviewStartTime());
        notice.setInterviewEndTime(dto.getInterviewEndTime());
        notice.setInterviewAddress(dto.getInterviewAddress());
        notice.setRtcPlatform(dto.getRtcPlatform());
        notice.setRtcRoomId(dto.getRtcRoomId());
        notice.setRtcRoomName(dto.getRtcRoomName());
        notice.setRtcJoinUrl(dto.getRtcJoinUrl());
        notice.setRtcPassword(dto.getRtcPassword());
        notice.setComment(dto.getComment());
        notice.setStatus(InterviewNoticeStatusEnum.PENDING.getStatus());
        notice.setCandidateReplyReason(null);
        notice.setCandidateReplyTime(null);
        notice.setCancelReason(null);
        notice.setUpdateTime(LocalDateTime.now());
        if (noticeId == null) {
            notice.setCreateTime(LocalDateTime.now());
        }
        return notice;
    }

    private void saveParticipants(Long noticeId, List<Long> interviewerIds, Long jobSeekerId) {
        if (interviewerIds.isEmpty()) {
            return;
        }
        List<InterviewNoticeParticipant> participants = new ArrayList<>();
        for (int i = 0; i < interviewerIds.size(); i++) {
            Long userId = interviewerIds.get(i);
            InterviewNoticeParticipant participant = new InterviewNoticeParticipant();
            participant.setInterviewNoticeId(noticeId);
            participant.setUserId(userId);
            participant.setParticipantType(1);
            participant.setIsPrimary(i == 0 ? 1 : 0);
            participants.add(participant);
        }
        InterviewNoticeParticipant candidate = new InterviewNoticeParticipant();
        candidate.setInterviewNoticeId(noticeId);
        candidate.setUserId(jobSeekerId);
        candidate.setParticipantType(2);
        candidate.setIsPrimary(0);
        participants.add(candidate);
        for (InterviewNoticeParticipant participant : participants) {
            participantMapper.insert(participant);
        }
    }

    private void enrichPage(List<InterviewNoticeVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (InterviewNoticeVo record : records) {
            enrichNotice(record);
        }
    }

    private void enrichNotice(InterviewNoticeVo notice) {
        if (notice == null) {
            return;
        }
        notice.setStatusText(resolveStatusText(notice.getStatus()));
        notice.setInterviewTypeText(resolveInterviewTypeText(notice.getInterviewType()));
        notice.setRtcPlatformText(resolveRtcPlatformText(notice.getRtcPlatform()));
        notice.setInterviewers(interviewNoticeMapper.selectParticipantsByNoticeId(notice.getId())
            .stream()
            .filter(item -> Objects.equals(item.getParticipantType(), 1))
            .collect(Collectors.toList()));
    }

    private String resolveStatusText(Integer status) {
        InterviewNoticeStatusEnum statusEnum = InterviewNoticeStatusEnum.getEnum(status);
        return statusEnum == null ? null : statusEnum.getText();
    }

    private String resolveInterviewTypeText(Integer type) {
        InterviewTypeEnum typeEnum = InterviewTypeEnum.getEnum(type);
        return typeEnum == null ? null : typeEnum.getText();
    }

    private String resolveRtcPlatformText(Integer type) {
        RtcPlatformEnum platformEnum = RtcPlatformEnum.getEnum(type);
        return platformEnum == null ? null : platformEnum.getText();
    }

    private boolean isEditableStatus(Integer status) {
        return Objects.equals(status, InterviewNoticeStatusEnum.PENDING.getStatus())
            || Objects.equals(status, InterviewNoticeStatusEnum.REJECTED.getStatus())
            || Objects.equals(status, InterviewNoticeStatusEnum.CANCELED.getStatus());
    }

    private boolean isCancelableStatus(Integer status) {
        return Objects.equals(status, InterviewNoticeStatusEnum.PENDING.getStatus())
            || Objects.equals(status, InterviewNoticeStatusEnum.REJECTED.getStatus());
    }

    private void authorizeNoticeView(InterviewNoticeVo notice) {
        User currentUser = jwtUtil.parseLoginUser();
        ThrowUtil.throwIfTrue(currentUser == null, ErrorEnum.NOT_LOGIN_ERROR);
        if (Objects.equals(currentUser.getRole(), UserRoleEnum.RECRUITER.getValue())) {
            ThrowUtil.throwIfTrue(!Objects.equals(currentUser.getCompanyId(), notice.getCompanyId()),
                ErrorEnum.UNAUTHORIZED, "无权查看其他公司的面试记录");
            return;
        }
        ThrowUtil.throwIfTrue(!Objects.equals(currentUser.getRole(), UserRoleEnum.JOB_SEEKER.getValue())
                || !Objects.equals(currentUser.getUserId(), notice.getJobSeekerId()),
            ErrorEnum.UNAUTHORIZED, "无权查看其他人的面试记录");
    }
}
