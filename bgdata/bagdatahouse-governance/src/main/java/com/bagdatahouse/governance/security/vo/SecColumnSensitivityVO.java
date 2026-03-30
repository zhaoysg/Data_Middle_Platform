package com.bagdatahouse.governance.security.vo;

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
 * 字段敏感等级列表项 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "字段敏感等级列表项")
public class SecColumnSensitivityVO {

    @ApiModelProperty("记录ID")
    private Long id;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("数据源名称")
    private String dsName;

    @ApiModelProperty("表名")
    private String tableName;

    @ApiModelProperty("字段名")
    private String columnName;

    @ApiModelProperty("字段注释")
    private String columnComment;

    @ApiModelProperty("数据类型")
    private String dataType;

    @ApiModelProperty("数据分类ID")
    private Long classId;

    @ApiModelProperty("数据分类名称")
    private String className;

    @ApiModelProperty("数据分级ID")
    private Long levelId;

    @ApiModelProperty("数据分级名称")
    private String levelName;

    @ApiModelProperty("数据分级颜色")
    private String levelColor;

    @ApiModelProperty("匹配到的识别规则ID")
    private Long matchRuleId;

    @ApiModelProperty("匹配到的识别规则名称")
    private String matchRuleName;

    @ApiModelProperty("脱敏方式")
    private String maskType;

    @ApiModelProperty("脱敏方式中文名")
    private String maskTypeLabel;

    @ApiModelProperty("脱敏格式")
    private String maskPattern;

    @ApiModelProperty("脱敏位置")
    private String maskPosition;

    @ApiModelProperty("脱敏字符")
    private String maskChar;

    @ApiModelProperty("识别置信度 0-100")
    private BigDecimal confidence;

    @ApiModelProperty("识别置信度中文名")
    private String confidenceLabel;

    @ApiModelProperty("扫描批次号")
    private String scanBatchNo;

    @ApiModelProperty("扫描时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scanTime;

    @ApiModelProperty("审核状态")
    private String reviewStatus;

    @ApiModelProperty("审核状态中文名")
    private String reviewStatusLabel;

    @ApiModelProperty("审核意见")
    private String reviewComment;

    @ApiModelProperty("审核人ID")
    private Long approvedBy;

    @ApiModelProperty("审核人用户名")
    private String approvedByName;

    @ApiModelProperty("审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedTime;

    @ApiModelProperty("创建者ID")
    private Long createUser;

    @ApiModelProperty("创建人用户名")
    private String createUserName;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新者ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
