package com.bagdatahouse.dprofile.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 列级探查结果 VO
 * 包含列的完整统计信息和分布可视化数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "列级探查结果")
public class ColumnProfileResultVO {

    @ApiModelProperty("列统计记录ID")
    private Long id;

    @ApiModelProperty("表统计ID")
    private Long tableStatsId;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("目标表名")
    private String tableName;

    @ApiModelProperty("目标列名")
    private String columnName;

    @ApiModelProperty("数据类型")
    private String dataType;

    @ApiModelProperty("是否可空")
    private Boolean nullable;

    @ApiModelProperty("探查时间")
    private LocalDateTime profileTime;

    // ===== 基础统计 =====
    @ApiModelProperty("总行数")
    private Long totalCount;

    @ApiModelProperty("空值数")
    private Long nullCount;

    @ApiModelProperty("空值率(%)")
    private BigDecimal nullRate;

    @ApiModelProperty("唯一值数量")
    private Long uniqueCount;

    @ApiModelProperty("唯一率(%)")
    private BigDecimal uniqueRate;

    // ===== 数值统计 =====
    @ApiModelProperty("最小值")
    private String minValue;

    @ApiModelProperty("最大值")
    private String maxValue;

    @ApiModelProperty("平均值")
    private BigDecimal avgValue;

    @ApiModelProperty("中位数")
    private BigDecimal medianValue;

    @ApiModelProperty("标准差")
    private BigDecimal stdDev;

    @ApiModelProperty("零值数量")
    private Long zeroCount;

    @ApiModelProperty("零值率(%)")
    private BigDecimal zeroRate;

    @ApiModelProperty("负值数量")
    private Long negativeCount;

    // ===== 字符串统计 =====
    @ApiModelProperty("最小长度")
    private Integer minLength;

    @ApiModelProperty("最大长度")
    private Integer maxLength;

    @ApiModelProperty("平均长度")
    private BigDecimal avgLength;

    // ===== 分布数据 =====
    @ApiModelProperty("Top-N 值列表")
    private List<TopValueItem> topValues;

    @ApiModelProperty("直方图数据")
    private List<HistogramBucket> histogram;

    // ===== 异常检测 =====
    @ApiModelProperty("异常值数量")
    private Long outlierCount;

    @ApiModelProperty("异常值率(%)")
    private BigDecimal outlierRate;

    @ApiModelProperty("异常检测方法: IQR / 3SIGMA")
    private String outlierMethod;

    @ApiModelProperty("异常检测详情")
    private OutlierDetail outlierDetail;

    // ===== 数据类型标识 =====
    @ApiModelProperty("数据类型分类: NUMERIC / STRING / DATETIME / BOOLEAN / OTHER")
    private String dataTypeCategory;

    @ApiModelProperty("异常警告信息")
    private List<String> warnings;

    // ===== 内部字段 =====
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * Top-N 值项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopValueItem {
        @ApiModelProperty("值")
        private String value;

        @ApiModelProperty("出现次数")
        private Long count;

        @ApiModelProperty("占比(%)")
        private BigDecimal rate;
    }

    /**
     * 直方图桶
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistogramBucket {
        @ApiModelProperty("区间标签")
        private String range;

        @ApiModelProperty("区间下限")
        private Double lower;

        @ApiModelProperty("区间上限")
        private Double upper;

        @ApiModelProperty("计数")
        private Long count;

        @ApiModelProperty("占比(%)")
        private BigDecimal rate;
    }

    /**
     * 异常检测详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OutlierDetail {
        @ApiModelProperty("Q1")
        private Double q1;

        @ApiModelProperty("Q3")
        private Double q3;

        @ApiModelProperty("IQR")
        private Double iqr;

        @ApiModelProperty("下界")
        private Double lowerBound;

        @ApiModelProperty("上界")
        private Double upperBound;

        @ApiModelProperty("均值（3σ法）")
        private Double mean;

        @ApiModelProperty("标准差（3σ法）")
        private Double std;
    }
}
