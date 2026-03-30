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
 * 表级探查结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "表级探查结果")
public class TableProfileResultVO {

    @ApiModelProperty("执行ID")
    private Long executionId;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("数据源类型")
    private String dsType;

    @ApiModelProperty("数据源名称")
    private String dsName;

    @ApiModelProperty("目标表名")
    private String tableName;

    @ApiModelProperty("探查时间")
    private LocalDateTime profileTime;

    @ApiModelProperty("探查耗时(ms)")
    private Long elapsedMs;

    // ===== 基本统计 =====
    @ApiModelProperty("总行数")
    private Long rowCount;

    @ApiModelProperty("列数")
    private Integer columnCount;

    @ApiModelProperty("存储大小(字节)")
    private BigDecimal storageBytes;

    @ApiModelProperty("存储大小(格式化)")
    private String storageSizeFormatted;

    @ApiModelProperty("表注释")
    private String tableComment;

    // ===== 更新相关 =====
    @ApiModelProperty("数据最后更新时间")
    private LocalDateTime lastUpdateTime;

    @ApiModelProperty("距上次探查增量行数")
    private Long incrementRows;

    @ApiModelProperty("增量百分比")
    private BigDecimal incrementRate;

    @ApiModelProperty("数据新鲜度(分钟)")
    private Long freshnessMinutes;

    // ===== 字段信息 =====
    @ApiModelProperty("字段列表")
    private List<ColumnInfoVO> columns;

    // ===== 健康状态 =====
    @ApiModelProperty("健康状态: HEALTHY/WARNING/CRITICAL/UNKNOWN")
    private String healthStatus;

    @ApiModelProperty("健康提示")
    private List<String> healthHints;

    @ApiModelProperty("探查任务ID")
    private Long taskId;

    @ApiModelProperty("任务名称")
    private String taskName;

    // ===== 内部存储字段 =====
    @ApiModelProperty("存储到DB的统计记录ID")
    private Long tableStatsId;

    /**
     * 字段信息 VO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnInfoVO {
        @ApiModelProperty("字段名")
        private String columnName;

        @ApiModelProperty("数据类型")
        private String dataType;

        @ApiModelProperty("字段注释")
        private String columnComment;

        @ApiModelProperty("是否可空")
        private Boolean nullable;

        @ApiModelProperty("是否主键")
        private Boolean primaryKey;

        @ApiModelProperty("默认值")
        private String defaultValue;

        @ApiModelProperty("字段长度")
        private Integer columnSize;
    }
}
