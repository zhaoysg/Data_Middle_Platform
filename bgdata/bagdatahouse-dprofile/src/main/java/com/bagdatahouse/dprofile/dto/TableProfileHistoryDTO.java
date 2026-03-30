package com.bagdatahouse.dprofile.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表历史探查记录 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "表历史探查记录")
public class TableProfileHistoryDTO {

    @ApiModelProperty("历史记录ID")
    private Long id;

    @ApiModelProperty("执行ID")
    private Long executionId;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("目标表名")
    private String tableName;

    @ApiModelProperty("探查时间")
    private String profileTime;

    @ApiModelProperty("总行数")
    private Long rowCount;

    @ApiModelProperty("列数")
    private Integer columnCount;

    @ApiModelProperty("存储大小(格式化)")
    private String storageSizeFormatted;

    @ApiModelProperty("距上次探查增量行数")
    private Long incrementRows;

    @ApiModelProperty("增量百分比")
    private Double incrementRate;
}
