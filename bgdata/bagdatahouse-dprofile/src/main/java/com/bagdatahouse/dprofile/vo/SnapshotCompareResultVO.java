package com.bagdatahouse.dprofile.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 快照比对结果 VO
 * 包含完整的表级、列级及分布比对结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "快照比对结果")
public class SnapshotCompareResultVO {

    // ===== 比对元信息 =====
    @ApiModelProperty("比对记录ID")
    private Long compareId;

    @ApiModelProperty("比对名称")
    private String compareName;

    @ApiModelProperty("比对类型: TABLE/COLUMN/FULL")
    private String compareType;

    @ApiModelProperty("比对状态: SUCCESS/FAILED")
    private String status;

    @ApiModelProperty("比对时间")
    private LocalDateTime compareTime;

    @ApiModelProperty("比对耗时(ms)")
    private Long elapsedMs;

    // ===== 快照A信息 =====
    @ApiModelProperty("快照A ID")
    private Long snapshotAId;

    @ApiModelProperty("快照A名称")
    private String snapshotAName;

    @ApiModelProperty("快照A探查时间")
    private LocalDateTime snapshotATime;

    @ApiModelProperty("快照A数据源ID")
    private Long snapshotADsId;

    @ApiModelProperty("快照A表名")
    private String snapshotATable;

    // ===== 快照B信息 =====
    @ApiModelProperty("快照B ID")
    private Long snapshotBId;

    @ApiModelProperty("快照B名称")
    private String snapshotBName;

    @ApiModelProperty("快照B探查时间")
    private LocalDateTime snapshotBTime;

    @ApiModelProperty("快照B数据源ID")
    private Long snapshotBDsId;

    @ApiModelProperty("快照B表名")
    private String snapshotBTable;

    // ===== 时间间隔信息 =====
    @ApiModelProperty("两次探查时间间隔(天)")
    private Integer daysBetween;

    // ===== 差异汇总 =====
    @ApiModelProperty("总体差异级别: NONE/MINOR/MODERATE/MAJOR/CRITICAL")
    private String diffLevel;

    @ApiModelProperty("差异总计数")
    private Integer diffCount;

    @ApiModelProperty("表级差异数")
    private Integer tableDiffCount;

    @ApiModelProperty("列级差异数")
    private Integer columnDiffCount;

    @ApiModelProperty("显著差异列表")
    private List<String> significantChanges;

    // ===== 表级比对结果 =====
    @ApiModelProperty("表级比对结果")
    private TableCompare tableCompare;

    // ===== 列级比对结果 =====
    @ApiModelProperty("列级比对结果列表")
    private List<ColumnCompareVO> columnCompares;

    @ApiModelProperty("新增列列表")
    private List<ColumnCompareVO> newColumns;

    @ApiModelProperty("删除列列表")
    private List<ColumnCompareVO> removedColumns;

    // ===== 表级比对子结果 =====
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TableCompare {
        // --- 行数 ---
        @ApiModelProperty("快照A行数")
        private Long snapshotARowCount;

        @ApiModelProperty("快照B行数")
        private Long snapshotBRowCount;

        @ApiModelProperty("行数变化量")
        private Long rowCountChange;

        @ApiModelProperty("行数变化率(%)")
        private BigDecimal rowCountChangeRate;

        @ApiModelProperty("行数是否显著变化")
        private Boolean rowCountSignificant;

        // --- 列数 ---
        @ApiModelProperty("快照A列数")
        private Integer snapshotAColumnCount;

        @ApiModelProperty("快照B列数")
        private Integer snapshotBColumnCount;

        @ApiModelProperty("列数变化量")
        private Integer columnCountChange;

        @ApiModelProperty("列数变化率(%)")
        private BigDecimal columnCountChangeRate;

        @ApiModelProperty("列数是否显著变化")
        private Boolean columnCountSignificant;

        // --- 存储大小 ---
        @ApiModelProperty("快照A存储大小(字节)")
        private BigDecimal snapshotAStorageBytes;

        @ApiModelProperty("快照B存储大小(字节)")
        private BigDecimal snapshotBStorageBytes;

        @ApiModelProperty("存储大小变化量")
        private BigDecimal storageBytesChange;

        @ApiModelProperty("存储大小变化率(%)")
        private BigDecimal storageBytesChangeRate;

        @ApiModelProperty("存储大小是否显著变化")
        private Boolean storageBytesSignificant;

        // --- 更新时间 ---
        @ApiModelProperty("快照A更新时间")
        private LocalDateTime snapshotAUpdateTime;

        @ApiModelProperty("快照B更新时间")
        private LocalDateTime snapshotBUpdateTime;

        @ApiModelProperty("更新是否变化")
        private Boolean updateTimeChanged;

        // --- 增量行数 ---
        @ApiModelProperty("快照A增量行数")
        private Long snapshotAIncrementRows;

        @ApiModelProperty("快照B增量行数")
        private Long snapshotBIncrementRows;

        @ApiModelProperty("增量行数变化")
        private Long incrementRowsChange;

        @ApiModelProperty("增量是否变化")
        private Boolean incrementRowsChanged;
    }
}
