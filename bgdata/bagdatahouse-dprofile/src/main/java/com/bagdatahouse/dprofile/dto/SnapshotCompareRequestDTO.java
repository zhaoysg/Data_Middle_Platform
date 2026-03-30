package com.bagdatahouse.dprofile.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 快照比对请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "快照比对请求")
public class SnapshotCompareRequestDTO {

    @ApiModelProperty(value = "快照A ID", required = true)
    private Long snapshotAId;

    @ApiModelProperty(value = "快照B ID", required = true)
    private Long snapshotBId;

    @ApiModelProperty(value = "比对类型: TABLE/COLUMN/FULL", notes = "TABLE=仅表级比对, COLUMN=含列级比对, FULL=完整比对(含分布)")
    private String compareType;

    @ApiModelProperty(value = "比对名称")
    private String compareName;

    // ===== 表级比对参数 =====
    @ApiModelProperty(value = "表级行数变化阈值(%)", notes = "超过此阈值标记为显著变化，默认5")
    private BigDecimal rowCountChangeThreshold;

    @ApiModelProperty(value = "表级列数变化阈值(%)", notes = "超过此阈值标记为显著变化，默认0")
    private BigDecimal columnCountChangeThreshold;

    // ===== 列级比对参数 =====
    @ApiModelProperty(value = "空值率变化阈值(%)", notes = "超过此阈值标记为显著变化，默认10")
    private BigDecimal nullRateChangeThreshold;

    @ApiModelProperty(value = "唯一率变化阈值(%)", notes = "超过此阈值标记为显著变化，默认10")
    private BigDecimal uniqueRateChangeThreshold;

    @ApiModelProperty(value = "最小/最大值变化是否比对", notes = "是否比对列的最小/最大值变化")
    private Boolean checkMinMaxChange;

    // ===== 分布比对参数 =====
    @ApiModelProperty(value = "Top-N 值变化比对数量", notes = "比对频率最高的N个值，默认5")
    private Integer topNCompareCount;

    @ApiModelProperty(value = "Top-N 值占比变化阈值(%)", notes = "某值占比变化超过此阈值标记为显著，默认5")
    private BigDecimal topNChangeThreshold;

    /**
     * 获取或默认的行数变化阈值
     */
    public BigDecimal getRowCountChangeThreshold() {
        return rowCountChangeThreshold != null ? rowCountChangeThreshold : new BigDecimal("5");
    }

    /**
     * 获取或默认的表级列数变化阈值
     */
    public BigDecimal getColumnCountChangeThreshold() {
        return columnCountChangeThreshold != null ? columnCountChangeThreshold : BigDecimal.ZERO;
    }

    /**
     * 获取或默认的空值率变化阈值
     */
    public BigDecimal getNullRateChangeThreshold() {
        return nullRateChangeThreshold != null ? nullRateChangeThreshold : new BigDecimal("10");
    }

    /**
     * 获取或默认的唯一率变化阈值
     */
    public BigDecimal getUniqueRateChangeThreshold() {
        return uniqueRateChangeThreshold != null ? uniqueRateChangeThreshold : new BigDecimal("10");
    }

    /**
     * 获取或默认的Top-N比对数量
     */
    public Integer getTopNCompareCount() {
        return topNCompareCount != null ? topNCompareCount : 5;
    }

    /**
     * 获取或默认的Top-N变化阈值
     */
    public BigDecimal getTopNChangeThreshold() {
        return topNChangeThreshold != null ? topNChangeThreshold : new BigDecimal("5");
    }

    /**
     * 获取或默认的最小最大值变化检查
     */
    public Boolean getCheckMinMaxChange() {
        return checkMinMaxChange != null ? checkMinMaxChange : true;
    }
}
