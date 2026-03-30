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
 * 敏感字段访问审计日志表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("sec_access_log")
@ApiModel(description = "敏感字段访问审计日志")
public class SecAccessLog {

    @ApiModelProperty("日志ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("关联的申请ID")
    private Long applicationId;

    @ApiModelProperty("申请编号")
    private String applicationNo;

    @ApiModelProperty("操作人ID")
    private Long operatorId;

    @ApiModelProperty("操作人名称")
    private String operatorName;

    @ApiModelProperty("操作人部门")
    private String operatorDept;

    @ApiModelProperty("操作类型：APPLY-申请/APPROVE-审批/REJECT-拒绝/ACCESS-访问/CANCEL-取消/EXPIRE-过期")
    private String operationType;

    @ApiModelProperty("操作内容描述")
    private String operationContent;

    @ApiModelProperty("目标数据源ID")
    private Long targetDsId;

    @ApiModelProperty("目标表名")
    private String targetTable;

    @ApiModelProperty("目标字段名")
    private String targetColumn;

    @ApiModelProperty("IP地址")
    private String ipAddress;

    @ApiModelProperty("用户代理")
    private String userAgent;

    @ApiModelProperty("操作后状态")
    private String status;

    @ApiModelProperty("备注")
    private String remark;

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
