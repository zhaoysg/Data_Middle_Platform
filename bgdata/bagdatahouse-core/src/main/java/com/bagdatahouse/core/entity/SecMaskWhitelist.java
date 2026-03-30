package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 脱敏白名单实体
 * <p>
 * 用户/角色 + 有效期，期内不脱敏
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_mask_whitelist")
@ApiModel(description = "脱敏白名单")
public class SecMaskWhitelist {

    @ApiModelProperty("白名单ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("关联脱敏策略ID")
    private Long strategyId;

    @ApiModelProperty("白名单类型：USER-用户 / ROLE-角色")
    private String entityType;

    @ApiModelProperty("用户ID或角色ID")
    private Long entityId;

    @ApiModelProperty("用户姓名或角色名称")
    private String entityName;

    @ApiModelProperty("白名单类型：FULL_EXEMPT-完全豁免 / PARTIAL_EXEMPT-部分豁免")
    private String whitelistType;

    @ApiModelProperty("生效开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @ApiModelProperty("生效结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty("申请原因")
    private String reason;

    @ApiModelProperty("审批人ID")
    private Long approverId;

    @ApiModelProperty("审批人姓名")
    private String approverName;

    @ApiModelProperty("审批时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    @ApiModelProperty("审批意见")
    private String approveComment;

    @ApiModelProperty("状态：ACTIVE-生效 / EXPIRED-已过期 / REVOKED-已撤销")
    private String status;

    @ApiModelProperty("删除标记")
    @TableLogic
    private Integer deleted;

    @ApiModelProperty("创建者")
    private Long createUser;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新者")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
