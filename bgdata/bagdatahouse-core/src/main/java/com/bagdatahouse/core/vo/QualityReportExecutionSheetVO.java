package com.bagdatahouse.core.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
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
 * 质量报告明细导出VO（用于Excel多Sheet导出）
 * Sheet1: 执行概览
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "质量报告-执行概览导出VO")
@HeadRowHeight(25)
@ContentRowHeight(18)
public class QualityReportExecutionSheetVO {

    @ExcelProperty(value = "执行编号", index = 0)
    @ColumnWidth(22)
    @ApiModelProperty("执行编号")
    private String executionNo;

    @ExcelProperty(value = "质检方案", index = 1)
    @ColumnWidth(20)
    @ApiModelProperty("质检方案名称")
    private String planName;

    @ExcelProperty(value = "数据层", index = 2)
    @ColumnWidth(10)
    @ApiModelProperty("数据层 ODS/DWD/DWS/ADS")
    private String layerCode;

    @ExcelProperty(value = "触发方式", index = 3)
    @ColumnWidth(10)
    @ApiModelProperty("触发方式")
    private String triggerType;

    @ExcelProperty(value = "执行状态", index = 4)
    @ColumnWidth(12)
    @ApiModelProperty("执行状态")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "开始时间", index = 5)
    @ColumnWidth(20)
    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "结束时间", index = 6)
    @ColumnWidth(20)
    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

    @ExcelProperty(value = "耗时(ms)", index = 7)
    @ColumnWidth(12)
    @ApiModelProperty("耗时毫秒")
    private Long elapsedMs;

    @ExcelProperty(value = "总规则数", index = 8)
    @ColumnWidth(10)
    @ApiModelProperty("总规则数")
    private Integer totalRules;

    @ExcelProperty(value = "通过规则数", index = 9)
    @ColumnWidth(12)
    @ApiModelProperty("通过规则数")
    private Integer passedRules;

    @ExcelProperty(value = "失败规则数", index = 10)
    @ColumnWidth(12)
    @ApiModelProperty("失败规则数")
    private Integer failedRules;

    @ExcelProperty(value = "跳过规则数", index = 11)
    @ColumnWidth(10)
    @ApiModelProperty("跳过规则数")
    private Integer skippedRules;

    @ExcelProperty(value = "强规则阻断", index = 12)
    @ColumnWidth(12)
    @ApiModelProperty("是否阻断")
    private String blocked;

    @ExcelProperty(value = "质量评分", index = 13)
    @ColumnWidth(10)
    @ApiModelProperty("质量评分 0-100")
    private Integer qualityScore;

    @ExcelProperty(value = "综合评分", index = 14)
    @ColumnWidth(12)
    @ApiModelProperty("综合评分(百分制)")
    private BigDecimal overallScore;

    @ExcelProperty(value = "完整性评分", index = 15)
    @ColumnWidth(14)
    @ApiModelProperty("完整性维度评分")
    private BigDecimal completenessScore;

    @ExcelProperty(value = "唯一性评分", index = 16)
    @ColumnWidth(14)
    @ApiModelProperty("唯一性维度评分")
    private BigDecimal uniquenessScore;

    @ExcelProperty(value = "准确性评分", index = 17)
    @ColumnWidth(14)
    @ApiModelProperty("准确性维度评分")
    private BigDecimal accuracyScore;

    @ExcelProperty(value = "一致性评分", index = 18)
    @ColumnWidth(14)
    @ApiModelProperty("一致性维度评分")
    private BigDecimal consistencyScore;

    @ExcelProperty(value = "及时性评分", index = 19)
    @ColumnWidth(14)
    @ApiModelProperty("及时性维度评分")
    private BigDecimal timelinessScore;

    @ExcelProperty(value = "有效性评分", index = 20)
    @ColumnWidth(14)
    @ApiModelProperty("有效性维度评分")
    private BigDecimal validityScore;

    @ApiModelProperty("规则通过率")
    private BigDecimal rulePassRate;
}
