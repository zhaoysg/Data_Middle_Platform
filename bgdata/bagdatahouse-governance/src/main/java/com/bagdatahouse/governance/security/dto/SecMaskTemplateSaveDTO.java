package com.bagdatahouse.governance.security.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 脱敏模板保存 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "脱敏模板保存参数")
public class SecMaskTemplateSaveDTO {

    @ApiModelProperty("模板ID（编辑时传入）")
    private Long id;

    @ApiModelProperty("模板名称")
    private String templateName;

    @ApiModelProperty("模板编码")
    private String templateCode;

    @ApiModelProperty("模板说明")
    private String templateDesc;

    @ApiModelProperty("适用数据类型：STRING/NUMBER/DATE/ALL")
    private String dataType;

    @ApiModelProperty("脱敏类型：NONE/MASK/HIDE/ENCRYPT/DELETE")
    private String maskType;

    @ApiModelProperty("脱敏位置：CENTER/TAIL/HEAD/FULL")
    private String maskPosition;

    @ApiModelProperty("脱敏字符")
    private String maskChar;

    @ApiModelProperty("头部保留字符数")
    private Integer maskHeadKeep;

    @ApiModelProperty("尾部保留字符数")
    private Integer maskTailKeep;

    @ApiModelProperty("脱敏格式模板")
    private String maskPattern;

    @ApiModelProperty("状态：0-禁用，1-启用")
    private Integer status;

    @ApiModelProperty("创建者")
    private Long createUser;
}
