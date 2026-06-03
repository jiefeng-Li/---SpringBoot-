package com.cuit.interviewsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.mapper.CompanyMapper;
import com.cuit.interviewsystem.mapper.JobPositionMapper;
import com.cuit.interviewsystem.mapper.ResumeMapper;
import com.cuit.interviewsystem.model.dto.jobApplication.OwnJobApplicationPageDto;
import com.cuit.interviewsystem.model.dto.jobApplication.UpdateJobApplicationBatchDto;
import com.cuit.interviewsystem.model.dto.jobApplication.UpdateJobApplicationDto;
import com.cuit.interviewsystem.model.dto.jobApplication.AddJobApplicationDto;
import com.cuit.interviewsystem.model.dto.jobApplication.JobApplicationPageDto;
import com.cuit.interviewsystem.model.entity.Company;
import com.cuit.interviewsystem.model.entity.JobApplication;
import com.cuit.interviewsystem.model.entity.JobPosition;
import com.cuit.interviewsystem.model.enums.CompanyStatusEnum;
import com.cuit.interviewsystem.model.enums.JobApplicationStatusEnum;
import com.cuit.interviewsystem.model.enums.JobPositionStatusEnum;
import com.cuit.interviewsystem.model.vo.JobApplicationVo;
import com.cuit.interviewsystem.model.vo.ResumeVo;
import com.cuit.interviewsystem.service.JobApplicationService;
import com.cuit.interviewsystem.mapper.JobApplicationMapper;
import com.cuit.interviewsystem.utils.RedisUtil;
import com.cuit.interviewsystem.utils.ThrowUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

