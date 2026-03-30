package com.bagdatahouse.server.controller.dprofile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DprofileColumnStats;
import com.bagdatahouse.core.entity.DprofileCompareResult;
import com.bagdatahouse.core.entity.DprofileProfileTask;
import com.bagdatahouse.core.entity.DprofileSnapshot;
import com.bagdatahouse.core.entity.DprofileTableStats;
import com.bagdatahouse.dprofile.dto.SnapshotCompareRequestDTO;
import com.bagdatahouse.dprofile.dto.TableProfileRequestDTO;
import com.bagdatahouse.dprofile.service.DprofileService;
import com.bagdatahouse.dprofile.service.DprofileService.ProfileResult;
import com.bagdatahouse.dprofile.service.DprofileService.SnapshotDetail;
import com.bagdatahouse.dprofile.service.DprofileService.TaskStats;
import com.bagdatahouse.dprofile.vo.ColumnProfileResultVO;
import com.bagdatahouse.dprofile.vo.ProfileExecutionRecordVO;
import com.bagdatahouse.dprofile.vo.SnapshotCompareResultVO;
import com.bagdatahouse.dprofile.vo.TableProfileResultVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据探查控制器
 */
@Api(tags = "数据探查")
@RestController
@RequestMapping("/dprofile")
public class DprofileController {

    @Autowired
    private DprofileService dprofileService;

    // ========== 探查任务 ==========

    @ApiOperation("创建探查任务")
    @PostMapping("/task")
    public Result<Long> createTask(@RequestBody DprofileProfileTask task) {
        return dprofileService.createTask(task);
    }

    @ApiOperation("更新探查任务")
    @PutMapping("/task/{id}")
    public Result<Void> updateTask(@PathVariable Long id, @RequestBody DprofileProfileTask task) {
        return dprofileService.updateTask(id, task);
    }

    @ApiOperation("删除探查任务")
    @DeleteMapping("/task/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        return dprofileService.deleteTask(id);
    }

    @ApiOperation("分页查询探查任务")
    @GetMapping("/task/page")
    public Result<Page<DprofileProfileTask>> pageTasks(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("任务名称") @RequestParam(required = false) String taskName,
            @ApiParam("触发方式") @RequestParam(required = false) String triggerType,
            @ApiParam("探查级别") @RequestParam(required = false) String profileLevel,
            @ApiParam("数据源ID") @RequestParam(required = false) Long targetDsId,
            @ApiParam("状态") @RequestParam(required = false) String status) {
        return dprofileService.pageTasks(pageNum, pageSize, taskName, triggerType, profileLevel, targetDsId, status);
    }

    @ApiOperation("根据ID查询探查任务")
    @GetMapping("/task/{id}")
    public Result<DprofileProfileTask> getTaskById(@PathVariable Long id) {
        return dprofileService.getTaskById(id);
    }

    @ApiOperation("立即执行探查任务")
    @PostMapping("/task/{id}/execute")
    public Result<Long> executeTask(@PathVariable Long id) {
        return dprofileService.executeTask(id);
    }

    @ApiOperation("取消探查任务")
    @PostMapping("/task/{id}/cancel")
    public Result<Void> cancelTask(@PathVariable Long id) {
        return dprofileService.cancelTask(id);
    }

    @ApiOperation("启用/禁用探查任务")
    @PostMapping("/task/{id}/toggle")
    public Result<Void> toggleTask(@PathVariable Long id, @RequestParam Boolean enabled) {
        return dprofileService.toggleTask(id, enabled);
    }

    @ApiOperation("获取探查任务统计")
    @GetMapping("/task/stats")
    public Result<TaskStats> getTaskStats() {
        return dprofileService.getTaskStats();
    }

    // ========== 探查统计 ==========

    @ApiOperation("查询表级统计记录")
    @GetMapping("/table-stats")
    public Result<List<DprofileTableStats>> listTableStats(
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId,
            @ApiParam("表名") @RequestParam(required = false) String tableName,
            @ApiParam("执行ID（可选，用于区分同表多次执行）") @RequestParam(required = false) Long executionId,
            @ApiParam("返回数量") @RequestParam(defaultValue = "10") int limit) {
        return dprofileService.listTableStats(dsId, tableName, executionId, limit);
    }

    @ApiOperation("查询列级统计记录")
    @GetMapping("/column-stats")
    public Result<List<DprofileColumnStats>> listColumnStats(
            @ApiParam("表统计ID") @RequestParam Long tableStatsId) {
        return dprofileService.listColumnStats(tableStatsId);
    }

    @ApiOperation("获取最新探查结果（表+列）")
    @GetMapping("/profile/latest/{metadataId}")
    public Result<ProfileResult> getLatestProfile(@PathVariable Long metadataId) {
        return dprofileService.getLatestProfile(metadataId);
    }

    @ApiOperation("手动探查指定表")
    @PostMapping("/profile/table")
    public Result<Void> profileTable(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName,
            @ApiParam("指定列（逗号分隔）") @RequestParam(required = false) String columns,
            @ApiParam("是否收集列统计") @RequestParam(defaultValue = "true") boolean collectColumnStats) {
        return dprofileService.profileTable(dsId, tableName, columns, collectColumnStats);
    }

