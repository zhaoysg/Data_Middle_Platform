package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 脱敏策略列表项 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "脱敏策略列表项")
public class SecMaskStrategyVO {

    @ApiModelProperty("策略ID")
    private Long id;

    @ApiModelProperty("策略名称")
    private String strategyName;

    @ApiModelProperty("策略编码")
    private String strategyCode;

    @ApiModelProperty("应用场景")
    private String sceneType;

    @ApiModelProperty("应用场景中文名")
    private String sceneTypeLabel;

    @ApiModelProperty("策略描述")
    private String strategyDesc;

    @ApiModelProperty("等级→脱敏类型映射JSON")
    private String levelMaskMapping;

    @ApiModelProperty("白名单数量")
    private Integer whitelistCount;

    @ApiModelProperty("白名单全局有效期")
    private LocalDateTime whitelistExpiry;

    @ApiModelProperty("优先级")
    private Integer priority;

    @ApiModelProperty("是否启用冲突检测")
    private Integer conflictCheck;

    @ApiModelProperty("策略状态")
    private String status;

    @ApiModelProperty("策略状态中文名")
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
