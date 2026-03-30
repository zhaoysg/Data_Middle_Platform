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
 * 敏感字段访问申请表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_access_application")
@ApiModel(description = "敏感字段访问申请")
public class SecAccessApplication {

    @ApiModelProperty("申请ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("申请编号")
    private String applicationNo;

    @ApiModelProperty("申请人ID")
    private Long applicantId;

    @ApiModelProperty("申请人名称")
    private String applicantName;

    @ApiModelProperty("申请部门ID")
    private Long deptId;

    @ApiModelProperty("申请部门名称")
    private String deptName;

    @ApiModelProperty("目标数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标数据源名称")
    private String targetDsName;

    @ApiModelProperty("目标表名")
    private String targetTable;

    @ApiModelProperty("目标字段列表（JSON数组）")
    private String targetColumns;

    @ApiModelProperty("目标字段名称列表（逗号分隔，供显示）")
    private String targetColumnNames;

    @ApiModelProperty("访问时长类型：HOUR/DAY/WEEK/CUSTOM")
    private String durationType;

    @ApiModelProperty("访问时长值（小时）")
    private Integer durationHours;

    @ApiModelProperty("申请开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @ApiModelProperty("申请截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty("申请理由")
    private String applyReason;

    @ApiModelProperty("申请状态：PENDING/APPROVED/REJECTED/EXPIRED/CANCELLED")
    private String status;

    @ApiModelProperty("审批人ID")
    private Long approverId;

    @ApiModelProperty("审批人名称")
    private String approverName;

    @ApiModelProperty("审批意见")
    private String approvalComment;

    @ApiModelProperty("审批时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalTime;

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
