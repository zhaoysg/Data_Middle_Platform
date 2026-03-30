package com.bagdatahouse.server.controller.gov;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.governance.lineage.dto.LineageBatchImportDTO;
import com.bagdatahouse.governance.lineage.service.LineageManualService;
import com.bagdatahouse.governance.lineage.vo.BatchImportResultVO;
import com.bagdatahouse.governance.lineage.vo.ImpactAnalysisResultVO;
import com.bagdatahouse.governance.lineage.vo.TableColumnSuggestVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.List;

/**
 * 手动录入血缘增强接口
 * 提供影响分析、表/字段自动补全、批量导入等增强功能
 */
@Api(tags = "手动录入血缘增强")
@RestController
@RequestMapping("/gov/lineage-manual")
public class LineageManualController {

    @Autowired
    private LineageManualService lineageManualService;

    @ApiOperation("影响分析：从指定表/字段追踪下游影响")
    @GetMapping("/impact/downstream")
    public Result<ImpactAnalysisResultVO> analyzeDownstream(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName,
            @ApiParam("字段名（可空）") @RequestParam(required = false) String column,
            @ApiParam(value = "最大深度", defaultValue = "5") @RequestParam(defaultValue = "5") Integer maxDepth) {
        return lineageManualService.analyzeDownstream(dsId, tableName, column, maxDepth);
    }

    @ApiOperation("回溯分析：从指定表/字段追溯上游来源")
    @GetMapping("/impact/upstream")
    public Result<ImpactAnalysisResultVO> analyzeUpstream(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName,
            @ApiParam("字段名（可空）") @RequestParam(required = false) String column,
            @ApiParam(value = "最大深度", defaultValue = "5") @RequestParam(defaultValue = "5") Integer maxDepth) {
        return lineageManualService.analyzeUpstream(dsId, tableName, column, maxDepth);
    }

    @ApiOperation("从节点详情面板分析（自动判断方向）")
    @GetMapping("/impact/node")
    public Result<ImpactAnalysisResultVO> analyzeFromNode(
            @ApiParam("分析方向：DOWNSTREAM/UPSTREAM") @RequestParam String direction,
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName,
            @ApiParam("字段名（可空）") @RequestParam(required = false) String column,
            @ApiParam(value = "最大深度", defaultValue = "5") @RequestParam(defaultValue = "5") Integer maxDepth) {
        return lineageManualService.analyzeFromNode(direction, dsId, tableName, column, maxDepth);
    }

    @ApiOperation("搜索表/字段建议（关联元数据，自动补全）")
    @GetMapping("/suggest")
    public Result<TableColumnSuggestVO> suggest(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("搜索关键词（表名/字段名/注释）") @RequestParam(required = false) String keyword) {
        return lineageManualService.suggestTablesAndColumns(dsId, keyword);
    }

    @ApiOperation("获取指定数据源的表名列表")
    @GetMapping("/tables")
    public Result<List<TableColumnSuggestVO.TableSuggest>> listTables(
            @ApiParam("数据源ID") @RequestParam Long dsId) {
        return lineageManualService.listTables(dsId);
    }

    @ApiOperation("获取指定表的字段列表")
    @GetMapping("/columns")
    public Result<List<TableColumnSuggestVO.ColumnSuggest>> listColumns(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName) {
        return lineageManualService.listColumns(dsId, tableName);
    }

    @ApiOperation("批量导入血缘（Excel 解析后的结构化数据）")
    @PostMapping("/batch-import")
    public Result<BatchImportResultVO> batchImport(
            @Validated @RequestBody LineageBatchImportDTO dto) {
        return lineageManualService.batchImport(dto);
    }

    @ApiOperation("下载血缘导入模板说明")
    @GetMapping("/import-template")
    public Result<String> getImportTemplate() {
        return lineageManualService.getImportTemplate();
    }
}
