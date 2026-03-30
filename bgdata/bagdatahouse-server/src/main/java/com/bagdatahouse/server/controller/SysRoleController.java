package com.bagdatahouse.server.controller;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SysRoleDTO;
import com.bagdatahouse.core.entity.SysRole;
import com.bagdatahouse.server.service.SysRoleService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统角色控制器
 */
@Api(tags = "系统管理-角色管理")
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    @GetMapping("/page")
    @ApiOperation("分页查询角色")
    public Result<Page<SysRole>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer status
    ) {
        return sysRoleService.page(pageNum, pageSize, roleName, roleCode, status);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取角色详情")
    public Result<SysRole> getById(
            @ApiParam("角色ID") @PathVariable Long id
    ) {
        return sysRoleService.getById(id);
    }

    @PostMapping
    @ApiOperation("新增角色")
    public Result<Long> create(@RequestBody SysRoleDTO dto) {
        return sysRoleService.create(dto);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新角色")
    public Result<Void> update(
            @ApiParam("角色ID") @PathVariable Long id,
            @RequestBody SysRoleDTO dto
    ) {
        return sysRoleService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除角色")
    public Result<Void> delete(
            @ApiParam("角色ID") @PathVariable Long id
    ) {
        return sysRoleService.delete(id);
    }

    @PutMapping("/{id}/status")
    @ApiOperation("修改角色状态")
    public Result<Void> updateStatus(
            @ApiParam("角色ID") @PathVariable Long id,
            @RequestParam Integer status
    ) {
        return sysRoleService.updateStatus(id, status);
    }

    @GetMapping("/options")
    @ApiOperation("获取角色选择器数据")
    public Result<List<Object>> listOptions() {
        return sysRoleService.listOptions();
    }

    @GetMapping("/{id}/menus")
    @ApiOperation("获取角色菜单权限")
    public Result<List<Long>> getRoleMenus(
            @ApiParam("角色ID") @PathVariable Long id
    ) {
        return sysRoleService.getRoleMenus(id);
    }

    @PutMapping("/{id}/menus")
    @ApiOperation("分配菜单权限")
    public Result<Void> assignMenus(
            @ApiParam("角色ID") @PathVariable Long id,
            @RequestBody List<Long> menuIds
    ) {
        return sysRoleService.assignMenus(id, menuIds);
    }
}
