package com.bagdatahouse.server.controller;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.SysDept;
import com.bagdatahouse.server.service.SysDeptService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统部门控制器
 */
@Api(tags = "系统管理-部门管理")
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    private final SysDeptService deptService;

    public SysDeptController(SysDeptService deptService) {
        this.deptService = deptService;
    }

    @GetMapping("/tree")
    @ApiOperation("获取部门树")
    public Result<List<SysDept>> getTree() {
        return deptService.getTree();
    }

    @GetMapping("/options")
    @ApiOperation("获取部门选项列表")
    public Result<List<Object>> listOptions() {
        return deptService.listOptions();
    }

    @GetMapping("/page")
    @ApiOperation("分页查询部门")
    public Result<Page<SysDept>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String deptCode,
            @RequestParam(required = false) Integer status
    ) {
        return deptService.page(pageNum, pageSize, deptName, deptCode, status);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取部门详情")
    public Result<SysDept> getById(
            @ApiParam("部门ID") @PathVariable Long id
    ) {
        return deptService.getById(id);
    }

    @PostMapping
    @ApiOperation("新增部门")
    public Result<Long> create(@RequestBody SysDept dept) {
        return deptService.create(dept);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新部门")
    public Result<Void> update(
            @ApiParam("部门ID") @PathVariable Long id,
            @RequestBody SysDept dept
    ) {
        return deptService.update(id, dept);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除部门")
    public Result<Void> delete(
            @ApiParam("部门ID") @PathVariable Long id
    ) {
        return deptService.delete(id);
    }

    @PutMapping("/{id}/status")
    @ApiOperation("修改部门状态")
    public Result<Void> updateStatus(
            @ApiParam("部门ID") @PathVariable Long id,
            @RequestParam Integer status
    ) {
        return deptService.updateStatus(id, status);
    }
}
