package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 敏感字段扫描参数 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段扫描参数")
public class SecSensitivityScanDTO {

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("PostgreSQL 等库的目标 Schema；直连扫描时优先于数据源连接配置中的默认 Schema")
    private String schema;

    @ApiModelProperty("扫描范围：ALL-全部表/SPECIFIC-指定表/LAYER-按数据层")
    private String scanScope;

    @ApiModelProperty("指定表名列表（逗号分隔或JSON数组），scanScope=SPECIFIC 时使用")
    private String tableNames;

    @ApiModelProperty("数据层，scanScope=LAYER 时使用：ODS/DWD/DWS/ADS")
    private String layerCode;

    @ApiModelProperty("是否扫描列名")
    private Boolean scanColumnName = true;

    @ApiModelProperty("是否扫描列注释")
    private Boolean scanColumnComment = true;

    @ApiModelProperty("是否扫描数据类型")
    private Boolean scanDataType = false;

    @ApiModelProperty("是否使用正则匹配")
    private Boolean useRegex = true;

    @ApiModelProperty("是否排除系统表（sys_ 开头）")
    private Boolean excludeSystemTables = true;

    @ApiModelProperty("采样数量上限（默认50，最大200，对齐阿里）")
    private Integer sampleSize = 50;

    @ApiModelProperty("是否启用内容级扫描（默认开启，数据采样后用内置算法增强置信度）")
    private Boolean enableContentScan = true;

    @ApiModelProperty("增量扫描：仅扫描上次后的变更表")
    private Boolean incremental = false;

    @ApiModelProperty("扫描周期：ONCE-单次/DAILY-每日/WEEKLY-每周/MONTHLY-每月")
    private String scanCycle = "ONCE";

    @ApiModelProperty("直连数据源扫描模式（无需先采集元数据，直接查 INFORMATION_SCHEMA）")
    private Boolean directScan = false;

    @ApiModelProperty("指定扫描的字段名列表（单字段扫描时使用，如一键扫描新字段）")
    private java.util.List<String> columnNames;

    @ApiModelProperty("启用列名匹配扫描（默认 true）")
    private Boolean enableColumnNameMatch = true;

    @ApiModelProperty("启用列注释匹配扫描（默认 true）")
    private Boolean enableColumnCommentMatch = true;

    @ApiModelProperty("扫描模式：ALL-全部/SINGLE_COLUMN-单字段扫描（用于新字段一键扫描）")
    private String scanMode;

    @ApiModelProperty("创建人用户ID")
    private Long createUser;
}
