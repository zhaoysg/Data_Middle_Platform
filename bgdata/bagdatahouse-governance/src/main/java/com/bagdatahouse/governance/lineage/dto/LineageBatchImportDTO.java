package com.bagdatahouse.governance.lineage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量导入血缘请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "批量导入血缘请求")
public class LineageBatchImportDTO {

    @ApiModelProperty(value = "血缘类型：TABLE-表级/COLUMN-字段级", required = true)
    @NotBlank(message = "血缘类型不能为空")
    private String lineageType;

    @ApiModelProperty(value = "血缘来源：MANUAL-手动录入/AUTO_PARSER-自动解析")
    private String lineageSource;

    @ApiModelProperty(value = "血缘记录列表（Excel 中每行一条）", required = true)
    @NotNull(message = "血缘记录列表不能为空")
    private List<LineageRecord> records;

    @ApiModelProperty(value = "所属部门ID")
    private Long deptId;

    @ApiModelProperty(value = "批量导入时全局的源数据源ID（可被单条覆盖）")
    private Long defaultSourceDsId;

    @ApiModelProperty(value = "批量导入时全局的目标数据源ID（可被单条覆盖）")
    private Long defaultTargetDsId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ApiModel(description = "单条血缘记录（对应 Excel 一行）")
    public static class LineageRecord {

        @ApiModelProperty(value = "源数据源ID（为空则使用全局 defaultSourceDsId）")
        private Long sourceDsId;

        @ApiModelProperty(value = "源表名", required = true)
        @NotBlank(message = "源表名不能为空")
        private String sourceTable;

        @ApiModelProperty(value = "源字段名（字段级血缘必填）")
        private String sourceColumn;

        @ApiModelProperty(value = "源字段中文名")
        private String sourceColumnAlias;

        @ApiModelProperty(value = "目标数据源ID（为空则使用全局 defaultTargetDsId）")
        private Long targetDsId;

        @ApiModelProperty(value = "目标表名", required = true)
        @NotBlank(message = "目标表名不能为空")
        private String targetTable;

        @ApiModelProperty(value = "目标字段名（字段级血缘必填）")
        private String targetColumn;

        @ApiModelProperty(value = "目标字段中文名")
        private String targetColumnAlias;

        @ApiModelProperty(value = "转换类型：DIRECT/SUM/AVG/COUNT/MAX/MIN/CONCAT/CASE_WHEN/CUSTOM_EXPR")
        private String transformType;

        @ApiModelProperty(value = "转换表达式描述")
        private String transformExpr;

        @ApiModelProperty(value = "来源作业名称")
        private String jobName;
    }
}
