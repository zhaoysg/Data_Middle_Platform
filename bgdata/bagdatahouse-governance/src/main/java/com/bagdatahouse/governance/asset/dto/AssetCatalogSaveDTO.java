package com.bagdatahouse.governance.asset.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资产目录保存/更新 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "资产目录保存请求")
public class AssetCatalogSaveDTO {

    @ApiModelProperty("目录ID（更新时传入）")
    private Long id;

    @ApiModelProperty("父目录ID，顶级目录为0")
    private Long parentId;

    @ApiModelProperty("目录类型：BUSINESS_DOMAIN/DATA_DOMAIN/ALBUM")
    private String catalogType;

    @ApiModelProperty("目录名称")
    private String catalogName;

    @ApiModelProperty("目录编码")
    private String catalogCode;

    @ApiModelProperty("目录描述")
    private String catalogDesc;

    @ApiModelProperty("封面图片URL")
    private String coverImage;

    @ApiModelProperty("负责人用户ID")
    private Long ownerId;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("可见性：0-私有，1-公开")
    private Integer visible;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("收录的元数据ID列表（专辑收录资产时使用）")
    private List<Long> metadataIds;

    @ApiModelProperty("部门ID")
    private Long deptId;

    @ApiModelProperty("专辑说明（仅专辑类型）")
    private String albumDesc;

    @ApiModelProperty("创建人用户ID")
    private Long createUser;
}
