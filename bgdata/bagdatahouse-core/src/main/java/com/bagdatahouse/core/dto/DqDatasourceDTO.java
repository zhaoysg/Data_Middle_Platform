package com.bagdatahouse.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DQ数据源DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "DQ数据源DTO")
public class DqDatasourceDTO {

    @ApiModelProperty("主键ID")
    private Long id;

    @ApiModelProperty("数据源名称")
    private String dsName;

    @ApiModelProperty("数据源编码")
    private String dsCode;

    @ApiModelProperty("数据源类型")
    private String dsType;

    @ApiModelProperty("主机地址")
    private String host;

    @ApiModelProperty("端口号")
    private Integer port;

    @ApiModelProperty("数据库名称")
    private String databaseName;

    @ApiModelProperty("Schema名称(用于PostgreSQL等，可选)")
    private String schemaName;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("连接参数(JSON)")
    private String connectionParams;

    @ApiModelProperty("数据层编码")
    private String dataLayer;

    @ApiModelProperty("部门ID")
    private Long deptId;

    @ApiModelProperty("负责人ID")
    private Long ownerId;

    @ApiModelProperty("状态: 0-禁用, 1-启用")
    private Integer status;

    @ApiModelProperty("最后测试时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTestTime;

    @ApiModelProperty("最后测试结果")
    private String lastTestResult;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建人ID")
    private Long createUser;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty("更新人ID")
    private Long updateUser;

    @ApiModelProperty("更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @ApiModelProperty("是否测试连接(不持久化)")
    private Boolean testConnection;
}
