package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "字段敏感记录")
@AutoMapper(target = SecColumnSensitivity.class)
public class SecColumnSensitivityVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long dsId;

    @Schema(description = "数据源名称")
    private String dsName;

    private String tableName;

    private String columnName;

    @Schema(description = "字段注释")
    private String columnComment;

    private String dataType;

    @Schema(description = "敏感等级编码")
    private String levelCode;

    @Schema(description = "敏感等级名称")
    private String levelName;

    @Schema(description = "敏感等级颜色")
    private String levelColor;

    @Schema(description = "数据分类编码")
    private String classCode;

    @Schema(description = "数据分类名称")
    private String className;

    @Schema(description = "匹配规则ID")
    private Long matchRuleId;

    @Schema(description = "匹配规则名称")
    private String matchRuleName;

    @Schema(description = "识别方式 AUTO/MANUAL")
    private String identifiedBy;

    @Schema(description = "确认状态 0待确认 1已确认")
    private String confirmed;

    @Schema(description = "置信度 0-100")
    private BigDecimal confidence;

    @Schema(description = "置信度标签")
    private String confidenceLabel;

    @Schema(description = "建议脱敏方式")
    private String maskType;

    @Schema(description = "建议脱敏方式标签")
    private String maskTypeLabel;

    @Schema(description = "建议脱敏模板")
    private String maskPattern;

    @Schema(description = "建议脱敏位置")
    private String maskPosition;

    @Schema(description = "扫描批次号")
    private String scanBatchNo;

    @Schema(description = "扫描时间")
    private LocalDateTime scanTime;

    private Long scanTaskId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
