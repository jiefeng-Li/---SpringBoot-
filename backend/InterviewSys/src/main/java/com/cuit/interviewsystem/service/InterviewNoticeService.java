package com.cuit.interviewsystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeAddDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeCancelDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeListDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeRespondDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeUpdateDto;
import com.cuit.interviewsystem.model.entity.InterviewNotice;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cuit.interviewsystem.model.vo.InterviewNoticeVo;

/**
* @author jiefe
* @description 针对表【t_interview_notice(面试通知表)】的数据库操作Service
* @createDate 2026-03-27 03:26:41
*/
public interface InterviewNoticeService extends IService<InterviewNotice> {
    Long addInterviewNotice(InterviewNoticeAddDto dto);

    void updateInterviewNotice(InterviewNoticeUpdateDto dto);

    void cancelInterviewNotice(InterviewNoticeCancelDto dto);

    void respondInterviewNotice(InterviewNoticeRespondDto dto);

    Page<InterviewNoticeVo> getInterviewNoticeList(InterviewNoticeListDto dto);

    Page<InterviewNoticeVo> getOwnInterviewNoticeList(InterviewNoticeListDto dto);

    InterviewNoticeVo getNoticeVoById(Long noticeId);

    /**
     * 将已到结束时间且仍处于“已确认”的面试记录自动置为“已结束”。
     *
     * @return 本次更新的记录数
     */
    int autoFinishExpiredAcceptedNotices();
}
