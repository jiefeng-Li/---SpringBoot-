package com.cuit.interviewsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cuit.interviewsystem.exception.BusinessException;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.mapper.*;
import com.cuit.interviewsystem.model.dto.resume.*;
import com.cuit.interviewsystem.model.entity.*;
import com.cuit.interviewsystem.model.enums.JobApplicationStatusEnum;
import com.cuit.interviewsystem.model.enums.UserRoleEnum;
import com.cuit.interviewsystem.model.vo.ResumeEducationVo;
import com.cuit.interviewsystem.model.vo.ResumeExperienceVo;
import com.cuit.interviewsystem.model.vo.ResumeProjectVo;
import com.cuit.interviewsystem.model.vo.ResumeVo;
import com.cuit.interviewsystem.service.*;
import com.cuit.interviewsystem.utils.AliOSSUtil;
import com.cuit.interviewsystem.utils.JWTUtil;
import com.cuit.interviewsystem.utils.ThrowUtil;
import jakarta.annotation.Resource;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
* @author jiefe
* @description 针对表【t_resume】的数据库操作Service实现
* @createDate 2026-02-23 21:03:31
*/
@Service
public class ResumeServiceImpl extends ServiceImpl<ResumeMapper, Resume>
    implements ResumeService{
    @Resource
    private JWTUtil jwtUtil;
    @Resource
    private ResumeMapper resumeMapper;
    @Resource
    private ResumeEducationMapper educationMapper;
    @Resource
    private ResumeEducationService educationService;
    @Resource
    private ResumeExperienceMapper experienceMapper;
    @Resource
    private ResumeExperienceService experienceService;
    @Resource
    private ResumeProjectMapper projectMapper;
    @Resource
    private ResumeProjectService projectService;
    @Resource
    private JobApplicationMapper jobApplicationMapper;
    @Resource
    private AliOSSUtil aliOSSUtil;

    @Override
    @Transactional
    public Long addResume(AddResumeDto resume, MultipartFile avatar) {
        Resume baseInfo = new Resume();
        BeanUtils.copyProperties(resume, baseInfo);

        if (avatar != null && !avatar.isEmpty()) {
            try {
                String avatarUrl = aliOSSUtil.uploadPictureAndCheck(avatar, baseInfo.getUserId().toString());
                baseInfo.setAvatar(avatarUrl);
                resumeMapper.updateById(baseInfo);
            } catch (Exception e) {
                throw new BusinessException(ErrorEnum.PARAMS_ERROR, "上传头像失败");
            }
        }
        resumeMapper.insert(baseInfo);
        long resumeId = baseInfo.getId();
        saveResumeInfo(resume, resumeId);
        return resumeId;
    }

    @Override
    public Page<ResumeVo> getResumesByUserId(ResumePageDto resumePageDto) {
        Page<ResumeVo> page = new Page<>(resumePageDto.getPageNum(), resumePageDto.getPageSize());
        resumeMapper.getResumesByUserId(page, resumePageDto.getUserId());
        return page;
    }

    @Override
    public ResumeVo getResumeById(Long id) {
        ThrowUtil.throwIfTrue(id == null, ErrorEnum.PARAMS_ERROR, "简历id不能为空");
        ResumeVo resumeVo = resumeMapper.selectVoById(id);
        if (resumeVo == null) {
            return null;
        }

        // 强制单表查询并覆盖，避免联表映射在一对多场景下漏项
        List<ResumeEducation> educations = educationMapper.selectList(
                new LambdaQueryWrapper<ResumeEducation>()
                        .eq(ResumeEducation::getResumeId, id)
                        .orderByAsc(ResumeEducation::getId)
        );
        List<ResumeEducationVo> educationVos = new ArrayList<>();
        if (educations != null && !educations.isEmpty()) {
            educationVos = new ArrayList<>(educations.size());
            for (ResumeEducation education : educations) {
                ResumeEducationVo vo = new ResumeEducationVo();
                BeanUtils.copyProperties(education, vo);
                educationVos.add(vo);
            }
        }
        resumeVo.setEducations(educationVos);

        List<ResumeExperience> experiences = experienceMapper.selectList(
                new LambdaQueryWrapper<ResumeExperience>()
                        .eq(ResumeExperience::getResumeId, id)
                        .orderByAsc(ResumeExperience::getId)
        );
        List<ResumeExperienceVo> experienceVos = new ArrayList<>();
        if (experiences != null && !experiences.isEmpty()) {
            experienceVos = new ArrayList<>(experiences.size());
            for (ResumeExperience experience : experiences) {
                ResumeExperienceVo vo = new ResumeExperienceVo();
                vo.setId(experience.getId());
                vo.setCompany(experience.getCompany());
                vo.setPosition(experience.getPosition());
                vo.setDescription(experience.getDescription());
                vo.setStartDate(experience.getStartDate() == null ? null : Date.valueOf(experience.getStartDate()));
                vo.setEndDate(experience.getEndDate() == null ? null : Date.valueOf(experience.getEndDate()));
                experienceVos.add(vo);
            }
        }
        resumeVo.setExperiences(experienceVos);

        List<ResumeProject> projects = projectMapper.selectList(
                new LambdaQueryWrapper<ResumeProject>()
                        .eq(ResumeProject::getResumeId, id)
                        .orderByAsc(ResumeProject::getId)
        );
        List<ResumeProjectVo> projectVos = new ArrayList<>();
        if (projects != null && !projects.isEmpty()) {
            projectVos = new ArrayList<>(projects.size());
            for (ResumeProject project : projects) {
                ResumeProjectVo vo = new ResumeProjectVo();
                vo.setId(project.getId());
                vo.setName(project.getName());
                vo.setDescription(project.getDescription());
                vo.setStartDate(project.getStartDate() == null ? null : Date.valueOf(project.getStartDate()));
                vo.setEndDate(project.getEndDate() == null ? null : Date.valueOf(project.getEndDate()));
                projectVos.add(vo);
            }
        }
        resumeVo.setProjects(projectVos);

        return resumeVo;
    }

    @Override
    public ResumeVo getDefaultResumeByUserId(Long userId) {
        ThrowUtil.throwIfTrue(userId == null, ErrorEnum.PARAMS_ERROR, "用户id不能为空");
        Resume defaultResume;
        try {
            defaultResume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                    .eq(Resume::getUserId, userId)
                    .eq(Resume::getIsDefault, 1)
                    .eq(Resume::getIsDeleted, 0)
                    .orderByDesc(Resume::getUpdateTime)
                    .last("limit 1"));
        } catch (TooManyResultsException e) {
            throw new BusinessException(ErrorEnum.SYSTEM_ERROR, "存在多个默认简历");
        }
        return defaultResume == null ? null : getResumeById(defaultResume.getId());
    }

    @Override
    @Transactional
    public void updateResume(AddResumeDto resume, Long id, MultipartFile avatar) {
        Resume target = resumeMapper.selectById(id);
        ThrowUtil.throwIfTrue(target == null || target.getIsDeleted() == 1,
                ErrorEnum.PARAMS_ERROR, "简历不存在或已被删除");
        educationMapper.delete(new LambdaQueryWrapper<ResumeEducation>().eq(ResumeEducation::getResumeId, id));
        experienceMapper.delete(new LambdaQueryWrapper<ResumeExperience>().eq(ResumeExperience::getResumeId, id));
        projectMapper.delete(new LambdaQueryWrapper<ResumeProject>().eq(ResumeProject::getResumeId, id));
        BeanUtils.copyProperties(resume, target);
        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl;
            try {
                avatarUrl = aliOSSUtil.uploadPictureAndCheck(avatar, target.getUserId().toString());
            } catch (Exception e) {
                throw new BusinessException(ErrorEnum.PARAMS_ERROR, "上传头像失败");
            }
            aliOSSUtil.deleteFile(target.getAvatar());
            target.setAvatar(avatarUrl);
        }
        saveResumeInfo(resume, id);
        resumeMapper.update(target, new LambdaUpdateWrapper<Resume>().eq(Resume::getId, id));
    }

    @Override
    public void deleteResume(Long id) {
        ThrowUtil.throwIfTrue(id == null, ErrorEnum.PARAMS_ERROR, "简历id不能为空");
        Resume resume = resumeMapper.selectById(id);
        ThrowUtil.throwIfTrue(resume == null || resume.getIsDeleted() == 1, ErrorEnum.PARAMS_ERROR, "简历不存在或已被删除");
        User curUser = jwtUtil.parseLoginUser();
        if (!resume.getUserId().equals(curUser.getUserId()) && !UserRoleEnum.SYS_ADMIN.getValue().equals(curUser.getRole()))
            throw new BusinessException(ErrorEnum.UNAUTHORIZED, "无权限删除");
        if (jobApplicationMapper.exists(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getResumeId, id)
                .eq(JobApplication::getIsDeleted, 0)
                .notIn(JobApplication::getStatus,
                        JobApplicationStatusEnum.ELIMINATED.getStatus(),
                        JobApplicationStatusEnum.HIRED.getStatus(),
                        JobApplicationStatusEnum.PENDING.getStatus(),
                        JobApplicationStatusEnum.HIRED.getStatus()))) {
            throw new BusinessException(ErrorEnum.PARAMS_ERROR, "简历已被申请，无法删除");
        }
        resumeMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long id) {
        ThrowUtil.throwIfTrue(id == null, ErrorEnum.PARAMS_ERROR, "简历id不能为空");
        Resume resume = resumeMapper.selectById(id);
        ThrowUtil.throwIfTrue(resume == null || resume.getIsDeleted() == 1, ErrorEnum.PARAMS_ERROR, "简历不存在或已被删除");
        User curUser = jwtUtil.parseLoginUser();
        if (!resume.getUserId().equals(curUser.getUserId()) && !UserRoleEnum.SYS_ADMIN.getValue().equals(curUser.getRole()))
            throw new BusinessException(ErrorEnum.UNAUTHORIZED, "无权限设置默认");
        if (resume.getIsDefault() != 1) {
            resume.setIsDefault(1);
            try {
                Resume old = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, resume.getUserId())
                        .eq(Resume::getIsDefault, 1)
                        .eq(Resume::getIsDeleted, 0));
                //当用户只有一个简历，或者并没有设置默认简历的时候 old可能为空
                if (old != null) {
                    old.setIsDefault(0);
                    resumeMapper.updateById(old);
                }
            } catch (TooManyResultsException e) {
                throw new BusinessException(ErrorEnum.SYSTEM_ERROR, "存在多个默认简历");
            }
            resumeMapper.updateById(resume);
        }
    }


    private void saveResumeInfo(AddResumeDto resume, long id) {
        List<ResumeEducation> educations = new ArrayList<>();
        if (resume.getEducations() != null) {
            for (AddResumeEducationDto education : resume.getEducations()) {
                ResumeEducation e = new ResumeEducation();
                BeanUtils.copyProperties(education, e);
                e.setResumeId(id);
                educations.add(e);
            }
        }
        if (!educations.isEmpty()) {
            educationService.saveBatch(educations);
        }
        educations.clear();
        List<ResumeExperience> experiences = new ArrayList<>();
        if (resume.getExperiences() != null) {
            for (AddResumeExperiencesDto experience : resume.getExperiences()) {
                ResumeExperience e = new ResumeExperience();
                BeanUtils.copyProperties(experience, e);
                e.setResumeId(id);
                experiences.add(e);
            }
        }
        if (!experiences.isEmpty()) {
            experienceService.saveBatch(experiences);
        }
        experiences.clear();
        List<ResumeProject> projects = new ArrayList<>();
        if (resume.getProjects() != null) {
            for (AddResumeProjectDto project : resume.getProjects()) {
                ResumeProject p = new ResumeProject();
                BeanUtils.copyProperties(project, p);
                p.setResumeId(id);
                projects.add(p);
            }
        }
        if (!projects.isEmpty()) {
            projectService.saveBatch(projects);
        }
    }
}