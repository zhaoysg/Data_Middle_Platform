package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.dromara.metadata.domain.SecColumnSensitivity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 字段敏感记录视图对象
 */
@Data
@ExcelIgnoreUnannotated
@ApiModel("字段敏感记录")
@AutoMapper(target = SecColumnSensitivity.class)
public class SecColumnSensitivityVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long dsId;

    @ApiModelProperty("数据源名称")
    private String dsName;

    private String tableName;

    private String columnName;

    @ApiModelProperty("字段注释")
    private String columnComment;

    private String dataType;

    @ApiModelProperty("敏感等级编码")
    private String levelCode;

    @ApiModelProperty("敏感等级名称")
    private String levelName;

    @ApiModelProperty("敏感等级颜色")
    private String levelColor;

    @ApiModelProperty("数据分类编码")
    private String classCode;

    @ApiModelProperty("数据分类名称")
    private String className;

    @ApiModelProperty("匹配规则ID")
    private Long matchRuleId;

    @ApiModelProperty("匹配规则名称")
    private String matchRuleName;

    @ApiModelProperty("识别方式 AUTO/MANUAL")
    private String identifiedBy;

    @ApiModelProperty("确认状态 0待确认 1已确认")
    private String confirmed;

    @ApiModelProperty("置信度 0-100")
    private BigDecimal confidence;

    @ApiModelProperty("置信度标签")
    private String confidenceLabel;

    @ApiModelProperty("建议脱敏方式")
    private String maskType;

    @ApiModelProperty("建议脱敏方式标签")
    private String maskTypeLabel;

    @ApiModelProperty("建议脱敏模板")
    private String maskPattern;

    @ApiModelProperty("建议脱敏位置")
    private String maskPosition;

    @ApiModelProperty("扫描批次号")
    private String scanBatchNo;

    @ApiModelProperty("扫描时间")
    private LocalDateTime scanTime;

    private Long scanTaskId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
