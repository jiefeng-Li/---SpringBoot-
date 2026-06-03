package com.cuit.interviewsystem.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cuit.interviewsystem.annotation.AuthCheck;
import com.cuit.interviewsystem.common.Result;
import com.cuit.interviewsystem.exception.BusinessException;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.model.dto.company.*;
import com.cuit.interviewsystem.model.dto.user.UserPageDto;
import com.cuit.interviewsystem.model.enums.CompanyStatusEnum;
import com.cuit.interviewsystem.model.enums.UserRoleEnum;
import com.cuit.interviewsystem.model.vo.CompanyVo;
import com.cuit.interviewsystem.model.vo.PageVo;
import com.cuit.interviewsystem.model.vo.UserVo;
import com.cuit.interviewsystem.service.CompanyService;
import com.cuit.interviewsystem.service.UserService;
import com.cuit.interviewsystem.utils.JWTUtil;
import com.cuit.interviewsystem.utils.RedisUtil;
import com.cuit.interviewsystem.utils.ThrowUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/company")
@Tag(name = "公司接口", description = "公司接口")
public class CompanyController {
    @Resource
    private CompanyService companyService;
    @Resource
    private UserService userService;
    @Resource
    private JWTUtil jwtUtil;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @GetMapping("/{id}")
    public Result<CompanyVo> getCompanyById(@PathVariable Long id) {
        if (redisUtil.hasKey("companyId=" + id)) {
            return Result.success((CompanyVo) redisUtil.get("companyId=" + id));
        }
        CompanyVo c = companyService.getCompanyVoById(id);
        redisUtil.set("companyId=" + id, c, 1, TimeUnit.DAYS);
        return Result.success(c);
    }

    @GetMapping("/token")
    @AuthCheck
    public Result<CompanyVo> getCompanyByToken() {
        String cmpId = jwtUtil.getLoginUserInfo(JWTUtil.ELEMENT.COMPANY_ID);
        ThrowUtil.throwIfTrue(cmpId == null, ErrorEnum.PARAMS_ERROR, "当前用户未绑定公司");
        if (redisUtil.hasKey("companyId=" + cmpId)) {
            return Result.success((CompanyVo) redisUtil.get("companyId=" + cmpId));
        }
        CompanyVo res = companyService.getCompanyVoById(Long.parseLong(cmpId));
        redisUtil.set("companyId=" + cmpId, res, 1, TimeUnit.DAYS);
        return Result.success(res);
    }

    @GetMapping("/status")
    public Result<?> getCompanyStatusEnum() {
        record StatusRecord(Integer status, String text){}
        List<StatusRecord> res = new ArrayList<>();
        for (CompanyStatusEnum c : CompanyStatusEnum.values())
            res.add(new StatusRecord(c.getStatus(), c.getText()));
        return Result.success(res);
    }

