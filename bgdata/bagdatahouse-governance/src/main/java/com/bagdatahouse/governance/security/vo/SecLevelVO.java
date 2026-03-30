package com.bagdatahouse.governance.security.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据分级列表项 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据分级列表项")
public class SecLevelVO {

    @ApiModelProperty("分级ID")
    private Long id;

    @ApiModelProperty("分级编码")
    private String levelCode;

    @ApiModelProperty("分级名称")
    private String levelName;

    @ApiModelProperty("分级说明")
    private String levelDesc;

    @ApiModelProperty("等级值 1-4")
    private Integer levelValue;

    @ApiModelProperty("展示颜色")
    private String color;

    @ApiModelProperty("展示图标")
    private String icon;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("状态中文名")
    private String statusLabel;

    @ApiModelProperty("关联敏感字段数量")
    private Integer sensitiveFieldCount;

    @ApiModelProperty("创建者ID")
    private Long createUser;

    @ApiModelProperty("创建人用户名")
    private String createUserName;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新者ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
