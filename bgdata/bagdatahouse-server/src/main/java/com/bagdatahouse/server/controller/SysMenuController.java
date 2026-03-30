package com.bagdatahouse.server.controller;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SysMenuDTO;
import com.bagdatahouse.core.entity.SysMenu;
import com.bagdatahouse.core.vo.MenuTreeVO;
import com.bagdatahouse.server.service.SysMenuService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统菜单控制器
 */
@Api(tags = "系统管理-菜单管理")
@RestController
@RequestMapping("/system/menu")
public class SysMenuController {

    private final SysMenuService sysMenuService;

    public SysMenuController(SysMenuService sysMenuService) {
        this.sysMenuService = sysMenuService;
    }

    @GetMapping("/tree")
    @ApiOperation("获取菜单树")
    public Result<List<MenuTreeVO>> getMenuTree() {
        return sysMenuService.getMenuTree();
    }

    @GetMapping("/page")
    @ApiOperation("分页查询菜单")
    public Result<Page<SysMenu>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String menuName,
            @RequestParam(required = false) String menuType,
            @RequestParam(required = false) Integer status
    ) {
        return sysMenuService.page(pageNum, pageSize, menuName, menuType, status);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取菜单详情")
    public Result<SysMenu> getById(
            @ApiParam("菜单ID") @PathVariable Long id
    ) {
        return sysMenuService.getById(id);
    }

    @PostMapping
    @ApiOperation("新增菜单")
    public Result<Long> create(@RequestBody SysMenuDTO dto) {
        return sysMenuService.create(dto);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新菜单")
    public Result<Void> update(
            @ApiParam("菜单ID") @PathVariable Long id,
            @RequestBody SysMenuDTO dto
    ) {
        return sysMenuService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除菜单")
    public Result<Void> delete(
            @ApiParam("菜单ID") @PathVariable Long id
    ) {
        return sysMenuService.delete(id);
    }

    @GetMapping("/options")
    @ApiOperation("获取菜单选择器数据")
    public Result<List<Object>> listOptions() {
        return sysMenuService.listOptions();
    }

    @GetMapping("/role/{roleId}")
    @ApiOperation("根据角色获取菜单")
    public Result<List<SysMenu>> getMenusByRoleId(
            @ApiParam("角色ID") @PathVariable Long roleId
    ) {
        return sysMenuService.getMenusByRoleId(roleId);
    }
}
