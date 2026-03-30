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
 * Column profile stats entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("dprofile_column_stats")
@ApiModel(description = "Column profile statistics")
public class DprofileColumnStats {

    @ApiModelProperty("Primary key")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("Table stats ID")
    private Long tableStatsId;

    @ApiModelProperty("Execution ID")
    private Long executionId;

    @ApiModelProperty("Data source ID")
    private Long dsId;

    @ApiModelProperty("Table name")
    private String tableName;

    @ApiModelProperty("Column name")
    private String columnName;

    @ApiModelProperty("Profile time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime profileTime;

    @ApiModelProperty("Data type")
    private String dataType;

    @ApiModelProperty("Is nullable")
    private Boolean nullable;

    @ApiModelProperty("Total count")
    private Long totalCount;

    @ApiModelProperty("Null count")
    private Long nullCount;

    @ApiModelProperty("Null rate")
    private BigDecimal nullRate;

    @ApiModelProperty("Unique count")
    private Long uniqueCount;

    @ApiModelProperty("Unique rate")
    private BigDecimal uniqueRate;

    @ApiModelProperty("Minimum value")
    private String minValue;

    @ApiModelProperty("Maximum value")
    private String maxValue;

    @ApiModelProperty("Average value")
    private BigDecimal avgValue;

    @ApiModelProperty("Median value")
    private BigDecimal medianValue;

    @ApiModelProperty("Standard deviation")
    private BigDecimal stdDev;

    @ApiModelProperty("Minimum length")
    private Integer minLength;

    @ApiModelProperty("Maximum length")
    private Integer maxLength;

    @ApiModelProperty("Average length")
    private BigDecimal avgLength;

    @ApiModelProperty("Top values (JSON string)")
    private String topValues;

    @ApiModelProperty("Histogram data (JSON string)")
    private String histogram;

    @ApiModelProperty("Outlier count")
    private Long outlierCount;

    @ApiModelProperty("Outlier rate")
    private BigDecimal outlierRate;

    @ApiModelProperty("Outlier method: IQR / 3SIGMA")
    private String outlierMethod;

    @ApiModelProperty("Zero count")
    private Long zeroCount;

    @ApiModelProperty("Zero rate")
    private BigDecimal zeroRate;

    @ApiModelProperty("Negative count")
    private Long negativeCount;

    @ApiModelProperty("Create time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
