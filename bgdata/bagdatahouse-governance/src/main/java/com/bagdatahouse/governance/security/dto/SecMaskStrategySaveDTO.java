package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脱敏策略保存 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "脱敏策略保存参数")
public class SecMaskStrategySaveDTO {

    @ApiModelProperty("策略ID（编辑时传入）")
    private Long id;

    @ApiModelProperty("策略名称")
    private String strategyName;

    @ApiModelProperty("策略编码")
    private String strategyCode;

    @ApiModelProperty("应用场景：DEVELOP_SHOW/DATA_MAP_SHOW/ANALYSIS_QUERY/EXPORT_RESULT/PRINT_REPORT")
    private String sceneType;

    @ApiModelProperty("策略描述")
    private String strategyDesc;

    @ApiModelProperty("敏感等级→脱敏类型映射JSON")
    private String levelMaskMapping;

    @ApiModelProperty("白名单配置JSON")
    private String whitelistConfig;

    @ApiModelProperty("白名单全局有效期")
    private String whitelistExpiry;

    @ApiModelProperty("优先级（数字越小优先级越高）")
    private Integer priority;

    @ApiModelProperty("是否启用冲突检测：0-否，1-是")
    private Integer conflictCheck;

    @ApiModelProperty("创建者")
    private Long createUser;
}
