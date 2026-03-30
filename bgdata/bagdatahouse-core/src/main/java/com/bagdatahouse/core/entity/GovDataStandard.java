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
 * 数据标准定义实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("gov_data_standard")
@ApiModel(description = "数据标准定义")
public class GovDataStandard {

    @ApiModelProperty("标准ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("标准编码")
    private String standardCode;

    @ApiModelProperty("标准名称")
    private String standardName;

    @ApiModelProperty("标准类型：CODE_STANDARD-编码标准/NAMING_STANDARD-命名规范/PRIMARY_DATA-主数据")
    private String standardType;

    @ApiModelProperty("标准分类")
    private String standardCategory;

    @ApiModelProperty("标准描述")
    private String standardDesc;

    @ApiModelProperty("标准内容")
    private String standardContent;

    @ApiModelProperty("校验表达式")
    private String ruleExpr;

    @ApiModelProperty("示例值")
    private String exampleValue;

    @ApiModelProperty("适用对象：TABLE_NAME-表名/COLUMN_NAME-列名/DATA_VALUE-数据值")
    private String applicableObject;

    @ApiModelProperty("不合规处理方式：ALERT-仅告警/BLOCK-阻断创建")
    private String enforceAction;

    @ApiModelProperty("状态：DRAFT-草稿/PUBLISHED-已发布/DEPRECATED-已废弃")
    private String status;

    @ApiModelProperty("业务域")
    private String bizDomain;

    @ApiModelProperty("负责人")
    private Long ownerId;

    @ApiModelProperty("所属部门")
    private Long deptId;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("是否启用：0-禁用，1-启用")
    private Integer enabled;

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
