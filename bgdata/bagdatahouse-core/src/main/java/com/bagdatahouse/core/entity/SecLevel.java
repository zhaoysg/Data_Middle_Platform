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
 * 数据分级实体（参考《个人信息保护法》四级分级）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_level")
@ApiModel(description = "数据分级")
public class SecLevel {

    @ApiModelProperty("分级ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("分级编码，唯一，如 L1/L2/L3/L4")
    private String levelCode;

    @ApiModelProperty("分级名称，如 公开/内部/敏感/机密")
    private String levelName;

    @ApiModelProperty("分级说明")
    private String levelDesc;

    @ApiModelProperty("等级值 1-4，数值越大越敏感")
    private Integer levelValue;

    @ApiModelProperty("页面展示颜色")
    private String color;

    @ApiModelProperty("展示图标")
    private String icon;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

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
