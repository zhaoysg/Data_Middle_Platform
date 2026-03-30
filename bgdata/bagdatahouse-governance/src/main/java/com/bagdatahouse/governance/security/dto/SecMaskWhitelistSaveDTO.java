package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脱敏白名单保存 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "脱敏白名单保存参数")
public class SecMaskWhitelistSaveDTO {

    @ApiModelProperty("白名单ID（编辑时传入）")
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
    private String startTime;

    @ApiModelProperty("生效结束时间")
    private String endTime;

    @ApiModelProperty("申请原因")
    private String reason;

    @ApiModelProperty("创建者")
    private Long createUser;
}
