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
 * 术语实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("gov_glossary_term")
@ApiModel(description = "术语")
public class GovGlossaryTerm {

    @ApiModelProperty("术语ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("术语编码")
    private String termCode;

    @ApiModelProperty("术语名称（中文）")
    private String termName;

    @ApiModelProperty("英文名")
    private String termNameEn;

    @ApiModelProperty("别名/简称，多个用逗号分隔")
    private String termAlias;

    @ApiModelProperty("所属分类ID")
    private Long categoryId;

    @ApiModelProperty("业务域")
    private String bizDomain;

    @ApiModelProperty("术语定义/业务解释")
    private String definition;

    @ApiModelProperty("计算公式")
    private String formula;

    @ApiModelProperty("数据类型：STRING/NUMBER/DATE/BOOLEAN")
    private String dataType;

    @ApiModelProperty("单位")
    private String unit;

    @ApiModelProperty("示例值")
    private String exampleValue;

    @ApiModelProperty("敏感等级：PUBLIC/INTERNAL/CONFIDENTIAL/SECRET")
    private String sensitivityLevel;

    @ApiModelProperty("状态：DRAFT-草稿/PUBLISHED-已发布/DEPRECATED-已废弃")
    private String status;

    @ApiModelProperty("术语负责人")
    private Long ownerId;

    @ApiModelProperty("所属部门")
    private Long deptId;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;

    @ApiModelProperty("发布时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedTime;

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
