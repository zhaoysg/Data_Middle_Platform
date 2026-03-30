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
 * Data warehouse layer entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dq_data_layer")
@ApiModel(description = "Data warehouse layer")
public class DqDataLayer {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Layer code (e.g., ods, dwd, dws, ads)")
    private String layerCode;

    @ApiModelProperty("Layer name")
    private String layerName;

    @ApiModelProperty("Layer description")
    private String layerDesc;

    @ApiModelProperty("Layer color for UI display")
    private String layerColor;

    @ApiModelProperty("Sort order")
    private Integer sortOrder;

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
