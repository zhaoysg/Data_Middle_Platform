package com.bagdatahouse.governance.lineage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "血缘视图对象")
public class LineageVO {

    @ApiModelProperty("血缘ID")
    private Long id;

    @ApiModelProperty("血缘类型：TABLE-表级/COLUMN-字段级")
    private String lineageType;

    @ApiModelProperty("血缘类型标签")
    private String lineageTypeLabel;

    @ApiModelProperty("源数据源ID")
    private Long sourceDsId;

    @ApiModelProperty("源数据源名称")
    private String sourceDsName;

    @ApiModelProperty("源数据源编码")
    private String sourceDsCode;

    @ApiModelProperty("源数据源类型")
    private String sourceDsType;

    @ApiModelProperty("源表名")
    private String sourceTable;

    @ApiModelProperty("源字段名")
    private String sourceColumn;

    @ApiModelProperty("源字段中文名")
    private String sourceColumnAlias;

    @ApiModelProperty("目标数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标数据源名称")
    private String targetDsName;

    @ApiModelProperty("目标数据源编码")
    private String targetDsCode;

    @ApiModelProperty("目标数据源类型")
    private String targetDsType;

    @ApiModelProperty("目标表名")
    private String targetTable;

    @ApiModelProperty("目标字段名")
    private String targetColumn;

    @ApiModelProperty("目标字段中文名")
    private String targetColumnAlias;

    @ApiModelProperty("转换类型")
    private String transformType;

    @ApiModelProperty("转换类型标签")
    private String transformTypeLabel;

    @ApiModelProperty("转换表达式描述")
    private String transformExpr;

    @ApiModelProperty("来源作业ID")
    private Long jobId;

    @ApiModelProperty("来源作业名称")
    private String jobName;

    @ApiModelProperty("血缘来源")
    private String lineageSource;

    @ApiModelProperty("血缘来源标签")
    private String lineageSourceLabel;

    @ApiModelProperty("所属部门ID")
    private Long deptId;

    @ApiModelProperty("所属部门名称")
    private String deptName;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建人名称")
    private String createUserName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新人ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
