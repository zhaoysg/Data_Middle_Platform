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
 * 脱敏策略实体
 * <p>
 * 支持 sceneType + sensitivityLevel → maskType 映射，以及白名单配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_mask_strategy")
@ApiModel(description = "脱敏策略")
public class SecMaskStrategy {

    @ApiModelProperty("策略ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("策略名称")
    private String strategyName;

    @ApiModelProperty("策略编码（唯一）")
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime whitelistExpiry;

    @ApiModelProperty("优先级（数字越小优先级越高）")
    private Integer priority;

    @ApiModelProperty("是否启用冲突检测：0-否，1-是")
    private Integer conflictCheck;

    @ApiModelProperty("策略状态：ENABLED-启用 / DISABLED-禁用")
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
