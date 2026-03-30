package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 字段敏感等级保存 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "字段敏感等级保存参数")
public class SecColumnSensitivitySaveDTO {

    @ApiModelProperty("记录ID（编辑时传入）")
    private Long id;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("元数据ID")
    private Long metadataId;

    @ApiModelProperty("表名")
    private String tableName;

    @ApiModelProperty("字段名")
    private String columnName;

    @ApiModelProperty("字段注释")
    private String columnComment;

    @ApiModelProperty("数据类型")
    private String dataType;

    @ApiModelProperty("匹配规则ID")
    private Long matchRuleId;

    @ApiModelProperty("数据分类ID")
    private Long classId;

    @ApiModelProperty("数据分级ID")
    private Long levelId;

    @ApiModelProperty("脱敏方式：NONE/MASK/HIDE/ENCRYPT/DELETE")
    private String maskType;

    @ApiModelProperty("脱敏格式")
    private String maskPattern;

    @ApiModelProperty("脱敏位置：CENTER/TAIL/HEAD/FULL")
    private String maskPosition;

    @ApiModelProperty("脱敏字符")
    private String maskChar;

    @ApiModelProperty("匹配置信度")
    private BigDecimal confidence;

    @ApiModelProperty("扫描批次号")
    private String scanBatchNo;

    @ApiModelProperty("创建者")
    private Long createUser;
}
