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
 * 数据标准绑定实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("gov_standard_binding")
@ApiModel(description = "数据标准绑定")
public class GovStandardBinding {

    @ApiModelProperty("主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("标准ID")
    private Long standardId;

    @ApiModelProperty("元数据ID")
    private Long metadataId;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("目标表名")
    private String targetTable;

    @ApiModelProperty("目标列名")
    private String targetColumn;

    @ApiModelProperty("合规状态：PENDING-待检测/COMPLIANT-合规/NON_COMPLIANT-不合规")
    private String complianceStatus;

    @ApiModelProperty("不合规记录数")
    private Integer violationCount;

    @ApiModelProperty("最后检测时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCheckTime;

    @ApiModelProperty("最后检测结果详情")
    private String lastCheckResult;

    @ApiModelProperty("实际执行的处理方式")
    private String enforceAction;

    @ApiModelProperty("创建者")
    private Long createUser;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
