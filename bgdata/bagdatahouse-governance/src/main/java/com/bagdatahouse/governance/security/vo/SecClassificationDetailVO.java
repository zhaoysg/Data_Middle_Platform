package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据分类详情 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "数据分类详情")
public class SecClassificationDetailVO {

    @ApiModelProperty("分类ID")
    private Long id;

    @ApiModelProperty("分类编码")
    private String classCode;

    @ApiModelProperty("分类名称")
    private String className;

    @ApiModelProperty("分类说明")
    private String classDesc;

    @ApiModelProperty("排序号")
    private Integer classOrder;

    @ApiModelProperty("敏感级别（展示用，关联等级中最严的）")
    private String sensitivityLevel;

    @ApiModelProperty("敏感级别中文名")
    private String sensitivityLevelLabel;

    @ApiModelProperty("状态")
    private Integer status;

    @ApiModelProperty("状态中文名")
    private String statusLabel;

    @ApiModelProperty("关联规则数量")
    private Integer ruleCount;

    @ApiModelProperty("关联敏感字段数量")
    private Integer sensitiveFieldCount;

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
