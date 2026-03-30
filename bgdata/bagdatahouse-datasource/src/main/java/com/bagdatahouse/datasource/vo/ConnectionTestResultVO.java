package com.bagdatahouse.datasource.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 连接测试结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "连接测试结果")
public class ConnectionTestResultVO {

    @ApiModelProperty("是否成功")
    private Boolean success;

    @ApiModelProperty("测试消息")
    private String message;

    @ApiModelProperty("数据库版本")
    private String databaseVersion;

    @ApiModelProperty("测试耗时(毫秒)")
    private Long elapsedMs;

    @ApiModelProperty("测试时间")
    private LocalDateTime testTime;
}
