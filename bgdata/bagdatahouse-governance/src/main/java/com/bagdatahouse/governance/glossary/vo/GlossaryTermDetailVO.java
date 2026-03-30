package com.bagdatahouse.governance.glossary.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 术语详情 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "术语详情")
public class GlossaryTermDetailVO {

    @ApiModelProperty("术语ID")
    private Long id;

    @ApiModelProperty("术语编码")
    private String termCode;

    @ApiModelProperty("术语名称（中文）")
    private String termName;

    @ApiModelProperty("英文名")
    private String termNameEn;

    @ApiModelProperty("别名/简称")
    private String termAlias;

    @ApiModelProperty("所属分类ID")
    private Long categoryId;

    @ApiModelProperty("所属分类名称")
    private String categoryName;

    @ApiModelProperty("业务域")
    private String bizDomain;

    @ApiModelProperty("术语定义/业务解释")
    private String definition;

    @ApiModelProperty("计算公式")
    private String formula;

    @ApiModelProperty("数据类型")
    private String dataType;

    @ApiModelProperty("单位")
    private String unit;

    @ApiModelProperty("示例值")
    private String exampleValue;

    @ApiModelProperty("敏感等级")
    private String sensitivityLevel;

    @ApiModelProperty("敏感等级中文名")
    private String sensitivityLevelLabel;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("状态中文名")
    private String statusLabel;

    @ApiModelProperty("是否启用")
    private Integer enabled;

    @ApiModelProperty("是否启用中文名")
    private String enabledLabel;

    @ApiModelProperty("负责人用户ID")
    private Long ownerId;

    @ApiModelProperty("负责人姓名")
    private String ownerName;

    @ApiModelProperty("部门ID")
    private Long deptId;

    @ApiModelProperty("部门名称")
    private String deptName;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("发布时间")
    private LocalDateTime publishedTime;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建人姓名")
    private String createUserName;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新人ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty("字段映射列表")
    private List<GlossaryMappingVO> mappings;
}
