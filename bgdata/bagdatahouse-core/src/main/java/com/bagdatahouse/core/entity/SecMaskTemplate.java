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
 * 脱敏规则模板实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_mask_template")
@ApiModel(description = "脱敏规则模板")
public class SecMaskTemplate {

    @ApiModelProperty("模板ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("模板名称")
    private String templateName;

    @ApiModelProperty("模板编码，唯一")
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

    @ApiModelProperty("是否内置：0-自定义，1-内置")
    private Integer builtin;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

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
