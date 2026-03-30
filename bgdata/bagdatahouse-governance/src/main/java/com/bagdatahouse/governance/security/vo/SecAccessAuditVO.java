package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 敏感字段访问审批记录 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段访问审批记录")
public class SecAccessAuditVO {

    @ApiModelProperty("审批记录ID")
    private Long id;

    @ApiModelProperty("申请ID")
    private Long applicationId;

    @ApiModelProperty("申请编号")
    private String applicationNo;

    @ApiModelProperty("审批人ID")
    private Long approverId;

    @ApiModelProperty("审批人名称")
    private String approverName;

    @ApiModelProperty("审批人部门")
    private String approverDept;

    @ApiModelProperty("审批操作")
    private String action;

    @ApiModelProperty("审批操作中文名")
    private String actionLabel;

    @ApiModelProperty("审批意见")
    private String comment;

    @ApiModelProperty("审批结果")
    private String result;

    @ApiModelProperty("审批结果中文名")
    private String resultLabel;

    @ApiModelProperty("审批节点")
    private String auditNode;

    @ApiModelProperty("审批节点中文名")
    private String auditNodeLabel;

    @ApiModelProperty("审批时间")
    private LocalDateTime auditTime;

    @ApiModelProperty("审批来源")
    private String auditSource;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
