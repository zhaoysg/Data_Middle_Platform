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
 * 资产目录-元数据关联表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("gov_catalog_metadata")
@ApiModel(description = "资产目录-元数据关联")
public class GovCatalogMetadata {

    @ApiModelProperty("主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("目录ID")
    private Long catalogId;

    @ApiModelProperty("元数据ID")
    private Long metadataId;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
