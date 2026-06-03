package com.cuit.interviewsystem.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeListDto;
import com.cuit.interviewsystem.model.entity.InterviewNotice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cuit.interviewsystem.model.vo.InterviewParticipantVo;
import com.cuit.interviewsystem.model.vo.InterviewNoticeVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author jiefe
* @description 针对表【t_interview_notice(面试通知表)】的数据库操作Mapper
* @createDate 2026-03-27 03:26:41
* @Entity generator.domain.InterviewNotice
*/
@Mapper
public interface InterviewNoticeMapper extends BaseMapper<InterviewNotice> {

    IPage<InterviewNoticeVo> getNoticeVoList(@Param("page") Page<InterviewNoticeVo> page,
                                             @Param("dto") InterviewNoticeListDto dto);

    InterviewNoticeVo selectNoticeVoById(@Param("noticeId") Long noticeId);

    Integer countUserConflict(@Param("userId") Long userId,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime,
                              @Param("excludeId") Long excludeId);

    List<InterviewParticipantVo> selectParticipantsByNoticeId(@Param("noticeId") Long noticeId);
}




