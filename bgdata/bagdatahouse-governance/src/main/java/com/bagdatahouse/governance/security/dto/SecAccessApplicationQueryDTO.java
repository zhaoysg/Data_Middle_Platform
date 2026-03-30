package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 敏感字段访问申请查询 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段访问申请查询参数")
public class SecAccessApplicationQueryDTO {

    @ApiModelProperty("申请ID")
    private Long id;

    @ApiModelProperty("申请编号")
    private String applicationNo;

    @ApiModelProperty("申请人ID")
    private Long applicantId;

    @ApiModelProperty("申请人名称（模糊搜索）")
    private String applicantName;

    @ApiModelProperty("申请部门ID")
    private Long deptId;

    @ApiModelProperty("目标数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标表名（模糊搜索）")
    private String targetTable;

    @ApiModelProperty("申请状态：PENDING/APPROVED/REJECTED/EXPIRED/CANCELLED")
    private String status;

    @ApiModelProperty("审批人ID")
    private Long approverId;

    @ApiModelProperty("申请开始时间（范围查询开始）")
    private String startTimeStart;

    @ApiModelProperty("申请开始时间（范围查询结束）")
    private String startTimeEnd;

    @ApiModelProperty("创建时间（范围查询开始）")
    private String createTimeStart;

    @ApiModelProperty("创建时间（范围查询结束）")
    private String createTimeEnd;
}
