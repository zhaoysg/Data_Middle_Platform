package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Table profile stats entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dprofile_table_stats")
@ApiModel(description = "Table profile statistics")
public class DprofileTableStats {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Profile task ID")
    private Long taskId;

    @ApiModelProperty("Execution ID")
    private Long executionId;

    @ApiModelProperty("Data source ID")
    private Long dsId;

    @ApiModelProperty("Table name")
    private String tableName;

    @ApiModelProperty("Profile time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime profileTime;

    @ApiModelProperty("Row count")
    private Long rowCount;

    @ApiModelProperty("Column count")
    private Integer columnCount;

    @ApiModelProperty("Storage size in bytes")
    private BigDecimal storageBytes;

    @ApiModelProperty("Table comment")
    private String tableComment;

    @ApiModelProperty("Last update time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @ApiModelProperty("Increment rows since last profile")
    private Long incrementRows;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
