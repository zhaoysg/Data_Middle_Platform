package com.bagdatahouse.core.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 字段敏感等级记录实体（扫描结果 + 手动配置）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_column_sensitivity")
@ApiModel(description = "字段敏感等级记录")
public class SecColumnSensitivity {

    @ApiModelProperty("记录ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("元数据ID")
    private Long metadataId;

    @ApiModelProperty("表名")
    private String tableName;

    @ApiModelProperty("字段名")
    private String columnName;

    @ApiModelProperty("字段注释")
    private String columnComment;

    @ApiModelProperty("数据类型")
    private String dataType;

    @ApiModelProperty("数据分类ID")
    private Long classId;

    @ApiModelProperty("数据分级ID")
    private Long levelId;

    @ApiModelProperty("匹配到的识别规则ID")
    private Long matchRuleId;

    @ApiModelProperty("脱敏方式：NONE/MASK/HIDE/ENCRYPT/DELETE")
    private String maskType;

    @ApiModelProperty("脱敏格式")
    private String maskPattern;

    @ApiModelProperty("脱敏位置：CENTER/TAIL/HEAD/FULL")
    private String maskPosition;

    @ApiModelProperty("脱敏字符")
    private String maskChar;

    @ApiModelProperty("识别置信度 0-100")
    private BigDecimal confidence;

    @ApiModelProperty("扫描批次号")
    private String scanBatchNo;

    @ApiModelProperty("扫描时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scanTime;

    @ApiModelProperty("审核状态：PENDING/APPROVED/REJECTED")
    private String reviewStatus;

    @ApiModelProperty("审核意见")
    private String reviewComment;

    @ApiModelProperty("审核人")
    private Long approvedBy;

    @ApiModelProperty("审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedTime;

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