    @GetMapping("/admin/list")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN})
    public Result<PageVo<CompanyVo>> getCompanyListToAdmin(AdminCompanySearchPageDto dto) {
        Page<CompanyVo> res = companyService.adminGetCompanyList(dto);
        return Result.success(PageVo.of(res));
    }


    @GetMapping("/list")
    @AuthCheck
    public Result<PageVo<CompanyVo>> getCompanyList(CompanySearchPageDto dto) {
        // 构建缓存键，包含查询参数和分页信息
        String cacheKey = String.format("company:list:%s", dto.toString());
        PageVo<CompanyVo> cachedResult = (PageVo<CompanyVo>) redisUtil.get(cacheKey);
        if (cachedResult != null) {
            log.info("从Redis缓存中获取公司列表: {}", cacheKey);
            return Result.success(cachedResult);
        }
        Page<CompanyVo> res = companyService.getCompanyList(dto);
        PageVo<CompanyVo> result = PageVo.of(res);
        if (result.getTotal() != 0) {
            redisUtil.set(cacheKey, result, 30, TimeUnit.MINUTES);
            log.info("公司列表已存入Redis缓存: {}", cacheKey);
        }
        return Result.success(result);
    }

    @GetMapping("/user/list")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER, UserRoleEnum.COMP_ADMIN})
    public Result<PageVo<UserVo>> getCompanyUserList(CompanyUserPageDto dto) {
        String companyIdStr = jwtUtil.getLoginUserInfo(JWTUtil.ELEMENT.COMPANY_ID);
        ThrowUtil.throwIfTrue(companyIdStr == null, ErrorEnum.PARAMS_ERROR, "当前用户未绑定公司");

        UserPageDto query = new UserPageDto();
        query.setPageNum(dto.getPageNum());
        query.setPageSize(dto.getPageSize());
        query.setCompanyId(Long.parseLong(companyIdStr));
        query.setRole(dto.getRole() == null ? UserRoleEnum.RECRUITER.getValue() : dto.getRole());
        query.setIsDeleted(0);

        Page<com.cuit.interviewsystem.model.entity.User> users = userService.getUsers(query);
        PageVo<UserVo> result = new PageVo<>();
        result.setPageSize(users.getSize());
        result.setPages(users.getPages());
        result.setPageNum(users.getCurrent());
        result.setTotal(users.getTotal());
        result.setList(users.getRecords().stream().map(user -> {
            UserVo vo = UserVo.objToVo(user);
            vo.setCompanyName(companyService.getCompanyById(user.getCompanyId()).getCompanyName());
            return vo;
        }).toList());
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN})
    public Result<?> deleteCompanyById(@PathVariable Long id) {
        int i = companyService.deleteCompanyById(id);
        if (redisUtil.hasKey("companyId=" + id))
            redisUtil.delete("companyId=" + id);
        clearAllCompanyCache(); // 清除所有公司相关缓存
        return Result.success(null, i <= 0 ? "删除公司不存在或已被删除" : "删除成功");
    }

    @PutMapping("/deregister/{id}")
    @AuthCheck(roles = {UserRoleEnum.COMP_ADMIN})
    public Result<?> deregisterCompanyById(@PathVariable Long id) {
        int i = companyService.deregisterCompanyById(id);
        if (i <= 0)
            return Result.error(ErrorEnum.SYSTEM_ERROR, "注销失败");
        if (redisUtil.hasKey("companyId=" + id))
            redisUtil.delete("companyId=" + id);
        clearAllCompanyCache(); // 清除所有公司相关缓存
        return Result.success(null, "注销成功");
    }

    @PutMapping("/{id}")
    public Result<Long> updateCompanyById(@PathVariable Long id, @Valid @ModelAttribute CompanyInfoDto cid) {
        int i = companyService.updateCompanyById(id, cid);
        if (i <= 0)
            throw new BusinessException(ErrorEnum.NOT_FOUND_ERROR);
        if (redisUtil.hasKey("companyId=" + id))
            redisUtil.delete("companyId=" + id);
        clearAllCompanyCache(); // 清除所有公司相关缓存
        return Result.success(null, "更新成功");
    }

    @PutMapping("/{id}/status")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN})
    public Result<?> updateCompanyStatusBySysAdmin(@PathVariable Long id,
                                                   @Valid @RequestBody CompanyStatusUpdateDto dto) {
        int i = companyService.updateCompanyStatusBySysAdmin(id, dto.getStatus());
        if (i <= 0)
            return Result.error(ErrorEnum.SYSTEM_ERROR, "状态更新失败");
        if (redisUtil.hasKey("companyId=" + id))
            redisUtil.delete("companyId=" + id);
        clearAllCompanyCache();
        return Result.success(null, "状态更新成功");
    }

    /**
     * HttpMediaTypeNotSupportedException 表明Spring无法处理 multipart/form-data 类型的请求。
     * 使用了 @RequestBody 注解：这个注解用于接收JSON/XML等格式，不能用于接收文件上传
     * @param cad
     * @return 放入公司id的token
     */
    @PostMapping
    @AuthCheck(roles = {UserRoleEnum.COMP_ADMIN})
    public Result<String> addOneCompany(@ModelAttribute CompanyAddDto cad) {
        String newToken = companyService.addOneCompany(cad);
        clearAllCompanyCache(); // 清除所有公司相关缓存
        return Result.success(newToken,"添加成功");
    }

    /**
     * 清除所有公司相关的缓存
     */
    private void clearAllCompanyCache() {
        // 清除公司列表缓存
        Set<String> listKeys = redisTemplate.keys("company:list:*");
        if (listKeys != null && !listKeys.isEmpty()) {
            redisTemplate.delete(listKeys);
            log.info("已清除公司列表缓存");
        }

        // 清除单个公司信息缓存
        Set<String> companyKeys = redisTemplate.keys("companyId=*");
        if (companyKeys != null && !companyKeys.isEmpty()) {
            redisTemplate.delete(companyKeys);
            log.info("已清除公司信息缓存");
        }
    }

}
