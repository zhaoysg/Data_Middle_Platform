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
 * 数据分类与分级推荐绑定配置实体
 * <p>
 * 定义分类→等级的推荐绑定关系，用于：
 * - 选择分类时自动推荐绑定等级（"客户数据" → L3/L4，"公开数据" → L1）
 * - 新增/编辑字段级血缘时，校验目标字段等级 ≥ 源字段等级
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_class_level_binding")
@ApiModel(description = "数据分类与分级推荐绑定")
public class SecClassLevelBinding {

    @ApiModelProperty("绑定ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("分类ID（关联 sec_classification.id）")
    private Long classId;

    @ApiModelProperty("分级ID（关联 sec_level.id）")
    private Long levelId;

    @ApiModelProperty("是否为推荐绑定：0-可选，1-推荐")
    private Integer isRecommended;

    @ApiModelProperty("绑定说明，如 客户数据通常为敏感等级")
    private String bindingDesc;

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
