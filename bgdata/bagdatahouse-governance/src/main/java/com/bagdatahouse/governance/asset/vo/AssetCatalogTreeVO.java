package com.bagdatahouse.governance.asset.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资产目录树节点 VO（用于左侧树形结构）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "资产目录树节点")
public class AssetCatalogTreeVO {

    @ApiModelProperty("目录ID")
    private Long id;

    @ApiModelProperty("父目录ID")
    private Long parentId;

    @ApiModelProperty("目录类型：BUSINESS_DOMAIN/DATA_DOMAIN/ALBUM")
    private String catalogType;

    @ApiModelProperty("目录类型中文名")
    private String catalogTypeLabel;

    @ApiModelProperty("目录名称")
    private String catalogName;

    @ApiModelProperty("目录编码")
    private String catalogCode;

    @ApiModelProperty("目录描述")
    private String catalogDesc;

    @ApiModelProperty("封面图片URL")
    private String coverImage;

    @ApiModelProperty("资产数量")
    private Integer itemCount;

    @ApiModelProperty("访问次数")
    private Long accessCount;

    @ApiModelProperty("负责人用户ID")
    private Long ownerId;

    @ApiModelProperty("负责人用户名")
    private String ownerName;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("可见性：0-私有，1-公开")
    private Integer visible;

    @ApiModelProperty("可见性中文名")
    private String visibleLabel;

    @ApiModelProperty("状态：ACTIVE-启用，INACTIVE-禁用")
    private String status;

    @ApiModelProperty("状态中文名")
    private String statusLabel;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("子目录列表")
    private java.util.List<AssetCatalogTreeVO> children;
}
