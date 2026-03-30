package com.bagdatahouse.governance.security.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 敏感字段扫描结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段扫描结果")
public class SecScanResultVO {

    @ApiModelProperty("扫描批次号")
    private String scanBatchNo;

    @ApiModelProperty("扫描开始时间")
    private String startTime;

    @ApiModelProperty("扫描结束时间")
    private String endTime;

    @ApiModelProperty("扫描耗时（毫秒）")
    private Long elapsedMs;

    @ApiModelProperty("扫描数据源ID")
    private Long dsId;

    @ApiModelProperty("扫描数据源名称")
    private String dsName;

    @ApiModelProperty("扫描总表数")
    private Integer totalTableCount;

    @ApiModelProperty("扫描总字段数")
    private Integer totalColumnCount;

    @ApiModelProperty("发现敏感字段数")
    private Integer foundSensitiveCount;

    @ApiModelProperty("扫描结果列表")
    private List<SecColumnSensitivityVO> results;

    @ApiModelProperty("按分级分布")
    private java.util.Map<String, Integer> countByLevel;

    @ApiModelProperty("按分类分布")
    private java.util.Map<String, Integer> countByClassification;
}
