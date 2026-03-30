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
 * 数据分类实体（参考《数据安全法》一级分类）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_classification")
@ApiModel(description = "数据分类")
public class SecClassification {

    @ApiModelProperty("分类ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("分类编码，唯一，如 C01")
    private String classCode;

    @ApiModelProperty("分类名称，如 客户数据")
    private String className;

    @ApiModelProperty("分类说明")
    private String classDesc;

    @ApiModelProperty("排序号")
    private Integer classOrder;

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
