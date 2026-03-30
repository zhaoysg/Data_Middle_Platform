package com.bagdatahouse.governance.lineage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "血缘新增/编辑请求")
public class LineageSaveDTO {

    @ApiModelProperty(value = "血缘ID（编辑时传入）", required = false)
    private Long id;

    @NotBlank(message = "血缘类型不能为空")
    @ApiModelProperty(value = "血缘类型：TABLE-表级/COLUMN-字段级", required = true)
    private String lineageType;

    @NotNull(message = "源数据源ID不能为空")
    @ApiModelProperty(value = "源数据源ID", required = true)
    private Long sourceDsId;

    @NotBlank(message = "源表名不能为空")
    @ApiModelProperty(value = "源表名", required = true)
    private String sourceTable;

    @ApiModelProperty(value = "源字段名（字段级血缘必填）")
    private String sourceColumn;

    @ApiModelProperty(value = "源字段中文名")
    private String sourceColumnAlias;

    @NotNull(message = "目标数据源ID不能为空")
    @ApiModelProperty(value = "目标数据源ID", required = true)
    private Long targetDsId;

    @NotBlank(message = "目标表名不能为空")
    @ApiModelProperty(value = "目标表名", required = true)
    private String targetTable;

    @ApiModelProperty(value = "目标字段名（字段级血缘必填）")
    private String targetColumn;

    @ApiModelProperty(value = "目标字段中文名")
    private String targetColumnAlias;

    @ApiModelProperty(value = "转换类型：DIRECT/SUM/AVG/COUNT/MAX/MIN/CONCAT/CASE_WHEN/CUSTOM_EXPR")
    private String transformType;

    @ApiModelProperty(value = "转换表达式描述")
    private String transformExpr;

    @ApiModelProperty(value = "来源作业ID（Kettle接入后填充）")
    private Long jobId;

    @ApiModelProperty(value = "来源作业名称")
    private String jobName;

    @ApiModelProperty(value = "血缘来源：MANUAL-手动录入/AUTO_PARSER-自动解析")
    private String lineageSource;

    @ApiModelProperty(value = "所属部门ID")
    private Long deptId;
}
