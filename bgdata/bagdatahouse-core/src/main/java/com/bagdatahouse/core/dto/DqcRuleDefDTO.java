package com.bagdatahouse.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
 * DQC规则定义DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "DQC规则定义DTO")
public class DqcRuleDefDTO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("规则名称")
    private String ruleName;

    @ApiModelProperty("规则编码")
    private String ruleCode;

    @ApiModelProperty("模板ID")
    private Long templateId;

    @ApiModelProperty("规则类型")
    private String ruleType;

    @ApiModelProperty("适用级别(table/column/cross_field/cross_table/database)")
    private String applyLevel;

    @ApiModelProperty("数据层编码(与数据源 data_layer 一致，用于展示与筛选)")
    private String layerCode;

    @ApiModelProperty("维度(JSON数组)")
    private String dimensions;

    @ApiModelProperty("规则表达式/SQL")
    private String ruleExpr;

    @ApiModelProperty("目标数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标表名")
    private String targetTable;

    @ApiModelProperty("目标列名")
    private String targetColumn;

    @ApiModelProperty("比对数据源ID")
    private Long compareDsId;

    @ApiModelProperty("比对表名")
    private String compareTable;

    @ApiModelProperty("比对列名")
    private String compareColumn;

    @ApiModelProperty("最小阈值")
    private BigDecimal thresholdMin;

    @ApiModelProperty("最大阈值")
    private BigDecimal thresholdMax;

    @ApiModelProperty("波动阈值百分比")
    private BigDecimal fluctuationThreshold;

    @ApiModelProperty("正则表达式")
    private String regexPattern;

    @ApiModelProperty("错误级别: LOW/MEDIUM/HIGH/CRITICAL")
    private String errorLevel;

    @ApiModelProperty("规则强度: STRONG/WEAK")
    private String ruleStrength;

    @ApiModelProperty("告警接收人(JSON数组)")
    private String alertReceivers;

    @ApiModelProperty("排序顺序")
    private Integer sortOrder;

    @ApiModelProperty("是否启用")
    private Boolean enabled;

    @ApiModelProperty("部门ID")
    private Long deptId;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新人ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    // ========== 展示用字段（非数据库映射）==========
    @ApiModelProperty("目标数据源名称(展示用)")
    private String targetDsName;

    @ApiModelProperty("模板名称(展示用)")
    private String templateName;

    // ========== 自定义函数相关字段 ==========
    @ApiModelProperty("自定义函数类全限定名(CUSTOM_FUNC类型必填)")
    private String customFunctionClass;

    @ApiModelProperty("自定义函数参数字符串(JSON格式，用于传递给函数)")
    private String customFunctionParams;

    @ApiModelProperty("规则类型名称(展示用)")
    private String ruleTypeName;

    @ApiModelProperty("适用级别名称(展示用)")
    private String applyLevelName;

    @ApiModelProperty("规则强度名称(展示用)")
    private String ruleStrengthName;

    @ApiModelProperty("错误级别名称(展示用)")
    private String errorLevelName;

    // ========== 辅助字段（用于批量操作）==========
    @ApiModelProperty("规则ID列表(用于批量操作)")
    private List<Long> ids;

    @ApiModelProperty("是否选中(用于前端选中状态)")
    private Boolean selected;
}
