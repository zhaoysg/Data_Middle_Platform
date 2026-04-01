package org.dromara.metadata.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.metadata.domain.vo.ColumnStats;
import org.dromara.metadata.domain.vo.TableStats;
import org.dromara.metadata.service.IDprofileService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据探查分析控制器
 * <p>
 * 提供表级和列级的实时统计分析接口
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/metadata/dprofile")
public class DprofileController extends BaseController {

    private final IDprofileService dprofileService;

    /**
     * 分析单张表
     *
     * @param dsId      数据源ID
     * @param tableName 表名
     * @param level     探查级别：BASIC/DETAILED/FULL
     * @return 报告ID
     */
    @PostMapping("/analyze")
    public R<Long> analyzeTable(
            @RequestParam Long dsId,
            @RequestParam String tableName,
            @RequestParam(defaultValue = "DETAILED") String level) {
        Long reportId = dprofileService.analyzeTable(dsId, tableName, level);
        return R.ok(reportId);
    }

    /**
     * 批量分析多张表
     *
     * @param dsId        数据源ID
     * @param tableNames  表名列表
     * @param level       探查级别
     * @return 报告ID列表
     */
    @PostMapping("/analyze/batch")
    public R<List<Long>> analyzeTables(
            @RequestParam Long dsId,
            @RequestParam List<String> tableNames,
            @RequestParam(defaultValue = "DETAILED") String level) {
        List<Long> reportIds = dprofileService.analyzeTables(dsId, tableNames, level);
        return R.ok(reportIds);
    }

    /**
     * 获取表级统计
     *
     * @param dsId      数据源ID
     * @param tableName 表名
     * @return 表级统计信息
     */
    @GetMapping("/table/stats")
    public R<TableStats> getTableStats(
            @RequestParam Long dsId,
            @RequestParam String tableName) {
        return R.ok(dprofileService.getTableStats(dsId, tableName));
    }

    /**
     * 获取列级统计
     *
     * @param dsId      数据源ID
     * @param tableName 表名
     * @return 列级统计信息列表
     */
    @GetMapping("/column/stats")
    public R<List<ColumnStats>> getColumnStats(
            @RequestParam Long dsId,
            @RequestParam String tableName) {
        return R.ok(dprofileService.getColumnStats(dsId, tableName));
    }
}
