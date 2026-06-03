package com.cuit.interviewsystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cuit.interviewsystem.annotation.AuthCheck;
import com.cuit.interviewsystem.common.Result;
import com.cuit.interviewsystem.constant.JobTags;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.model.dto.job.UpdateJobDto;
import com.cuit.interviewsystem.model.dto.job.AddJobDto;
import com.cuit.interviewsystem.model.dto.job.JobSearchPageDto;
import com.cuit.interviewsystem.model.entity.JobPosition;
import com.cuit.interviewsystem.model.entity.User;
import com.cuit.interviewsystem.model.enums.UserRoleEnum;
import com.cuit.interviewsystem.model.vo.JobPositionVo;
import com.cuit.interviewsystem.model.vo.PageVo;
import com.cuit.interviewsystem.service.JobRecommendationService;
import com.cuit.interviewsystem.service.CompanyService;
import com.cuit.interviewsystem.service.JobPositionService;
import com.cuit.interviewsystem.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/job")
@Tag(name = "职位管理")
public class JobPositionController {
    @Resource
    private JobPositionService jobPositionService;
    @Resource
    private CompanyService companyService;
    @Resource
    private UserService userService;
    @Resource
    private JobRecommendationService jobRecommendationService;


    @PostMapping("")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER})
    public Result<?> addJobPosition(@Valid @RequestBody AddJobDto addJobDto){
        return jobPositionService.addJobPosition(addJobDto) == 0 ?
                Result.error(ErrorEnum.SYSTEM_ERROR) : Result.success("添加成功");
    }

    @DeleteMapping("/{id}")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER, UserRoleEnum.COMP_ADMIN})
    public Result<?> deleteJobPosition(@PathVariable Long id){
        return jobPositionService.deleteJobPosition(id) == 0 ?
                Result.error(ErrorEnum.SYSTEM_ERROR) : Result.success("删除成功");
    }

    @PutMapping("/{id}")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER})
    public Result<?> updateJobPosition(@Valid @RequestBody UpdateJobDto updateJobDto, @PathVariable Long id){
        int i = jobPositionService.updateJobPosition(updateJobDto, id);
        return i == 0 ? Result.error(ErrorEnum.SYSTEM_ERROR) : Result.success("更新成功");
    }

    @GetMapping("/view")
    @AuthCheck
    public Result<?> getJobPositionByCompanyId(Long jobId){
        jobPositionService.viewJobPosition(jobId);
        return Result.success();
    }

    @GetMapping("/tags")
    public Result<String[]> getJobTags(){
        return Result.success(JobTags.TAGS);
    }

    @GetMapping("")
    public Result<JobPositionVo> getJobPositionById(Long id){
        JobPosition jobPosition = jobPositionService.getJobPositionById(id);
        JobPositionVo res = JobPositionVo.objToVo(jobPosition);
        if (res != null) {
            res.setCompanyName(companyService.getCompanyById(jobPosition.getCompanyId()).getCompanyName());
            User hr = userService.getById(jobPosition.getHiringManagerId());
            res.setHiringManagerName(hr.getUsername());
            res.setHiringManagerAvatar(hr.getAvatarUrl());
        }
        return Result.success(res);
    }

    @PutMapping("/status/{id}")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER})
    public Result<?> updateJobPositionStatus(@PathVariable Long id, Integer status){
        return jobPositionService.updateJobStatus(id, status) == 0 ?
                Result.error(ErrorEnum.SYSTEM_ERROR) : Result.success("更新成功");
    }

/**
 * 获取职位列表接口
 * @param jobSearchPageDto 职位搜索分页参数DTO
 * @return 返回分页后的职位列表数据
 */
    @GetMapping("/list")
    public Result<PageVo<JobPositionVo>> getJobPositionList(@Valid JobSearchPageDto jobSearchPageDto){
    // 调用服务层获取职位列表数据
        Page<JobPosition> res = jobPositionService.getJobPositionList(jobSearchPageDto);
    // 创建新的分页对象用于返回结果
        Page<JobPositionVo> r = new Page<>();
    // 获取查询结果中的记录列表
        List<JobPosition> records = res.getRecords();
        List<JobPositionVo> list = new ArrayList<>();
    // 遍历职位记录列表
        for (JobPosition record : records) {
        // 将实体对象转换为VO对象
            JobPositionVo jobPositionVo = JobPositionVo.objToVo(record);
            if (jobPositionVo != null) {
            // 设置公司名称
                jobPositionVo.setCompanyName(companyService.getCompanyById(record.getCompanyId()).getCompanyName());
            // 获取招聘经理信息
                User hr = userService.getById(record.getHiringManagerId());
            // 设置招聘经理姓名和头像
                jobPositionVo.setHiringManagerName(hr.getUsername());
                jobPositionVo.setHiringManagerAvatar(hr.getAvatarUrl());
            // 将处理后的VO对象添加到列表中
                list.add(jobPositionVo);
            }
        }
    // 复制分页属性，但不复制records属性
        BeanUtils.copyProperties(res, r, "records");
    // 设置处理后的记录列表
        r.setRecords(list);
    // 返回成功响应，包含分页数据
        return Result.success(PageVo.of(r));
    }

    @GetMapping("/recommendations")
    @AuthCheck
    public Result<List<JobPositionVo>> getRecommendedJobs(@RequestParam(required = false, defaultValue = "8") Integer limit) {
        List<JobPositionVo> jobs = jobRecommendationService.recommendJobsForCurrentUser(limit);
        if (jobs == null || jobs.isEmpty()) {
            JobSearchPageDto dto = new JobSearchPageDto();
            dto.setPageNum(1L);
            dto.setPageSize(Long.valueOf(limit));
            Result<PageVo<JobPositionVo>> res = getJobPositionList(dto);
            PageVo<JobPositionVo> data = (PageVo<JobPositionVo>) res.getData();
            return Result.success(data.getList());
        }
        for (JobPositionVo job : jobs) {
            if (job.getCompanyId() != null) {
                var company = companyService.getCompanyById(job.getCompanyId());
                if (company != null) {
                    job.setCompanyName(company.getCompanyName());
                }
            }
            if (job.getHiringManagerId() != null) {
                User hr = userService.getById(job.getHiringManagerId());
                if (hr != null) {
                    job.setHiringManagerName(hr.getUsername());
                    job.setHiringManagerAvatar(hr.getAvatarUrl());
                }
            }
        }
        return Result.success(jobs);
    }
}
