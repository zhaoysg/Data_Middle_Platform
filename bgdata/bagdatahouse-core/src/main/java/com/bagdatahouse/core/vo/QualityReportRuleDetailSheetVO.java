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
 * 质量报告-规则明细导出VO（用于Excel多Sheet导出）
 * Sheet2: 规则明细
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "质量报告-规则明细导出VO")
@HeadRowHeight(25)
@ContentRowHeight(18)
public class QualityReportRuleDetailSheetVO {

    @ExcelProperty(value = "执行编号", index = 0)
    @ColumnWidth(22)
    @ApiModelProperty("执行编号")
    private String executionNo;

    @ExcelProperty(value = "规则名称", index = 1)
    @ColumnWidth(25)
    @ApiModelProperty("规则名称")
    private String ruleName;

    @ExcelProperty(value = "规则编码", index = 2)
    @ColumnWidth(20)
    @ApiModelProperty("规则编码")
    private String ruleCode;

    @ExcelProperty(value = "规则类型", index = 3)
    @ColumnWidth(15)
    @ApiModelProperty("规则类型")
    private String ruleType;

    @ExcelProperty(value = "规则强度", index = 4)
    @ColumnWidth(10)
    @ApiModelProperty("STRONG/WEAK")
    private String ruleStrength;

    @ExcelProperty(value = "质量维度", index = 5)
    @ColumnWidth(20)
    @ApiModelProperty("所属维度")
    private String dimensions;

    @ExcelProperty(value = "目标数据源", index = 6)
    @ColumnWidth(15)
    @ApiModelProperty("目标数据源ID")
    private Long targetDsId;

    @ExcelProperty(value = "目标表", index = 7)
    @ColumnWidth(30)
    @ApiModelProperty("目标表名")
    private String targetTable;

    @ExcelProperty(value = "目标列", index = 8)
    @ColumnWidth(20)
    @ApiModelProperty("目标列名")
    private String targetColumn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "开始时间", index = 9)
    @ColumnWidth(20)
    @ApiModelProperty("开始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "结束时间", index = 10)
    @ColumnWidth(20)
    @ApiModelProperty("结束时间")
    private LocalDateTime endTime;

    @ExcelProperty(value = "耗时(ms)", index = 11)
    @ColumnWidth(12)
    @ApiModelProperty("耗时毫秒")
    private Long elapsedMs;

    @ExcelProperty(value = "执行状态", index = 12)
    @ColumnWidth(12)
    @ApiModelProperty("SUCCESS/FAILED/SKIPPED")
    private String status;

    @ExcelProperty(value = "总数据量", index = 13)
    @ColumnWidth(15)
    @ApiModelProperty("总数据量")
    private Long totalCount;

    @ExcelProperty(value = "错误数量", index = 14)
    @ColumnWidth(12)
    @ApiModelProperty("错误数量")
    private Long errorCount;

    @ExcelProperty(value = "通过数量", index = 15)
    @ColumnWidth(12)
    @ApiModelProperty("通过数量")
    private Long passCount;

    @ExcelProperty(value = "实际值", index = 16)
    @ColumnWidth(15)
    @ApiModelProperty("实际检测值")
    private BigDecimal actualValue;

    @ExcelProperty(value = "阈值最小值", index = 17)
    @ColumnWidth(15)
    @ApiModelProperty("阈值最小值")
    private BigDecimal thresholdMin;

    @ExcelProperty(value = "阈值最大值", index = 18)
    @ColumnWidth(15)
    @ApiModelProperty("阈值最大值")
    private BigDecimal thresholdMax;

    @ExcelProperty(value = "规则评分", index = 19)
    @ColumnWidth(10)
    @ApiModelProperty("该规则质量评分")
    private Integer qualityScore;

    @ExcelProperty(value = "错误详情", index = 20)
    @ColumnWidth(40)
    @ApiModelProperty("错误详情描述")
    private String errorDetail;
}
