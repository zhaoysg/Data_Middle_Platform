package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脱敏白名单审批 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "脱敏白名单审批参数")
public class SecMaskWhitelistApproveDTO {

    @ApiModelProperty("白名单ID")
    private Long whitelistId;

    @ApiModelProperty("审批动作：APPROVE-通过 / REJECT-拒绝")
    private String action;

    @ApiModelProperty("审批意见")
    private String comment;

    @ApiModelProperty("审批人ID")
    private Long approverId;

    @ApiModelProperty("审批人姓名")
    private String approverName;
}
