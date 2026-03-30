package com.bagdatahouse.governance.asset.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 资产目录统计信息 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "资产目录统计信息")
public class AssetCatalogStatsVO {

    @ApiModelProperty("总目录数")
    private long totalCount;

    @ApiModelProperty("业务域数量")
    private long businessDomainCount;

    @ApiModelProperty("数据域数量")
    private long dataDomainCount;

    @ApiModelProperty("数据专辑数量")
    private long albumCount;

    @ApiModelProperty("收录资产总数")
    private long totalAssetCount;

    @ApiModelProperty("按目录类型的资产分布")
    private Map<String, Long> assetCountByType;

    @ApiModelProperty("按数据层的资产分布")
    private Map<String, Long> assetCountByLayer;
}
