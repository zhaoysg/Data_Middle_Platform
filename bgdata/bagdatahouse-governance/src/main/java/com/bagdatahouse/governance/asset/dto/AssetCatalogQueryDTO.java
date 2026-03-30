package com.bagdatahouse.governance.asset.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资产目录查询请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "资产目录查询请求")
public class AssetCatalogQueryDTO {

    @ApiModelProperty("目录类型：BUSINESS_DOMAIN/DATA_DOMAIN/ALBUM")
    private String catalogType;

    @ApiModelProperty("目录名称（模糊匹配）")
    private String catalogName;

    @ApiModelProperty("目录编码（精确匹配）")
    private String catalogCode;

    @ApiModelProperty("可见性：0-私有，1-公开")
    private Integer visible;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("负责人用户ID")
    private Long ownerId;

    @ApiModelProperty("部门ID")
    private Long deptId;

    @ApiModelProperty("是否仅查询顶级目录")
    private Boolean topLevelOnly;
}
