package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 脱敏白名单列表项 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "脱敏白名单列表项")
public class SecMaskWhitelistVO {

    @ApiModelProperty("白名单ID")
    private Long id;

    @ApiModelProperty("关联策略ID")
    private Long strategyId;

    @ApiModelProperty("策略名称")
    private String strategyName;

    @ApiModelProperty("策略编码")
    private String strategyCode;

    @ApiModelProperty("白名单类型：USER-用户 / ROLE-角色")
    private String entityType;

    @ApiModelProperty("白名单类型中文名")
    private String entityTypeLabel;

    @ApiModelProperty("用户ID或角色ID")
    private Long entityId;

    @ApiModelProperty("用户姓名或角色名称")
    private String entityName;

    @ApiModelProperty("豁免类型：FULL_EXEMPT-完全豁免 / PARTIAL_EXEMPT-部分豁免")
    private String whitelistType;

    @ApiModelProperty("豁免类型中文名")
    private String whitelistTypeLabel;

    @ApiModelProperty("生效开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty("生效结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("是否在有效期内")
    private Boolean inEffect;

    @ApiModelProperty("申请原因")
    private String reason;

    @ApiModelProperty("审批人ID")
    private Long approverId;

    @ApiModelProperty("审批人姓名")
    private String approverName;

    @ApiModelProperty("审批时间")
    private LocalDateTime approveTime;

    @ApiModelProperty("审批意见")
    private String approveComment;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("状态中文名")
    private String statusLabel;

    @ApiModelProperty("创建者ID")
    private Long createUser;

    @ApiModelProperty("创建人用户名")
    private String createUserName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新者ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;
}
