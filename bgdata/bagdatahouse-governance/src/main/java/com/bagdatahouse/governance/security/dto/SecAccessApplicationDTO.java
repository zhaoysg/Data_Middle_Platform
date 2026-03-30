package com.bagdatahouse.governance.security.dto;

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
 * 敏感字段访问申请 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段访问申请参数")
public class SecAccessApplicationDTO {

    @ApiModelProperty("申请ID（编辑时传入）")
    private Long id;

    @NotNull(message = "目标数据源ID不能为空")
    @ApiModelProperty("目标数据源ID")
    private Long targetDsId;

    @NotBlank(message = "目标表名不能为空")
    @ApiModelProperty("目标表名")
    private String targetTable;

    @NotBlank(message = "目标字段不能为空")
    @ApiModelProperty("目标字段列表（JSON数组）")
    private String targetColumns;

    @ApiModelProperty("目标字段名称列表（逗号分隔，供显示）")
    private String targetColumnNames;

    @NotBlank(message = "访问时长类型不能为空")
    @ApiModelProperty("访问时长类型：HOUR/DAY/WEEK/CUSTOM")
    private String durationType;

    @ApiModelProperty("访问时长值（小时）")
    private Integer durationHours;

    @ApiModelProperty("申请开始时间")
    private String startTime;

    @ApiModelProperty("申请截止时间")
    private String endTime;

    @NotBlank(message = "申请理由不能为空")
    @ApiModelProperty("申请理由")
    private String applyReason;

    @ApiModelProperty("创建者")
    private Long createUser;

    @ApiModelProperty("申请人ID")
    private Long applicantId;

    @ApiModelProperty("申请人名称")
    private String applicantName;

    @ApiModelProperty("申请部门ID")
    private Long deptId;

    @ApiModelProperty("申请部门名称")
    private String deptName;
}
