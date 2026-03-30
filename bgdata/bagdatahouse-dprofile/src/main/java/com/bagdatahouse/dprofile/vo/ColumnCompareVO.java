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

/**
 * 列级比对结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "列级比对结果")
public class ColumnCompareVO {

    // ===== 字段基本信息 =====
    @ApiModelProperty("列名")
    private String columnName;

    @ApiModelProperty("数据类型")
    private String dataType;

    @ApiModelProperty("数据类型分类")
    private String dataTypeCategory;

    // ===== 比对结果标识 =====
    @ApiModelProperty("比对状态: UNCHANGED/SIGNIFICANT/MINOR/NEW/REMOVED")
    private String status;

    @ApiModelProperty("差异级别: NONE/MINOR/SIGNIFICANT")
    private String diffLevel;

    // ===== 总行数 =====
    @ApiModelProperty("快照A总行数")
    private Long snapshotATotalCount;

    @ApiModelProperty("快照B总行数")
    private Long snapshotBTotalCount;

    @ApiModelProperty("总行数变化")
    private Long totalCountChange;

    // ===== 空值 =====
    @ApiModelProperty("快照A空值数")
    private Long snapshotANullCount;

    @ApiModelProperty("快照B空值数")
    private Long snapshotBNullCount;

    @ApiModelProperty("空值数变化")
    private Long nullCountChange;

    @ApiModelProperty("快照A空值率(%)")
    private BigDecimal snapshotANullRate;

    @ApiModelProperty("快照B空值率(%)")
    private BigDecimal snapshotBNullRate;

    @ApiModelProperty("空值率变化(百分点)")
    private BigDecimal nullRateChange;

    @ApiModelProperty("空值率是否显著变化")
    private Boolean nullRateSignificant;

    // ===== 唯一性 =====
    @ApiModelProperty("快照A唯一值数量")
    private Long snapshotAUniqueCount;

    @ApiModelProperty("快照B唯一值数量")
    private Long snapshotBUniqueCount;

    @ApiModelProperty("唯一值数量变化")
    private Long uniqueCountChange;

    @ApiModelProperty("快照A唯一率(%)")
    private BigDecimal snapshotAUniqueRate;

    @ApiModelProperty("快照B唯一率(%)")
    private BigDecimal snapshotBUniqueRate;

    @ApiModelProperty("唯一率变化(百分点)")
    private BigDecimal uniqueRateChange;

    @ApiModelProperty("唯一率是否显著变化")
    private Boolean uniqueRateSignificant;

    // ===== 数值列统计 =====
    @ApiModelProperty("快照A最小值")
    private String snapshotAMinValue;

    @ApiModelProperty("快照B最小值")
    private String snapshotBMinValue;

    @ApiModelProperty("最小值是否变化")
    private Boolean minValueChanged;

    @ApiModelProperty("快照A最大值")
    private String snapshotAMaxValue;

    @ApiModelProperty("快照B最大值")
    private String snapshotBMaxValue;

    @ApiModelProperty("最大值是否变化")
    private Boolean maxValueChanged;

    @ApiModelProperty("快照A平均值")
    private BigDecimal snapshotAAvgValue;

    @ApiModelProperty("快照B平均值")
    private BigDecimal snapshotBAvgValue;

    @ApiModelProperty("平均值是否显著变化")
    private Boolean avgValueSignificant;

    @ApiModelProperty("快照A标准差")
    private BigDecimal snapshotAStdDev;

    @ApiModelProperty("快照B标准差")
    private BigDecimal snapshotBStdDev;

    // ===== 字符串列统计 =====
    @ApiModelProperty("快照A最小长度")
    private Integer snapshotAMinLength;

    @ApiModelProperty("快照B最小长度")
    private Integer snapshotBMinLength;

    @ApiModelProperty("最小长度是否变化")
    private Boolean minLengthChanged;

    @ApiModelProperty("快照A最大长度")
    private Integer snapshotAMaxLength;

    @ApiModelProperty("快照B最大长度")
    private Integer snapshotBMaxLength;

    @ApiModelProperty("最大长度是否变化")
    private Boolean maxLengthChanged;

    @ApiModelProperty("快照A平均长度")
    private BigDecimal snapshotAAvgLength;

    @ApiModelProperty("快照B平均长度")
    private BigDecimal snapshotBAvgLength;

    // ===== Top-N 值变化 =====
    @ApiModelProperty("Top-N 值变化列表")
    private List<TopNValueCompare> topNChanges;

    // ===== 异常值变化 =====
    @ApiModelProperty("快照A异常值数量")
    private Long snapshotAOutlierCount;

    @ApiModelProperty("快照B异常值数量")
    private Long snapshotBOutlierCount;

    @ApiModelProperty("异常值数量是否变化")
    private Boolean outlierCountChanged;

    // ===== 差异描述 =====
    @ApiModelProperty("差异摘要描述")
    private String diffSummary;

    /**
     * Top-N 值比对项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopNValueCompare {
        @ApiModelProperty("值")
        private String value;

        @ApiModelProperty("比对状态: NEW/REMOVED/RATE_CHANGED")
        private String status;

        @ApiModelProperty("快照A占比(%)")
        private BigDecimal snapshotARate;

        @ApiModelProperty("快照B占比(%)")
        private BigDecimal snapshotBRate;

        @ApiModelProperty("占比变化(百分点)")
        private BigDecimal rateChange;

        @ApiModelProperty("占比变化是否显著")
        private Boolean significant;
    }
}
