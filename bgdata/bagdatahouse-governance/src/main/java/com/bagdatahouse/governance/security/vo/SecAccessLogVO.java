package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 敏感字段访问审计日志 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段访问审计日志")
public class SecAccessLogVO {

    @ApiModelProperty("日志ID")
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

    @ApiModelProperty("操作类型")
    private String operationType;

    @ApiModelProperty("操作类型中文名")
    private String operationTypeLabel;

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

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
