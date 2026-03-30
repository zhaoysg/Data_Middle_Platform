package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 敏感字段定时扫描任务保存 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段定时扫描任务保存参数")
public class SecScanScheduleSaveDTO {

    @ApiModelProperty("任务ID（编辑时传入）")
    private Long id;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("任务编码（唯一）")
    private String taskCode;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("扫描范围：ALL-全部表/SPECIFIC-指定表/LAYER-按数据层")
    private String scanScope;

    @ApiModelProperty("指定表名列表（逗号分隔），scanScope=SPECIFIC 时使用")
    private String tableNames;

    @ApiModelProperty("数据层，scanScope=LAYER 时使用：ODS/DWD/DWS/ADS")
    private String layerCode;

    @ApiModelProperty("是否启用内容级扫描")
    private Boolean enableContentScan;

    @ApiModelProperty("采样数量上限（最大200）")
    private Integer sampleSize;

    @ApiModelProperty("扫描周期：ONCE/DAILY/WEEKLY/MONTHLY")
    private String scanCycle;

    @ApiModelProperty("Cron 表达式（可选）")
    private String cronExpression;

    @ApiModelProperty("状态：ENABLED/DISABLED")
    private String status;

    @ApiModelProperty("创建者")
    private Long createUser;
}
