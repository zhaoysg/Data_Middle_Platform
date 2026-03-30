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
 * 敏感字段访问审批记录表（审批流程历史）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_access_audit")
@ApiModel(description = "敏感字段访问审批记录")
public class SecAccessAudit {

    @ApiModelProperty("审批记录ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("申请ID")
    private Long applicationId;

    @ApiModelProperty("申请编号")
    private String applicationNo;

    @ApiModelProperty("审批人ID")
    private Long approverId;

    @ApiModelProperty("审批人名称")
    private String approverName;

    @ApiModelProperty("审批人部门")
    private String approverDept;

    @ApiModelProperty("审批操作：APPROVE/REJECT")
    private String action;

    @ApiModelProperty("审批意见")
    private String comment;

    @ApiModelProperty("审批结果：AGREE/REJECT")
    private String result;

    @ApiModelProperty("审批节点：FIRST/SECOND/FINAL")
    private String auditNode;

    @ApiModelProperty("审批时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    @ApiModelProperty("审批来源：SYSTEM/MANUAL")
    private String auditSource;

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
