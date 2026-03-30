package com.bagdatahouse.server.controller;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SysUserDTO;
import com.bagdatahouse.core.entity.SysUser;
import com.bagdatahouse.server.service.SysUserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.*;

/**
 * 系统用户控制器
 */
@Api(tags = "系统管理-用户管理")
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/page")
    @ApiOperation("分页查询用户")
    public Result<Page<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId
    ) {
        return sysUserService.page(pageNum, pageSize, username, nickname, status, deptId);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取用户详情")
    public Result<SysUser> getById(
            @ApiParam("用户ID") @PathVariable Long id
    ) {
        return sysUserService.getById(id);
    }

    @PostMapping
    @ApiOperation("新增用户")
    public Result<Long> create(@RequestBody SysUserDTO dto) {
        return sysUserService.create(dto);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新用户")
    public Result<Void> update(
            @ApiParam("用户ID") @PathVariable Long id,
            @RequestBody SysUserDTO dto
    ) {
        return sysUserService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除用户")
    public Result<Void> delete(
            @ApiParam("用户ID") @PathVariable Long id
    ) {
        return sysUserService.delete(id);
    }

    @PutMapping("/{id}/status")
    @ApiOperation("修改用户状态")
    public Result<Void> updateStatus(
            @ApiParam("用户ID") @PathVariable Long id,
            @RequestParam Integer status
    ) {
        return sysUserService.updateStatus(id, status);
    }

    @PutMapping("/{id}/reset-password")
    @ApiOperation("重置用户密码")
    public Result<Void> resetPassword(
            @ApiParam("用户ID") @PathVariable Long id,
            @RequestParam(required = false) String newPassword
    ) {
        return sysUserService.resetPassword(id, newPassword);
    }

    @GetMapping("/options")
    @ApiOperation("获取用户选择器数据")
    public Result<Object> listOptions() {
        return sysUserService.listOptions();
    }
}
