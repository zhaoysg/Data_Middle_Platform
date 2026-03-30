package com.bagdatahouse.server.controller.dqc;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqDataDomain;
import com.bagdatahouse.server.service.dqc.DqDataDomainService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据域控制器
 */
@Api(tags = "数据质量-数据域管理")
@RestController
@RequestMapping("/dqc/data-domain")
public class DqDataDomainController {

    private final DqDataDomainService dataDomainService;

    public DqDataDomainController(DqDataDomainService dataDomainService) {
        this.dataDomainService = dataDomainService;
    }

    @GetMapping("/page")
    @ApiOperation("分页查询数据域")
    public Result<Page<DqDataDomain>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String domainName,
            @RequestParam(required = false) String domainCode,
            @RequestParam(required = false) Integer status
    ) {
        return dataDomainService.page(pageNum, pageSize, domainName, domainCode, status);
    }

    @GetMapping("/list")
    @ApiOperation("获取数据域列表")
    public Result<List<DqDataDomain>> list() {
        return dataDomainService.listEnabled();
    }

    @GetMapping("/tree")
    @ApiOperation("获取数据域树形结构")
    public Result<List<DqDataDomain>> getTree() {
        return dataDomainService.getTree();
    }

    @GetMapping("/{id}")
    @ApiOperation("获取数据域详情")
    public Result<DqDataDomain> getById(
            @ApiParam("数据域ID") @PathVariable Long id
    ) {
        return dataDomainService.getById(id);
    }

    @PostMapping
    @ApiOperation("新增数据域")
    public Result<Long> create(@RequestBody DqDataDomain dataDomain) {
        return dataDomainService.create(dataDomain);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新数据域")
    public Result<Void> update(
            @ApiParam("数据域ID") @PathVariable Long id,
            @RequestBody DqDataDomain dataDomain
    ) {
        return dataDomainService.update(id, dataDomain);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除数据域")
    public Result<Void> delete(
            @ApiParam("数据域ID") @PathVariable Long id
    ) {
        return dataDomainService.delete(id);
    }

    @PutMapping("/{id}/status")
    @ApiOperation("修改数据域状态")
    public Result<Void> updateStatus(
            @ApiParam("数据域ID") @PathVariable Long id,
            @RequestParam Integer status
    ) {
        return dataDomainService.updateStatus(id, status);
    }
}
