package com.cuit.interviewsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cuit.interviewsystem.annotation.AuthCheck;
import com.cuit.interviewsystem.common.Result;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeAddDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeCancelDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeListDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeRespondDto;
import com.cuit.interviewsystem.model.dto.interview.InterviewNoticeUpdateDto;
import com.cuit.interviewsystem.model.enums.UserRoleEnum;
import com.cuit.interviewsystem.model.vo.InterviewNoticeVo;
import com.cuit.interviewsystem.model.vo.PageVo;
import com.cuit.interviewsystem.service.InterviewNoticeService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 面试通知Controller
 */
@RestController
@RequestMapping("/interviewNotice")
public class InterviewNoticeController {

    @Resource
    private InterviewNoticeService interviewNoticeService;

    /**
     * 添加面试通知
     * @param dto 面试通知添加DTO
     * @return 操作结果
     */
    @PostMapping("/add")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER})
    public Result<Void> addInterviewNotice(@Valid @RequestBody InterviewNoticeAddDto dto) {
        interviewNoticeService.addInterviewNotice(dto);
        return Result.success();
    }

    /**
     * 更新面试通知
     */
    @PutMapping("/update")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER})
    public Result<Void> updateInterviewNotice(@Valid @RequestBody InterviewNoticeUpdateDto dto) {
        interviewNoticeService.updateInterviewNotice(dto);
        return Result.success();
    }

    /**
     * 取消面试通知
     */
    @PostMapping("/cancel")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER})
    public Result<Void> cancelInterviewNotice(@Valid @RequestBody InterviewNoticeCancelDto dto) {
        interviewNoticeService.cancelInterviewNotice(dto);
        return Result.success();
    }

    /**
     * 求职者响应面试通知
     */
    @PostMapping("/response")
    @AuthCheck(roles = {UserRoleEnum.JOB_SEEKER})
    public Result<Void> respondInterviewNotice(@Valid @RequestBody InterviewNoticeRespondDto dto) {
        interviewNoticeService.respondInterviewNotice(dto);
        return Result.success();
    }

    /**
     * 获取面试通知列表
     * @param dto 查询条件DTO
     * @return 面试通知分页列表
     */
    @GetMapping("/list")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER})
    public Result<PageVo<InterviewNoticeVo>> getInterviewNoticeList(InterviewNoticeListDto dto) {
        Page<InterviewNoticeVo> page = interviewNoticeService.getInterviewNoticeList(dto);
        return Result.success(PageVo.of(page));
    }

    /**
     * 获取求职者自己的面试通知列表
     */
    @GetMapping("/list/own")
    @AuthCheck(roles = {UserRoleEnum.JOB_SEEKER})
    public Result<PageVo<InterviewNoticeVo>> getOwnInterviewNoticeList(InterviewNoticeListDto dto) {
        Page<InterviewNoticeVo> page = interviewNoticeService.getOwnInterviewNoticeList(dto);
        return Result.success(PageVo.of(page));
    }

    /**
     * 获取面试通知详情
     * @param noticeId 面试通知ID
     * @return 面试通知详情
     */
    @GetMapping
    @AuthCheck(roles = {UserRoleEnum.RECRUITER, UserRoleEnum.JOB_SEEKER})
    public Result<InterviewNoticeVo> getNoticeById(@RequestParam Long noticeId) {
        return Result.success(interviewNoticeService.getNoticeVoById(noticeId));
    }
}
