package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 脱敏规则模板列表项 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "脱敏规则模板列表项")
public class SecMaskTemplateVO {

    @ApiModelProperty("模板ID")
    private Long id;

    @ApiModelProperty("模板名称")
    private String templateName;

    @ApiModelProperty("模板编码")
    private String templateCode;

    @ApiModelProperty("模板说明")
    private String templateDesc;

    @ApiModelProperty("适用数据类型")
    private String dataType;

    @ApiModelProperty("适用数据类型中文名")
    private String dataTypeLabel;

    @ApiModelProperty("脱敏类型")
    private String maskType;

    @ApiModelProperty("脱敏类型中文名")
    private String maskTypeLabel;

    @ApiModelProperty("脱敏位置")
    private String maskPosition;

    @ApiModelProperty("脱敏字符")
    private String maskChar;

    @ApiModelProperty("头部保留字符数")
    private Integer maskHeadKeep;

    @ApiModelProperty("尾部保留字符数")
    private Integer maskTailKeep;

    @ApiModelProperty("脱敏格式模板")
    private String maskPattern;

    @ApiModelProperty("是否内置")
    private Integer builtin;

    @ApiModelProperty("是否内置中文名")
    private String builtinLabel;

    @ApiModelProperty("状态")
    private Integer status;

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
