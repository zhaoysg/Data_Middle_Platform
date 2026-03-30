package com.bagdatahouse.governance.asset.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 目录收录资产 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "目录收录资产")
public class CatalogAssetVO {

    @ApiModelProperty("元数据ID")
    private Long metadataId;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("数据源名称")
    private String dsName;

    @ApiModelProperty("数据源类型")
    private String dsType;

    @ApiModelProperty("数据层：ODS/DWD/DWS/ADS")
    private String dataLayer;

    @ApiModelProperty("数据域")
    private String dataDomain;

    @ApiModelProperty("表名")
    private String tableName;

    @ApiModelProperty("表中文名/别名")
    private String tableAlias;

    @ApiModelProperty("表注释")
    private String tableComment;

    @ApiModelProperty("表类型：TABLE/VIEW")
    private String tableType;

    @ApiModelProperty("行数")
    private Long rowCount;

    @ApiModelProperty("存储大小（字节）")
    private BigDecimal storageBytes;

    @ApiModelProperty("敏感等级")
    private String sensitivityLevel;

    @ApiModelProperty("敏感等级中文名")
    private String sensitivityLevelLabel;

    @ApiModelProperty("最后修改时间")
    private LocalDateTime lastModifiedAt;

    @ApiModelProperty("录入目录时间")
    private LocalDateTime createTime;
}
