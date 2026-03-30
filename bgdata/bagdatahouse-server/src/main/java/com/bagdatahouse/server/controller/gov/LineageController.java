package com.bagdatahouse.server.controller.gov;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovLineage;
import com.bagdatahouse.governance.lineage.dto.LineageQueryDTO;
import com.bagdatahouse.governance.lineage.dto.LineageSaveDTO;
import com.bagdatahouse.governance.lineage.service.LineageService;
import com.bagdatahouse.governance.lineage.vo.LineageGraphVO;
import com.bagdatahouse.governance.lineage.vo.LineageNodeVO;
import com.bagdatahouse.governance.lineage.vo.LineageStatsVO;
import com.bagdatahouse.governance.lineage.vo.LineageVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据血缘管理接口
 */
@Api(tags = "数据血缘管理")
@RestController
@RequestMapping("/gov/lineage")
public class LineageController {

    @Autowired
    private LineageService lineageService;

    @ApiOperation("新增血缘记录")
    @PostMapping
    public Result<GovLineage> save(@Validated @RequestBody LineageSaveDTO dto) {
        return lineageService.save(dto);
    }

    @ApiOperation("批量新增血缘记录")
    @PostMapping("/batch")
    public Result<Void> batchSave(@RequestBody List<LineageSaveDTO> dtoList) {
        return lineageService.batchSave(dtoList);
    }

    @ApiOperation("更新血缘记录")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody LineageSaveDTO dto) {
        return lineageService.update(id, dto);
    }

    @ApiOperation("删除血缘记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return lineageService.delete(id);
    }

    @ApiOperation("批量删除血缘记录")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        return lineageService.batchDelete(ids);
    }

    @ApiOperation("根据ID查询血缘记录")
    @GetMapping("/{id}")
    public Result<GovLineage> getById(@PathVariable Long id) {
        return lineageService.getById(id);
    }

    @ApiOperation("分页查询血缘记录")
    @GetMapping("/page")
    public Result<Page<GovLineage>> page(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("血缘类型：TABLE/COLUMN") @RequestParam(required = false) String lineageType,
            @ApiParam("源数据源ID") @RequestParam(required = false) Long sourceDsId,
            @ApiParam("源表名（模糊匹配）") @RequestParam(required = false) String sourceTable,
            @ApiParam("源字段名（模糊匹配）") @RequestParam(required = false) String sourceColumn,
            @ApiParam("目标数据源ID") @RequestParam(required = false) Long targetDsId,
            @ApiParam("目标表名（模糊匹配）") @RequestParam(required = false) String targetTable,
            @ApiParam("目标字段名（模糊匹配）") @RequestParam(required = false) String targetColumn,
            @ApiParam("转换类型") @RequestParam(required = false) String transformType,
            @ApiParam("血缘来源：MANUAL/AUTO_PARSER") @RequestParam(required = false) String lineageSource,
            @ApiParam("状态") @RequestParam(required = false) String status,
            @ApiParam("部门ID") @RequestParam(required = false) Long deptId) {

        LineageQueryDTO queryDTO = LineageQueryDTO.builder()
                .lineageType(lineageType)
                .sourceDsId(sourceDsId)
                .sourceTable(sourceTable)
                .sourceColumn(sourceColumn)
                .targetDsId(targetDsId)
                .targetTable(targetTable)
                .targetColumn(targetColumn)
                .transformType(transformType)
                .lineageSource(lineageSource)
                .status(status)
                .deptId(deptId)
                .build();

        return lineageService.page(pageNum, pageSize, queryDTO);
    }

    @ApiOperation("查询血缘详情（含关联信息）")
    @GetMapping("/detail/{id}")
    public Result<LineageVO> getDetail(@PathVariable Long id) {
        return lineageService.getDetail(id);
    }

    @ApiOperation("根据源表和目标表查询血缘")
    @GetMapping("/source-target")
    public Result<List<GovLineage>> getBySourceAndTarget(
            @ApiParam("源数据源ID") @RequestParam Long sourceDsId,
            @ApiParam("源表名") @RequestParam String sourceTable,
            @ApiParam("目标数据源ID") @RequestParam Long targetDsId,
            @ApiParam("目标表名") @RequestParam String targetTable) {
        return lineageService.getBySourceAndTarget(sourceDsId, sourceTable, targetDsId, targetTable);
    }

    @ApiOperation("获取血缘图谱数据")
    @GetMapping("/graph")
    public Result<LineageGraphVO> getGraph(
            @ApiParam("血缘类型：TABLE/COLUMN") @RequestParam(required = false) String lineageType,
            @ApiParam("源数据源ID") @RequestParam(required = false) Long sourceDsId,
            @ApiParam("源表名") @RequestParam(required = false) String sourceTable,
            @ApiParam("源字段名") @RequestParam(required = false) String sourceColumn,
            @ApiParam("目标数据源ID") @RequestParam(required = false) Long targetDsId,
            @ApiParam("目标表名") @RequestParam(required = false) String targetTable,
            @ApiParam("目标字段名") @RequestParam(required = false) String targetColumn,
            @ApiParam("转换类型") @RequestParam(required = false) String transformType,
            @ApiParam("血缘来源：MANUAL/AUTO_PARSER") @RequestParam(required = false) String lineageSource,
            @ApiParam("状态") @RequestParam(required = false) String status,
            @ApiParam("部门ID") @RequestParam(required = false) Long deptId) {

        LineageQueryDTO queryDTO = LineageQueryDTO.builder()
                .lineageType(lineageType)
                .sourceDsId(sourceDsId)
                .sourceTable(sourceTable)
                .sourceColumn(sourceColumn)
                .targetDsId(targetDsId)
                .targetTable(targetTable)
                .targetColumn(targetColumn)
                .transformType(transformType)
                .lineageSource(lineageSource)
                .status(status)
                .deptId(deptId)
                .build();

        return lineageService.getGraph(queryDTO);
    }

    @ApiOperation("获取下游血缘（DAG向下追溯）")
    @GetMapping("/downstream")
    public Result<List<LineageNodeVO>> getDownstream(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName,
            @ApiParam(value = "最大深度", defaultValue = "3") @RequestParam(defaultValue = "3") Integer maxDepth) {
        return lineageService.getDownstream(dsId, tableName, maxDepth);
    }

    @ApiOperation("获取上游血缘（DAG向上回溯）")
    @GetMapping("/upstream")
    public Result<List<LineageNodeVO>> getUpstream(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName,
            @ApiParam(value = "最大深度", defaultValue = "3") @RequestParam(defaultValue = "3") Integer maxDepth) {
        return lineageService.getUpstream(dsId, tableName, maxDepth);
    }

    @ApiOperation("获取血缘统计信息")
    @GetMapping("/stats")
    public Result<LineageStatsVO> getStats() {
        return lineageService.getStats();
    }
}
