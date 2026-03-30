package com.bagdatahouse.governance.lineage.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "血缘查询请求")
public class LineageQueryDTO {

    @ApiModelProperty("血缘类型：TABLE/COLUMN")
    private String lineageType;

    @ApiModelProperty("源数据源ID")
    private Long sourceDsId;

    @ApiModelProperty("源表名（模糊匹配）")
    private String sourceTable;

    @ApiModelProperty("源字段名（模糊匹配）")
    private String sourceColumn;

    @ApiModelProperty("目标数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标表名（模糊匹配）")
    private String targetTable;

    @ApiModelProperty("目标字段名（模糊匹配）")
    private String targetColumn;

    @ApiModelProperty("转换类型")
    private String transformType;

    @ApiModelProperty("血缘来源：MANUAL/AUTO_PARSER")
    private String lineageSource;

    @ApiModelProperty("状态：ACTIVE/DEPRECATED")
    private String status;

    @ApiModelProperty("所属部门ID")
    private Long deptId;

    @ApiModelProperty("数据层：ODS/DWD/DWS/ADS")
    private String dataLayer;
}
