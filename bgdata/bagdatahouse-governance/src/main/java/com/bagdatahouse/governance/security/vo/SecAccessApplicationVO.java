package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 敏感字段访问申请 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段访问申请")
public class SecAccessApplicationVO {

    @ApiModelProperty("申请ID")
    private Long id;

    @ApiModelProperty("申请编号")
    private String applicationNo;

    @ApiModelProperty("申请人ID")
    private Long applicantId;

    @ApiModelProperty("申请人名称")
    private String applicantName;

    @ApiModelProperty("申请部门ID")
    private Long deptId;

    @ApiModelProperty("申请部门名称")
    private String deptName;

    @ApiModelProperty("目标数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标数据源名称")
    private String targetDsName;

    @ApiModelProperty("目标表名")
    private String targetTable;

    @ApiModelProperty("目标字段列表（JSON数组）")
    private String targetColumns;

    @ApiModelProperty("目标字段名称列表（逗号分隔，供显示）")
    private String targetColumnNames;

    @ApiModelProperty("访问时长类型")
    private String durationType;

    @ApiModelProperty("访问时长类型中文名")
    private String durationTypeLabel;

    @ApiModelProperty("访问时长值（小时）")
    private Integer durationHours;

    @ApiModelProperty("申请开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("申请截止时间")
    private LocalDateTime endTime;

    @ApiModelProperty("申请理由")
    private String applyReason;

    @ApiModelProperty("申请状态")
    private String status;

    @ApiModelProperty("申请状态中文名")
    private String statusLabel;

    @ApiModelProperty("审批人ID")
    private Long approverId;

    @ApiModelProperty("审批人名称")
    private String approverName;

    @ApiModelProperty("审批意见")
    private String approvalComment;

    @ApiModelProperty("审批时间")
    private LocalDateTime approvalTime;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("是否可审批（当前用户是否为审批人）")
    private Boolean canApprove;

    @ApiModelProperty("是否可撤销")
    private Boolean canCancel;

    @ApiModelProperty("是否已过期")
    private Boolean isExpired;
}
