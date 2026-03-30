package com.bagdatahouse.governance.metadata.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 元数据扫描结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "元数据扫描结果")
public class MetadataScanResult {

    @ApiModelProperty("扫描任务ID")
    private String taskId;

    @ApiModelProperty("扫描状态：RUNNING/SUCCESS/FAILED/PARTIAL_SUCCESS/CANCELLED")
    private String status;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("数据源名称")
    private String dsName;

    @ApiModelProperty("已参与扫描的表数量（成功+失败，不含跳过；进行中时持续增长）")
    private int totalTables;

    @ApiModelProperty("计划扫描表总数（用于进度条）")
    private int plannedTableCount;

    @ApiModelProperty("已处理表数（成功+失败+跳过）")
    private int processedTableCount;

    @ApiModelProperty("成功扫描的表数量")
    private int successTables;

    @ApiModelProperty("扫描失败的表数量")
    private int failedTables;

    @ApiModelProperty("新增的元数据表数量")
    private int insertedTables;

    @ApiModelProperty("更新的元数据表数量")
    private int updatedTables;

    @ApiModelProperty("跳过的表数量（已存在且未覆盖）")
    private int skippedTables;

    @ApiModelProperty("总字段数量")
    private int totalColumns;

    @ApiModelProperty("扫描耗时（毫秒）")
    private long elapsedMs;

    @ApiModelProperty("失败详情")
    private List<TableScanError> errors;

    @ApiModelProperty("扫描的表列表")
    private List<TableScanSummary> tables;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TableScanError {
        @ApiModelProperty("表名")
        private String tableName;
        @ApiModelProperty("错误信息")
        private String errorMessage;
        @ApiModelProperty("异常类型")
        private String exceptionType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TableScanSummary {
        @ApiModelProperty("元数据ID")
        private Long metadataId;
        @ApiModelProperty("表名")
        private String tableName;
        @ApiModelProperty("表类型")
        private String tableType;
        @ApiModelProperty("表注释")
        private String tableComment;
        @ApiModelProperty("字段数量")
        private int columnCount;
        @ApiModelProperty("行数")
        private Long rowCount;
        @ApiModelProperty("是否新增")
        private boolean inserted;
    }
}
