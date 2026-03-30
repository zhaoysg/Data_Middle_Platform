package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段敏感等级查询 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "字段敏感等级查询参数")
public class SecColumnSensitivityQueryDTO {

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("表名（模糊匹配）")
    private String tableName;

    @ApiModelProperty("字段名（模糊匹配）")
    private String columnName;

    @ApiModelProperty("数据分类ID")
    private Long classId;

    @ApiModelProperty("数据分级ID")
    private Long levelId;

    @ApiModelProperty("审核状态：PENDING/APPROVED/REJECTED")
    private String reviewStatus;

    @ApiModelProperty("扫描批次号")
    private String scanBatchNo;

    @ApiModelProperty("敏感级别：L1/L2/L3/L4")
    private String sensitivityLevel;

    @ApiModelProperty("记录ID（用于详情查询）")
    private Long id;
}
