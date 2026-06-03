package com.cuit.interviewsystem.controller;


import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cuit.interviewsystem.annotation.AuthCheck;
import com.cuit.interviewsystem.common.Result;
import com.cuit.interviewsystem.exception.ErrorEnum;
import com.cuit.interviewsystem.model.dto.user.OwnBindingRequestPageDto;
import com.cuit.interviewsystem.model.dto.company.CommonUserRegister;
import com.cuit.interviewsystem.model.dto.user.*;
import com.cuit.interviewsystem.model.entity.BindingRequest;
import com.cuit.interviewsystem.model.entity.Company;
import com.cuit.interviewsystem.model.entity.User;
import com.cuit.interviewsystem.model.enums.UserAccountStatusEnum;
import com.cuit.interviewsystem.model.enums.UserBindingStatusEnum;
import com.cuit.interviewsystem.model.enums.UserRoleEnum;
import com.cuit.interviewsystem.model.vo.BindingRequestVo;
import com.cuit.interviewsystem.model.vo.PageVo;
import com.cuit.interviewsystem.model.vo.UserVo;
import com.cuit.interviewsystem.service.CompanyService;
import com.cuit.interviewsystem.service.UserBindingRequestService;
import com.cuit.interviewsystem.service.UserService;
import com.cuit.interviewsystem.utils.JWTUtil;
import com.cuit.interviewsystem.utils.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private CompanyService companyService;
    @Resource
    private UserBindingRequestService bindingService;
    @Resource
    private JWTUtil jwtUtil;
    @Resource
    private RedisUtil redisUtil;


    /**
     * 管理员用户注册
     * @param userRegisterDto
     * @return
     */
    @Operation(summary = "管理员用户注册")
    @PostMapping("/admin/register")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN})
    public Result sysAdminRegister(@Valid UserRegisterDto userRegisterDto) {
        long userId = userService.sysAdminRegister(userRegisterDto);
        if (userId <= 0)
            return Result.error(ErrorEnum.SYSTEM_ERROR, "用户注册失败");
        record userRecord(long userId){}
        return Result.success(new userRecord(userId));
    }

    /**
     * 公司用户注册
     * @param userRegisterDto
     * @return
     */
    @Operation(summary = "公司用户注册")
    @PostMapping("/comp/register")
    public Result<Long> compUserRegister(@RequestBody UserRegisterDto userRegisterDto) {
        long userId = userService.compUserRegister(userRegisterDto);
        return Result.success(userId);
    }

    /**
     * 普通用户(求职者)注册
     * @param  cur
     * @return
     */
    @Operation(summary = "普通用户注册")
    @PostMapping("/register")
    public Result<Long> commonUserRegister(@RequestBody CommonUserRegister cur) {
        long userId = userService.commonUserRegister(cur);
        return Result.success(userId);
    }



    /**
     * 通用用户登录
     * @param userLoginDto
     * @return
     */
    @Operation(summary = "用户登录")
    @GetMapping("/login")
    @AuthCheck
    public Result<String> login(UserLoginDto userLoginDto) {
        User user = userService.login(userLoginDto);
        if (ObjUtil.isEmpty(user)) {
            return Result.error(ErrorEnum.PARAMS_ERROR, "账号或密码错误");
        }
        String jwt = jwtUtil.sign(user);
        UserVo userVo = UserVo.objToVo(user);
        if (user.getCompanyId() != null)
            userVo.setCompanyName(companyService.getCompanyById(user.getCompanyId()).getCompanyName());
        redisUtil.set("userId=" + user.getUserId(), userVo, 1, TimeUnit.DAYS);
        return Result.success(jwt);
    }

    @Operation(summary = "获取当前的登录用户")
    @GetMapping("/current")
    public Result<UserVo> getCurrentUser() {
        User cur = jwtUtil.parseLoginUser();
        if (redisUtil.hasKey("userId=" + cur.getUserId()))
            return Result.success((UserVo) redisUtil.get("userId=" + cur.getUserId()));
        User user = userService.getCurrentUser();
        UserVo res = UserVo.objToVo(user);
        res.setCompanyName(companyService.getCompanyById(user.getCompanyId()).getCompanyName());
        redisUtil.set("userId=" + cur.getUserId(), res);
        return Result.success(res);
    }


    /**
     *
     * @return 用户角色类型
     */
    @Operation(summary = "获取用户角色类型")
    @GetMapping("/roles")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN})
    public Result getUserRoles() {
        record roles(String text, String value){}
        List<roles> res = new ArrayList<>();
        for (UserRoleEnum e : UserRoleEnum.values())
            res.add(new roles(e.getText(), e.getValue()));
        return Result.success(res);
    }

    @Operation(summary = "获取用户状态类型")
    @GetMapping("/status")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN, UserRoleEnum.COMP_ADMIN})
    public Result getUserStatus() {
        record statusRecord(int status, String text){}
        List<statusRecord> res = new ArrayList<>();
        for (UserAccountStatusEnum e : UserAccountStatusEnum.values())
            res.add(new statusRecord(e.getStatus(), e.getText()));
        return Result.success(res);
    }



    /**
     * 通用 获取单个用户
     * @param conditions
     * @return
     */
    @Operation(summary = "根据组合条件获取一个用户")
    @GetMapping
    public Result<UserVo> getOneUser(User conditions) {
        if (conditions.getUserId() != null && redisUtil.hasKey("userId=" + conditions.getUserId())) {
            return Result.success((UserVo) redisUtil.get("userId=" + conditions.getUserId()));
        }
        User res = userService.getOneUser(conditions);
        UserVo resVo = UserVo.objToVo(res);
        if (resVo != null && resVo.getCompanyId() != null) {
            resVo.setCompanyName(companyService.getCompanyById(res.getCompanyId()).getCompanyName());
        }
        redisUtil.set("userId=" + res.getUserId(), resVo, 1, TimeUnit.DAYS);
        return Result.success(resVo);
    }

    @Operation(summary = "根据id获取一个用户")
    @GetMapping("/list")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN, UserRoleEnum.COMP_ADMIN})
    public Result<PageVo<UserVo>> getUserPage(UserPageDto conditions) {
        Page<User> users = userService.getUsers(conditions);
        List<User> records = users.getRecords();
        List<UserVo> res = new ArrayList<>();
        for (User u : records) {
            UserVo userVo = UserVo.objToVo(u);
            if (u.getCompanyId() != null)
                userVo.setCompanyName(companyService.getCompanyById(u.getCompanyId()).getCompanyName());
            res.add(userVo);
        }
        PageVo<UserVo> pageVo = new PageVo<>();
        pageVo.setPageSize(users.getSize());
        pageVo.setPages(users.getPages());
        pageVo.setPageNum(users.getCurrent());
        pageVo.setTotal(users.getTotal());
        pageVo.setList(res);
        return Result.success(pageVo);
    }


    /**
     * 批量添加用户
     * @param usersAddDto
     * @return
     */
    @Operation(summary = "批量添加用户 # 待重构")
    @PostMapping("/list")
    public Result addUsers(UsersAddDto usersAddDto) {
        //TODO 不太符合实际应用场景，待修改为使用excel添加
        userService.usersAdd(usersAddDto);
        return Result.success(null, "添加成功");
    }



    /**
     * 管理员添加单个用户
     * @param user
     * @return
     */
    @Operation(summary = "添加一个用户")
    @PostMapping("")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN})
    public Result<Long> addOneUser(User user) {
        long id = userService.addOneUser(user);
        if (id <= 0)
            return Result.error(ErrorEnum.SYSTEM_ERROR, "添加失败");
        return Result.success(null, "添加成功");
    }

    /**
     * 通用用户注销账号
     * @param id
     * @return
     */
    @Operation(summary = "根据id(userId)删除一个用户")
    @DeleteMapping("/{id}")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN, UserRoleEnum.COMP_ADMIN,
            UserRoleEnum.RECRUITER, UserRoleEnum.JOB_SEEKER})
    public Result deleteOneUser(@PathVariable Long id) {
        int i = userService.deleteOneUserById(id);
        if (i == 0)
            return Result.error(ErrorEnum.NOT_FOUND_ERROR.getCode(), "删除失败，用户不存在");
        redisUtil.delete("userId=" + id);
        return Result.success(i, "删除成功");
    }



    @Operation(summary = "根据id(userId)更新一个用户")
    @PutMapping({"/{id}"})
    @AuthCheck()
    public Result<UserVo> updateOneUser(@PathVariable Long id, @Valid @ModelAttribute UserUpdateDto user) {
        User res = userService.updateOneUser(id, user);
        UserVo resVo = UserVo.objToVo(res);
        if (resVo == null)
            return Result.error(ErrorEnum.NOT_FOUND_ERROR.getCode(), "更新失败，用户不存在");
        if (res.getCompanyId() != null)
            resVo.setCompanyName(companyService.getCompanyById(res.getCompanyId()).getCompanyName());
        redisUtil.set("userId=" + resVo.getUserId(), resVo);
        return Result.success(resVo);
    }

    @Operation(summary = "系统管理员修改用户账号状态")
    @PutMapping("/{id}/status")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN})
    public Result<?> updateUserStatusByAdmin(@PathVariable Long id,
                                             @Valid @RequestBody UserStatusUpdateDto dto) {
        int i = userService.updateUserStatusByAdmin(id, dto.getAccountStatus());
        if (i <= 0)
            return Result.error(ErrorEnum.SYSTEM_ERROR, "更新失败");
        redisUtil.delete("userId=" + id);
        return Result.success(null, "更新成功");
    }

    @Operation(summary = "用户自行注销账号")
    @PutMapping("/self/deregister")
    @AuthCheck(roles = {UserRoleEnum.SYS_ADMIN, UserRoleEnum.COMP_ADMIN, UserRoleEnum.RECRUITER, UserRoleEnum.JOB_SEEKER})
    public Result<?> deregisterSelf() {
        int i = userService.deregisterSelf();
        if (i <= 0)
            return Result.error(ErrorEnum.SYSTEM_ERROR, "注销失败");
        Long userId = Long.parseLong(jwtUtil.getLoginUserInfo(JWTUtil.ELEMENT.USER_ID));
        redisUtil.delete("userId=" + userId);
        return Result.success(null, "注销成功");
    }

    //region  招聘者绑定公司
    @Operation(summary = "获取绑定状态类型")
    @GetMapping("/binding/status")
    public Result<?> getBindingStatus() {
        record bindingStatusRecord(Integer status, String text) {}
        List<bindingStatusRecord> res = new ArrayList<>();
        for (UserBindingStatusEnum e : UserBindingStatusEnum.values()) {
            res.add(new bindingStatusRecord(e.getStatus(), e.getText()));
        }
        return Result.success(res);
    }

    @Operation(summary = "获取绑定信息")
    @GetMapping("/binding")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER})
    public Result<BindingRequestVo> getBindingInfo(Long id) {
        BindingRequest request = bindingService.getRequestById(id);
        User requestUser = userService.getById(request.getUserId());
        User reviewAdmin = userService.getById(request.getReviewedBy());
        Company cmp = companyService.getCompanyById(request.getCompanyId());
        BindingRequestVo res = new BindingRequestVo();
        BeanUtils.copyProperties(request, res);
        res.setUsername(requestUser.getUsername());
        res.setUserPhone(DesensitizedUtil.mobilePhone(requestUser.getPhone()));
        res.setReviewedByName(reviewAdmin.getUsername());
        res.setCompanyName(cmp.getCompanyName());
        return Result.success(res);
    }

    @Operation(summary = "招聘者绑定公司")
    @PostMapping("/binding")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER, UserRoleEnum.COMP_ADMIN})
    public Result<?> addBindingCompany(@Valid @RequestBody BindingRequestDto bindingRequestDto) {
        bindingService.bindingCompany(bindingRequestDto);
        return Result.success(null, "提交成功");
    }

    @Operation(summary = "获取用户id的绑定记录")
    @GetMapping("/binding/own/list")
    @AuthCheck
    public Result<PageVo<BindingRequestVo>> getBindingRequest(@Valid OwnBindingRequestPageDto dto) {
        Page<BindingRequestVo> page = bindingService.getBindingRequestByUserId(dto);
        PageVo<BindingRequestVo> res = PageVo.of(page);
        res.getList().forEach(vo ->
                vo.setUserPhone(DesensitizedUtil.mobilePhone(vo.getUserPhone()))
        );
        return Result.success(res);
    }

    @Operation(summary = "公司管理员审核绑定请求")
    @PutMapping("/binding/{id}")
    @AuthCheck(roles = {UserRoleEnum.COMP_ADMIN})
    public Result<?> reviewBindingRequest(@PathVariable Long id, @Valid @RequestBody ReviewBindingRequestDto dto) {
        int i = bindingService.reviewBindingRequest(id, dto);
        return i == 0 ? Result.error(ErrorEnum.SYSTEM_ERROR, "审核失败") : Result.success(null, "审核成功");
    }

    @Operation(summary = "招聘者取消绑定请求")
    @DeleteMapping("/binding/cancel/{id}")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER, UserRoleEnum.COMP_ADMIN})
    public Result<?> cancelBindingCompany(@PathVariable Long id) {
        int i = bindingService.cancelBinding(id);
        return i == 0 ? Result.error(ErrorEnum.SYSTEM_ERROR, "取消失败") : Result.success(null, "取消成功");
    }

    @Operation(summary = "公司管理员解绑公司")
    @PutMapping("/unbind/{id}")
    @AuthCheck(roles = {UserRoleEnum.RECRUITER, UserRoleEnum.COMP_ADMIN})
    public Result<?> unbindCompany(@PathVariable Long id) {
        String newToken = bindingService.unbindCompany(id);
        return newToken == null ? Result.error(ErrorEnum.SYSTEM_ERROR, "解绑失败") : Result.success(newToken, "解绑成功");
    }

    @Operation(summary = "公司管理员获取绑定请求列表")
    @GetMapping("/binding/list")
    @AuthCheck(roles = {UserRoleEnum.COMP_ADMIN})
    public Result<PageVo<?>> getBindingList(@Valid BindingRequestPageDto pageDto) {
        Page<BindingRequestVo> page = bindingService.getBindingList(pageDto);
        PageVo<BindingRequestVo> res = PageVo.of(page);
        //手机号脱敏
        res.getList().forEach(vo ->
            vo.setUserPhone(DesensitizedUtil.mobilePhone(vo.getUserPhone()))
        );
        return Result.success(res);
    }
    //endregion
}
