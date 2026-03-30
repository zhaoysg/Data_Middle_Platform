package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 敏感字段访问审批 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段访问审批参数")
public class SecAccessApprovalDTO {

    @NotNull(message = "申请ID不能为空")
    @ApiModelProperty("申请ID")
    private Long applicationId;

    @NotNull(message = "审批操作不能为空")
    @ApiModelProperty("审批操作：APPROVE-批准/REJECT-拒绝")
    private String action;

    @ApiModelProperty("审批意见")
    private String comment;

    @ApiModelProperty("审批人ID")
    private Long approverId;

    @ApiModelProperty("审批人名称")
    private String approverName;

    @ApiModelProperty("审批人部门")
    private String approverDept;
}
