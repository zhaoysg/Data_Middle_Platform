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
 * 新字段发现告警实体
 * <p>
 * 元数据扫描时，若发现表中有新的字段（之前未采集过），则写入此表并生成告警。
 * 管理员可一键扫描这些新字段，判断是否含敏感信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_new_column_alert")
@ApiModel(description = "新字段发现告警")
public class SecNewColumnAlert {

    @ApiModelProperty("告警ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("表名")
    private String tableName;

    @ApiModelProperty("新发现的字段名")
    private String columnName;

    @ApiModelProperty("字段数据类型")
    private String dataType;

    @ApiModelProperty("字段注释")
    private String columnComment;

    @ApiModelProperty("告警类型：NEW_COLUMN-新字段")
    private String alertType;

    @ApiModelProperty("状态：PENDING-待处理/SCANNED-已扫描/DISMISSED-已忽略")
    private String status;

    @ApiModelProperty("关联的扫描批次号")
    private String scanBatchNo;

    @ApiModelProperty("处理意见")
    private String handleComment;

    @ApiModelProperty("处理人")
    private Long handleUser;

    @ApiModelProperty("处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    @ApiModelProperty("发现时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

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
