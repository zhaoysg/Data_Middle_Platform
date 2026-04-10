package org.dromara.metadata.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 敏感字段扫描参数 DTO
 * 对齐 governance/SecSensitivityScanDTO，支撑 ruoyi-admin 单体版的完整扫描能力
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "敏感字段扫描参数")
public class SecSensitivityScanDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "数据源ID")
    private Long dsId;

    @Schema(description = "PostgreSQL 等库的目标 Schema；直连扫描时优先于数据源连接配置中的默认 Schema")
    private String schema;

    @Schema(description = "扫描范围：ALL-全部表/SPECIFIC-指定表/LAYER-按数据层")
    private String scanScope;

    @Schema(description = "指定表名列表（逗号分隔或JSON数组），scanScope=SPECIFIC 时使用")
    private String tableNames;

    @Schema(description = "数据层，scanScope=LAYER 时使用：ODS/DWD/DWS/ADS")
    private String layerCode;

    @Schema(description = "是否扫描列名")
    private Boolean scanColumnName = true;

    @Schema(description = "是否扫描列注释")
    private Boolean scanColumnComment = true;

    @Schema(description = "是否扫描数据类型")
    private Boolean scanDataType = false;

    @Schema(description = "是否使用正则匹配")
    private Boolean useRegex = true;

    @Schema(description = "是否排除系统表（sys_ 开头）")
    private Boolean excludeSystemTables = true;

    @Schema(description = "采样数量上限（默认50，最大200）")
    private Integer sampleSize = 50;

    @Schema(description = "是否启用内容级扫描（默认开启，数据采样后用内置算法增强置信度）")
    private Boolean enableContentScan = true;

    @Schema(description = "增量扫描：仅扫描上次后的变更表")
    private Boolean incremental = false;

    @Schema(description = "扫描周期：ONCE-单次/DAILY-每日/WEEKLY-每周/MONTHLY-每月")
    private String scanCycle = "ONCE";

    @Schema(description = "直连数据源扫描模式（无需先采集元数据，直接查 INFORMATION_SCHEMA）")
    private Boolean directScan = false;

    @Schema(description = "指定扫描的字段名列表（单字段扫描时使用，如一键扫描新字段）")
    private List<String> columnNames;

    @Schema(description = "启用列名匹配扫描（默认 true）")
    private Boolean enableColumnNameMatch = true;

    @Schema(description = "启用列注释匹配扫描（默认 true）")
    private Boolean enableColumnCommentMatch = true;

    @Schema(description = "扫描模式：ALL-全部/SINGLE_COLUMN-单字段扫描（用于新字段一键扫描）")
    private String scanMode;
}
