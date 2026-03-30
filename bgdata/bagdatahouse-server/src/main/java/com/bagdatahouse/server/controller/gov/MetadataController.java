package com.bagdatahouse.server.controller.gov;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovMetadata;
import com.bagdatahouse.core.entity.GovMetadataColumn;
import com.bagdatahouse.governance.metadata.dto.MetadataScanRequest;
import com.bagdatahouse.governance.metadata.dto.MetadataScanResult;
import com.bagdatahouse.governance.metadata.service.MetadataService;
import com.bagdatahouse.governance.metadata.service.MetadataService.MetadataStats;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 元数据管理接口
 */
@Api(tags = "元数据管理")
@RestController
@RequestMapping("/gov/metadata")
public class MetadataController {

    @Autowired
    private MetadataService metadataService;

    @ApiOperation("扫描元数据（同步）")
    @PostMapping("/scan")
    public Result<MetadataScanResult> scan(@RequestBody MetadataScanRequest request) {
        return metadataService.scan(request);
    }

    @ApiOperation("扫描元数据（异步）")
    @PostMapping("/scan/async")
    public Result<String> scanAsync(@RequestBody MetadataScanRequest request) {
        return metadataService.scanAsync(request);
    }

    @ApiOperation("获取异步扫描进度")
    @GetMapping("/scan/progress/{taskId}")
    public Result<MetadataScanResult> getScanProgress(@PathVariable String taskId) {
        return metadataService.getScanProgress(taskId);
    }

    @ApiOperation("取消扫描任务")
    @PostMapping("/scan/cancel/{taskId}")
    public Result<Void> cancelScan(@PathVariable String taskId) {
        return metadataService.cancelScan(taskId);
    }

    @ApiOperation("分页查询元数据")
    @GetMapping("/page")
    public Result<Page<GovMetadata>> page(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId,
            @ApiParam("数据层") @RequestParam(required = false) String dataLayer,
            @ApiParam("数据域") @RequestParam(required = false) String dataDomain,
            @ApiParam("表名") @RequestParam(required = false) String tableName,
            @ApiParam("状态") @RequestParam(required = false) String status) {
        return metadataService.page(pageNum, pageSize, dsId, dataLayer, dataDomain, tableName, status);
    }

    @ApiOperation("根据ID查询元数据")
    @GetMapping("/{id}")
    public Result<GovMetadata> getById(@PathVariable Long id) {
        return metadataService.getById(id);
    }

    @ApiOperation("根据数据源ID和表名查询")
    @GetMapping("/table")
    public Result<GovMetadata> getByDsIdAndTable(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName) {
        return metadataService.getByDsIdAndTable(dsId, tableName);
    }

    @ApiOperation("获取表的字段列表")
    @GetMapping("/{metadataId}/columns")
    public Result<List<GovMetadataColumn>> getColumns(@PathVariable Long metadataId) {
        return metadataService.getColumns(metadataId);
    }

    @ApiOperation("更新元数据")
    @PutMapping("/{id}")
    public Result<Void> updateMetadata(@PathVariable Long id, @RequestBody GovMetadata metadata) {
        return metadataService.updateMetadata(id, metadata);
    }

    @ApiOperation("更新字段信息")
    @PutMapping("/column/{id}")
    public Result<Void> updateColumn(@PathVariable Long id, @RequestBody GovMetadataColumn column) {
        return metadataService.updateColumn(id, column);
    }

    @ApiOperation("删除元数据")
    @DeleteMapping("/{id}")
    public Result<Void> deleteMetadata(@PathVariable Long id) {
        return metadataService.deleteMetadata(id);
    }

    @ApiOperation("批量删除元数据")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return metadataService.batchDelete(ids);
    }

    @ApiOperation("同步统计信息")
    @PostMapping("/sync-stats")
    public Result<Void> syncStats(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名列表") @RequestBody List<String> tableNames) {
        return metadataService.syncStats(dsId, tableNames);
    }

    @ApiOperation("获取元数据统计")
    @GetMapping("/stats")
    public Result<MetadataStats> getStats() {
        return metadataService.getStats();
    }
}
