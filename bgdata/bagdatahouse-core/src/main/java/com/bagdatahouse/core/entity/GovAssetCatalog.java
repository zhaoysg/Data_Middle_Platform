package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Asset catalog entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("gov_asset_catalog")
@ApiModel(description = "Asset catalog")
public class GovAssetCatalog {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Parent catalog ID")
    private Long parentId;

    @ApiModelProperty("Catalog type: folder/category/tag")
    private String catalogType;

    @ApiModelProperty("Catalog name")
    private String catalogName;

    @ApiModelProperty("Catalog code")
    private String catalogCode;

    @ApiModelProperty("Catalog description")
    private String catalogDesc;

    @ApiModelProperty("Cover image URL")
    private String coverImage;

    @ApiModelProperty("Item count")
    private Integer itemCount;

    @ApiModelProperty("Access count")
    private Long accessCount;

    @ApiModelProperty("Owner user ID")
    private Long ownerId;

    @ApiModelProperty("Sort order")
    private Integer sortOrder;

    @ApiModelProperty("Visible: 0-private, 1-public")
    private Integer visible;

    @ApiModelProperty("Department ID")
    private Long deptId;

    @ApiModelProperty("Status: ACTIVE/INACTIVE")
    private String status;

    @ApiModelProperty("Soft delete flag")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("Creator user ID")
    private Long createUser;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("Updater user ID")
    private Long updateUser;

    @ApiModelProperty("Update time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
