package com.bagdatahouse.server.controller.dqc;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqDatasourceDTO;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.datasource.vo.ConnectionTestResultVO;
import com.bagdatahouse.dqc.dto.PreviewSelectRequest;
import com.bagdatahouse.dqc.service.DqDatasourceService;
import com.bagdatahouse.dqc.vo.DatasourceTableColumnVO;
import com.bagdatahouse.dqc.vo.SqlPreviewResultVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * DQ数据源控制器
 */
@Api(tags = "数据质量-数据源管理")
@RestController
@RequestMapping("/dqc/datasource")
public class DqDatasourceController {

    private static final Logger log = LoggerFactory.getLogger(DqDatasourceController.class);

    @Autowired
    private DqDatasourceService datasourceService;

    @GetMapping("/page")
    @ApiOperation("分页查询数据源")
    public Result<Page<DqDatasource>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dsName,
            @RequestParam(required = false) String dsType,
            @RequestParam(required = false) String dataLayer,
            @RequestParam(required = false) Integer status
    ) {
        return datasourceService.page(pageNum, pageSize, dsName, dsType, dataLayer, status);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取数据源详情")
    public Result<DqDatasource> getById(
            @ApiParam("数据源ID") @PathVariable Long id
    ) {
        return datasourceService.getById(id);
    }

    @GetMapping("/code/{dsCode}")
    @ApiOperation("根据编码获取数据源")
    public Result<DqDatasource> getByCode(
            @ApiParam("数据源编码") @PathVariable String dsCode
    ) {
        return datasourceService.getByCode(dsCode);
    }

    @PostMapping
    @ApiOperation("新增数据源")
    public Result<Long> create(@RequestBody DqDatasourceDTO dto) {
        return datasourceService.create(dto);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新数据源")
    public Result<Void> update(
            @ApiParam("数据源ID") @PathVariable Long id,
            @RequestBody DqDatasourceDTO dto
    ) {
        return datasourceService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除数据源")
    public Result<Void> delete(
            @ApiParam("数据源ID") @PathVariable Long id
    ) {
        return datasourceService.delete(id);
    }

    @PostMapping("/test")
    @ApiOperation("测试数据源连接（兼容旧接口）")
    public Result<Boolean> testConnection(@RequestBody DqDatasourceDTO dto) {
        return datasourceService.testConnection(dto);
    }

    @PostMapping("/test/detail")
    @ApiOperation("测试数据源连接（返回详细信息）")
    public Result<ConnectionTestResultVO> testConnectionDetail(@RequestBody DqDatasourceDTO dto) {
        return datasourceService.testConnectionDetail(dto);
    }

    @PostMapping("/{id}/test")
    @ApiOperation("测试已有数据源连接（返回详细信息）")
    public Result<ConnectionTestResultVO> testConnectionById(
            @ApiParam("数据源ID") @PathVariable Long id
    ) {
        return datasourceService.testConnectionById(id);
    }

    @PostMapping("/{id}/enable")
    @ApiOperation("启用数据源")
    public Result<Void> enable(
            @ApiParam("数据源ID") @PathVariable Long id
    ) {
        return datasourceService.enable(id);
    }

    @PostMapping("/{id}/disable")
    @ApiOperation("禁用数据源")
    public Result<Void> disable(
            @ApiParam("数据源ID") @PathVariable Long id
    ) {
        return datasourceService.disable(id);
    }

    @GetMapping("/type/{dsType}")
    @ApiOperation("按类型查询数据源")
    public Result<List<DqDatasource>> listByType(
            @ApiParam("数据源类型") @PathVariable String dsType
    ) {
        return datasourceService.listByType(dsType);
    }

    @GetMapping("/layer/{layerCode}")
    @ApiOperation("按数据层查询数据源")
    public Result<List<DqDatasource>> listByLayer(
            @ApiParam("数据层编码") @PathVariable String layerCode
    ) {
        return datasourceService.listByLayer(layerCode);
    }

    @GetMapping("/enabled")
    @ApiOperation("获取已启用的数据源")
    public Result<List<DqDatasource>> listEnabled() {
        return datasourceService.listEnabled();
    }

    @GetMapping("/statistics")
    @ApiOperation("获取数据源统计")
    public Result<Map<String, Object>> getStatistics() {
        return datasourceService.getStatistics();
    }

    @GetMapping("/{id}/tables")
    @ApiOperation("获取数据源下的表列表")
    public Result<List<String>> getTables(
            @ApiParam("数据源ID") @PathVariable Long id,
            @ApiParam(value = "schema 名称（PostgreSQL 专用）") @RequestParam(required = false) String schema
    ) {
        return datasourceService.getTables(id, schema);
    }

    @GetMapping("/{id}/schemas")
    @ApiOperation("获取数据源下的 schema 列表（PostgreSQL 专用）")
    public Result<List<String>> getSchemas(
            @ApiParam("数据源ID") @PathVariable Long id
    ) {
        return datasourceService.getSchemas(id);
    }

    @GetMapping("/{id}/table-columns")
    @ApiOperation("获取数据源指定表的字段列表（直连数据库）")
    public Result<List<DatasourceTableColumnVO>> getTableColumns(
            @ApiParam("数据源ID") @PathVariable Long id,
            @ApiParam(value = "表名，需与库中一致", required = true) @RequestParam String tableName,
            @ApiParam(value = "schema 名称（PostgreSQL 专用）") @RequestParam(required = false) String schema
    ) {
        return datasourceService.getTableColumns(id, tableName, schema);
    }

    @PostMapping("/{id}/preview-select")
    @ApiOperation("只读 SELECT 预览（规则表达式调试，受超时与最大行数限制）")
    public Result<SqlPreviewResultVO> previewSelect(
            @ApiParam("数据源ID") @PathVariable Long id,
            @RequestBody PreviewSelectRequest body
    ) {
        return datasourceService.previewSelect(id, body);
    }
}