/**
* @author jiefe
* @description 针对表【t_job_application(简历投递记录表)】的数据库操作Service实现
* @createDate 2026-03-01 21:08:39
*/
@Service
public class JobApplicationServiceImpl extends ServiceImpl<JobApplicationMapper, JobApplication>
    implements JobApplicationService{

    @Resource
    private JobApplicationMapper jobApplicationMapper;
    @Resource
    private JobPositionMapper jobPositionMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private ResumeMapper resumeMapper;
    @Resource
    private RedisUtil redisUtil;

    @Override
    public JobApplicationVo getJobApplicationById(Long id) {
        ThrowUtil.throwIfTrue(id == null, ErrorEnum.PARAMS_ERROR, "投递记录ID不能为空");
        if (redisUtil.hasKey("jobApplication:" + id)) {
            return (JobApplicationVo) redisUtil.get("jobApplication:" + id);
        }
        JobApplicationVo res = jobApplicationMapper.getJobApplicationById(id);
        res.setResume(resumeMapper.selectVoById(res.getResumeId()));
        return res;
    }

    @Override
    public Page<JobApplicationVo> getOwnApplicationList(OwnJobApplicationPageDto pageDto) {
        Page<JobApplicationVo> page = new Page<>(pageDto.getPageNum(), pageDto.getPageSize());
        ThrowUtil.throwIfTrue(pageDto.getUserId() == null, ErrorEnum.PARAMS_ERROR, "用户ID不能为空");
        jobApplicationMapper.getOwnJobApplicationPage(page, pageDto);
        for (JobApplicationVo apply : page.getRecords()) {
            ResumeVo resume = resumeMapper.selectVoById(apply.getResumeId());
            if (resume != null)
                apply.setResume(resume);
        }
        return page;
    }

/**
 * 获取职位申请列表
 * @param jobApplicationPageDto 职位申请分页参数
 * @return 返回分页后的职位申请列表
 */
    @Override
    public Page<JobApplicationVo> getJobApplicationList(JobApplicationPageDto jobApplicationPageDto) {
    // 创建分页对象，设置当前页码和每页大小
        Page<JobApplicationVo> page = new Page<>(jobApplicationPageDto.getPageNum(), jobApplicationPageDto.getPageSize());
    // 调用Mapper查询职位申请分页数据
        jobApplicationMapper.getJobApplicationPage(page, jobApplicationPageDto);
    // 遍历分页结果，为每个申请添加简历信息
        for (JobApplicationVo apply : page.getRecords()) {
        // 根据简历ID查询简历信息
            ResumeVo resume = resumeMapper.selectVoById(apply.getResumeId());
        // 如果简历存在，则设置到申请对象中
            if (resume != null)
                apply.setResume(resume);
        }
    // 返回包含完整信息的分页结果
        return page;
    }

/**
 * 添加职位申请的方法
 * @param addJobApplicationDto 包含职位申请信息的DTO对象
 */
    @Override
    public void addJobApplication(AddJobApplicationDto addJobApplicationDto) {
        JobPosition job = jobPositionMapper.selectById(addJobApplicationDto.getJobPositionId());
        Company comp = companyMapper.selectById(addJobApplicationDto.getCompanyId());
    // 检查公司是否被封禁，如果被封禁则抛出异常
        ThrowUtil.throwIfTrue(comp.getStatus() == CompanyStatusEnum.BANED.getStatus(), ErrorEnum.NOT_FOUND_ERROR, "公司已被封禁");
    // 检查职位是否存在或已被删除，如果存在则抛出异常
        ThrowUtil.throwIfTrue(job == null || job.getIsDeleted() == 1, ErrorEnum.PARAMS_ERROR, "职位不存在");
    // 检查职位是否在招聘状态，如果不在招聘状态则抛出异常
        ThrowUtil.throwIfTrue(job.getStatus() != JobPositionStatusEnum.RECRUITING, ErrorEnum.PARAMS_ERROR, "职位不在招聘状态");
    // 检请的公司ID与职位中的公司ID是否一致，如果不一致则抛出异常
        ThrowUtil.throwIfTrue(!Objects.equals(job.getCompanyId(), addJobApplicationDto.getCompanyId()), ErrorEnum.PARAMS_ERROR, "公司ID错误");
    // 创建新的职位申请对象
        JobApplication jobApplication = new JobApplication();
    // 检查用户是否已经申请过该职位，如果申请过且状态为已查看、待处理、面试中、已录用或已发offer，则抛出异常
        ThrowUtil.throwIfTrue(jobApplicationMapper.exists(new LambdaQueryWrapper<JobApplication>()
                        .eq(JobApplication::getJobPositionId, addJobApplicationDto.getJobPositionId())
                        .eq(JobApplication::getUserId, addJobApplicationDto.getUserId())
                        .in(JobApplication::getStatus,
                                JobApplicationStatusEnum.VIEWED,
                                JobApplicationStatusEnum.PENDING,
                                JobApplicationStatusEnum.INTERVIEWING,
                                JobApplicationStatusEnum.HIRED,
                                JobApplicationStatusEnum.OFFER_SENT)),
                ErrorEnum.PARAMS_ERROR, "已投递过该职位");
    // 将DTO中的属性复制到职位申请对象中
        BeanUtils.copyProperties(addJobApplicationDto, jobApplication);
    // 设置申请状态为"待处理"
        jobApplication.setStatus(JobApplicationStatusEnum.PENDING.getStatus());
    // 将职位申请信息插入数据库
        jobApplicationMapper.insert(jobApplication);
    }

    @Override
    public int deleteJobApplication(Long id) {
        ThrowUtil.throwIfTrue(id == null, ErrorEnum.PARAMS_ERROR, "投递记录ID不能为空");
        JobApplication apply = jobApplicationMapper.selectById(id);
        ThrowUtil.throwIfTrue(apply == null || apply.getIsDeleted() == 1, ErrorEnum.PARAMS_ERROR, "投递记录不存在");
        apply.setIsDeleted(1);
        redisUtil.delete("jobApplication:" + id);
        return jobApplicationMapper.update(apply, new LambdaUpdateWrapper<>(JobApplication.class)
                .eq(JobApplication::getId, id));
    }

    @Override
    public int updateJobApplicationStatus(UpdateJobApplicationDto jobApplication) {
        JobApplication apply = jobApplicationMapper.selectById(jobApplication.getId());
        Company comp = companyMapper.selectById(apply.getCompanyId());
        ThrowUtil.throwIfTrue(comp.getStatus() == CompanyStatusEnum.BANED.getStatus(), ErrorEnum.NOT_FOUND_ERROR, "公司已被封禁");
        ThrowUtil.throwIfTrue(apply == null || apply.getIsDeleted() == 1, ErrorEnum.PARAMS_ERROR, "投递记录不存在");
        if (jobApplication.getStatus() != null) {
            ThrowUtil.throwIfTrue(!JobApplicationStatusEnum.checkStatus(apply.getStatus(), jobApplication.getStatus()),
                    ErrorEnum.PARAMS_ERROR, "状态错误");
            apply.setStatus(jobApplication.getStatus());
        }
        apply.setRemarks(jobApplication.getRemarks());
        apply.setUpdateTime(LocalDate.now());
        redisUtil.delete("jobApplication:" + jobApplication.getId());
        return jobApplicationMapper.update(apply, new LambdaUpdateWrapper<>(JobApplication.class)
                .eq(JobApplication::getId, jobApplication.getId()));
    }

    @Override
    @Transactional
    public int updateJobApplicationStatusBatch(UpdateJobApplicationBatchDto dto) {
        ThrowUtil.throwIfTrue(dto.getIds() == null || dto.getIds().isEmpty(), ErrorEnum.PARAMS_ERROR, "投递记录ID列表不能为空");
        ThrowUtil.throwIfTrue(dto.getStatus() == null, ErrorEnum.PARAMS_ERROR, "目标状态不能为空");

        int successCount = 0;
        for (Long id : dto.getIds()) {
            JobApplication apply = jobApplicationMapper.selectById(id);
            ThrowUtil.throwIfTrue(apply == null || apply.getIsDeleted() == 1, ErrorEnum.PARAMS_ERROR, "投递记录不存在");
            ThrowUtil.throwIfTrue(!JobApplicationStatusEnum.checkStatus(apply.getStatus(), dto.getStatus()),
                    ErrorEnum.PARAMS_ERROR, "投递记录状态流转不合法: " + id);

            apply.setStatus(dto.getStatus());
            apply.setRemarks(dto.getRemarks());
            apply.setUpdateTime(LocalDate.now());
            int updated = jobApplicationMapper.update(apply, new LambdaUpdateWrapper<>(JobApplication.class)
                    .eq(JobApplication::getId, id));
            if (updated > 0) {
                successCount++;
                redisUtil.delete("jobApplication:" + id);
            }
        }
        return successCount;
    }
}




