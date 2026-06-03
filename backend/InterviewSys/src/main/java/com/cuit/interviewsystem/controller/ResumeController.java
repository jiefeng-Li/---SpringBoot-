package com.cuit.interviewsystem.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cuit.interviewsystem.annotation.AuthCheck;
import com.cuit.interviewsystem.common.Result;
import com.cuit.interviewsystem.model.dto.resume.AddResumeDto;
import com.cuit.interviewsystem.model.dto.resume.PreviewResumeDto;
import com.cuit.interviewsystem.model.dto.resume.ResumePageDto;
import com.cuit.interviewsystem.model.vo.PageVo;
import com.cuit.interviewsystem.model.vo.ResumeTemplateVo;
import com.cuit.interviewsystem.model.vo.ResumeVo;
import com.cuit.interviewsystem.service.ResumeService;
import com.cuit.interviewsystem.service.ResumeTemplateService;
import com.cuit.interviewsystem.utils.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/resume")
@Tag(name = "简历管理")
public class ResumeController {
    @Resource
    private ResumeService resumeService;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private ResumeTemplateService resumeTemplateService;

    @GetMapping("/list")
    @AuthCheck
    public Result<PageVo<ResumeVo>> getResumesPageByUserId(@Valid ResumePageDto resumePageDto) {
        Page<ResumeVo> page = resumeService.getResumesByUserId(resumePageDto);
        PageVo<ResumeVo> res = PageVo.of(page);
        return Result.success(res);
    }

    /**
     * 获取所有简历模板
     * @return 简历模板列表
     */
    @GetMapping("/templates")
    @AuthCheck
    @Operation(summary = "获取所有简历模板")
    public Result<List<ResumeTemplateVo>> getAllTemplates() {
        List<ResumeTemplateVo> templates = resumeTemplateService.getAllTemplates();
        return Result.success(templates);
    }

    @GetMapping("/{id}")
    @AuthCheck
    public Result<ResumeVo> getResumeById(@PathVariable Long id) {
        if (redisUtil.hasKey("resumeId=" + id)) {
            return Result.success((ResumeVo) redisUtil.get("resumeId=" + id));
        }
        ResumeVo res = resumeService.getResumeById(id);
        redisUtil.set("resumeId=" + id, res, 5, TimeUnit.MINUTES);
        return Result.success(res);
    }

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuthCheck
    public Result<Long> addResume(@Valid @RequestPart("resume") AddResumeDto resume,
                                     @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        Long id = resumeService.addResume(resume, avatar);
        return Result.success(id, "添加成功");
    }

    @DeleteMapping("/{id}")
    @AuthCheck
    public Result<Integer> deleteResume(@PathVariable Long id) {
        redisUtil.delete("resumeId=" + id);
        resumeService.deleteResume(id);
        return Result.success(null, "删除成功");
    }

    @PutMapping("/{id}")
    @AuthCheck
    public Result<Integer> updateResume(@Valid @RequestPart("resume") AddResumeDto resume,
                                        @PathVariable Long id,
                                        @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        redisUtil.delete("resumeId=" + id);
        resumeService.updateResume(resume, id, avatar);
        return Result.success(null, "更新成功");
    }

    @PutMapping("/default/{id}")
    @AuthCheck
    public Result<?> setDefaultResume(@PathVariable Long id) {
        resumeService.setDefault(id);
        redisUtil.delete("resumeId=" + id);
        return Result.success(null, "设置成功");
    }

    //TODO: 简历上传
    //TODO: 简历模板

    /**
     * 预览简历
     * @param id 简历ID
     * @param templateId 模板ID
     * @return HTML内容
     */
    @GetMapping("/preview/{id}")
    @AuthCheck
    @Operation(summary = "预览简历")
    public Result<String> previewResume(@PathVariable Long id,
                                @RequestParam(required = false, defaultValue = "1") Integer templateId) {
        String html = resumeTemplateService.previewResume(id, templateId);
        return Result.success(html);
    }

    /**
     * 预览未保存的简历
     * @param previewResumeDto 简历数据
     * @param templateId 模板ID
     * @return HTML内容
     */
    @PostMapping("/preview")
    @AuthCheck
    @Operation(summary = "预览未保存的简历")
    public Result<String> previewUnsavedResume(@RequestBody PreviewResumeDto previewResumeDto,
                                       @RequestParam(required = false, defaultValue = "1") Integer templateId) {
        String html = resumeTemplateService.previewUnsavedResume(previewResumeDto, templateId);
        return Result.success(html);
    }

    /**
     * 导出简历为PDF
     * @param id 简历ID
     * @param templateId 模板ID
     * @param response HTTP响应对象
     */
    @GetMapping("/export/{id}")
    @AuthCheck
    @Operation(summary = "导出简历PDF")
    public void exportResumeToPdf(@PathVariable Long id,
                                   @RequestParam(required = false, defaultValue = "1") Integer templateId,
                                   HttpServletResponse response) {
        resumeTemplateService.exportResumeToPdf(id, templateId, response);
    }
}
