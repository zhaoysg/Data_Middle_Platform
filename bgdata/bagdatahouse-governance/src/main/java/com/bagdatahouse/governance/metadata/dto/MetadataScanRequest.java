package com.bagdatahouse.governance.metadata.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 元数据扫描请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "元数据扫描请求")
public class MetadataScanRequest {

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("扫描范围：ALL-全部表，SPECIFIED-指定表，PATTERN-按模式匹配")
    private String scanScope;

    @ApiModelProperty("指定表名列表（scanScope=SPECIFIED时必填）")
    private List<String> tableNames;

    @ApiModelProperty("表名匹配模式（scanScope=PATTERN时使用，支持 % 和 _ 通配符）")
    private String tablePattern;

    @ApiModelProperty("数据层编码（ODS/DWD/DWS/ADS）")
    private String dataLayer;

    @ApiModelProperty("数据域")
    private String dataDomain;

    @ApiModelProperty("业务域")
    private String bizDomain;

    @ApiModelProperty("是否收集表统计信息（行数、存储大小等）")
    private Boolean collectStats;

    @ApiModelProperty("是否收集列统计信息（空值率、唯一值等）")
    private Boolean collectColumnStats;

    @ApiModelProperty("是否覆盖已有元数据（true=覆盖，false=跳过已存在）")
    private Boolean overwriteExisting;

    @ApiModelProperty("创建用户ID")
    private Long createUser;

    public enum ScanScope {
        ALL,
        SPECIFIED,
        PATTERN
    }
}
