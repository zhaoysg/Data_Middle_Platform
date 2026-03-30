package com.bagdatahouse.datasource.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据源连接池配置 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "连接池配置")
public class DruidPoolConfigVO {

    @ApiModelProperty("初始连接数")
    private Integer initialSize = 5;

    @ApiModelProperty("最小空闲连接数")
    private Integer minIdle = 5;

    @ApiModelProperty("最大活跃连接数")
    private Integer maxActive = 20;

    @ApiModelProperty("获取连接超时时间(毫秒)")
    private Long maxWait = 60000L;

    @ApiModelProperty("连接有效性检测间隔(毫秒)")
    private Long timeBetweenEvictionRunsMillis = 60000L;

    @ApiModelProperty("连接最小空闲时间(毫秒)")
    private Long minEvictableIdleTimeMillis = 300000L;

    @ApiModelProperty("连接检测SQL")
    private String validationQuery = "SELECT 1";

    @ApiModelProperty("检测时是否validateObject")
    private Boolean testWhileIdle = true;

    @ApiModelProperty("获取连接时是否检测")
    private Boolean testOnBorrow = false;

    @ApiModelProperty("归还连接时是否检测")
    private Boolean testOnReturn = false;

    @ApiModelProperty("是否启用连接池")
    private Boolean poolPreparedStatements = false;
}
