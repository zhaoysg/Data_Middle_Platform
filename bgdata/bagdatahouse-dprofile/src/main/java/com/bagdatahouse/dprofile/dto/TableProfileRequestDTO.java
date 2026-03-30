package com.bagdatahouse.dprofile.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表级探查请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "表级探查请求")
public class TableProfileRequestDTO {

    @ApiModelProperty("探查任务ID（可选，为空表示手动一次性探查）")
    private Long taskId;

    @ApiModelProperty("任务名称（手动探查时必填）")
    private String taskName;

    @ApiModelProperty("数据源ID（必填）")
    private Long dsId;

    @ApiModelProperty("目标表名（必填，支持多表逗号分隔）")
    private String tableName;

    @ApiModelProperty("指定探查的列（逗号分隔，为空表示全部列）")
    private String columns;

    @ApiModelProperty("目标 schema（PostgreSQL 专用）")
    private String schema;

    @ApiModelProperty("是否执行列级探查")
    private Boolean collectColumnStats;

    @ApiModelProperty("探查级别: BASIC（仅表级）, DETAILED（表+列基础统计）, FULL（表+列完整统计）")
    private String profileLevel;
}