    // ========== 表级探查（新）==========

    @ApiOperation("执行表级探查（完整版）")
    @PostMapping("/profile/table/advanced")
    public Result<TableProfileResultVO> profileTableAdvanced(@RequestBody TableProfileRequestDTO request) {
        return dprofileService.profileTableAdvanced(request);
    }

    @ApiOperation("获取探查执行记录")
    @GetMapping("/profile/execution/{executionId}")
    public Result<ProfileExecutionRecordVO> getExecutionRecord(
            @ApiParam("执行ID") @PathVariable Long executionId) {
        return dprofileService.getExecutionRecord(executionId);
    }

    @ApiOperation("查询表历史探查记录")
    @GetMapping("/profile/history")
    public Result<List<DprofileTableStats>> getTableProfileHistory(
            @ApiParam("数据源ID") @RequestParam(required = false) Long dsId,
            @ApiParam("表名") @RequestParam(required = false) String tableName,
            @ApiParam("返回数量") @RequestParam(defaultValue = "30") int limit) {
        return dprofileService.getTableProfileHistory(dsId, tableName, limit);
    }

    @ApiOperation("查询表最新探查结果（不执行新探查）")
    @GetMapping("/profile/last")
    public Result<TableProfileResultVO> getLastProfile(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName) {
        return dprofileService.getLastProfile(dsId, tableName);
    }

    @ApiOperation("获取活跃的探查执行任务")
    @GetMapping("/profile/active")
    public Result<List<ProfileExecutionRecordVO>> getActiveExecutions() {
        return dprofileService.getActiveExecutions();
    }

    // ========== 列级探查 ==========

    @ApiOperation("执行列级探查并获取完整结果（包含分布可视化和异常检测）")
    @GetMapping("/column/profile")
    public Result<List<ColumnProfileResultVO>> profileColumns(
            @ApiParam("表统计记录ID") @RequestParam Long tableStatsId) {
        return dprofileService.profileColumns(tableStatsId);
    }

    @ApiOperation("获取单列的分布可视化数据")
    @GetMapping("/column/distribution")
    public Result<Map<String, Object>> getColumnDistribution(
            @ApiParam("数据源ID") @RequestParam Long dsId,
            @ApiParam("表名") @RequestParam String tableName,
            @ApiParam("列名") @RequestParam String columnName,
            @ApiParam("数据类型") @RequestParam(required = false) String dataType,
            @ApiParam("Top-N 值数量") @RequestParam(defaultValue = "10") int topN) {
        return dprofileService.getColumnDistribution(dsId, tableName, columnName, dataType, topN);
    }

    @ApiOperation("批量获取列的探查结果（带异常警告）")
    @GetMapping("/column/profile/warnings")
    public Result<List<ColumnProfileResultVO>> listColumnProfilesWithWarnings(
            @ApiParam("表统计记录ID") @RequestParam Long tableStatsId) {
        return dprofileService.listColumnProfilesWithWarnings(tableStatsId);
    }

    // ========== 快照管理 ==========

    @ApiOperation("创建探查快照")
    @PostMapping("/snapshot")
    public Result<Long> createSnapshot(@RequestBody DprofileSnapshot snapshot) {
        return dprofileService.createSnapshot(snapshot);
    }

    @ApiOperation("查询快照列表")
    @GetMapping("/snapshot")
    public Result<List<DprofileSnapshot>> listSnapshots(
            @ApiParam("数据源ID") @RequestParam(required = false) Long targetDsId,
            @ApiParam("表名") @RequestParam(required = false) String targetTable) {
        return dprofileService.listSnapshots(targetDsId, targetTable);
    }

    @ApiOperation("删除快照")
    @DeleteMapping("/snapshot/{id}")
    public Result<Void> deleteSnapshot(@PathVariable Long id) {
        return dprofileService.deleteSnapshot(id);
    }

    @ApiOperation("获取快照详情（包含表统计和列统计）")
    @GetMapping("/snapshot/{id}/detail")
    public Result<SnapshotDetail> getSnapshotDetail(@PathVariable Long id) {
        return dprofileService.getSnapshotDetail(id);
    }

    // ========== 快照比对 ==========

    @ApiOperation("执行快照比对")
    @PostMapping("/snapshot/compare")
    public Result<SnapshotCompareResultVO> compareSnapshots(@RequestBody SnapshotCompareRequestDTO request) {
        return dprofileService.compareSnapshots(request);
    }

    @ApiOperation("分页查询比对记录")
    @GetMapping("/snapshot/compare/page")
    public Result<Page<DprofileCompareResult>> pageCompareResults(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("快照A ID") @RequestParam(required = false) Long snapshotAId,
            @ApiParam("快照B ID") @RequestParam(required = false) Long snapshotBId,
            @ApiParam("比对类型") @RequestParam(required = false) String compareType) {
        return dprofileService.pageCompareResults(pageNum, pageSize, snapshotAId, snapshotBId, compareType);
    }

    @ApiOperation("根据ID查询比对结果详情")
    @GetMapping("/snapshot/compare/{compareId}")
    public Result<SnapshotCompareResultVO> getCompareResultById(@PathVariable Long compareId) {
        return dprofileService.getCompareResultById(compareId);
    }
}
