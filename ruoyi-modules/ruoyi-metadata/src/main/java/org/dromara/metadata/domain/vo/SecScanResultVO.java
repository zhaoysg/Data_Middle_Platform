package org.dromara.metadata.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 敏感字段扫描结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "敏感字段扫描结果")
public class SecScanResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("批次号")
    private String batchNo;

    @ApiModelProperty("数据源ID")
    private Long dsId;

    @ApiModelProperty("数据源名称")
    private String dsName;

    @ApiModelProperty("扫描开始时间")
    private Long startTime;

    @ApiModelProperty("扫描结束时间")
    private Long endTime;

    @ApiModelProperty("扫描耗时（毫秒）")
    private Long costMs;

    @ApiModelProperty("总表数")
    private Integer totalTableCount;

    @ApiModelProperty("总字段数")
    private Integer totalColumnCount;

    @ApiModelProperty("已扫描表数")
    private Integer scannedTableCount;

    @ApiModelProperty("已扫描字段数")
    private Integer scannedColumnCount;

    @ApiModelProperty("发现敏感字段数")
    private Integer foundSensitiveCount;

    @ApiModelProperty("新增敏感字段数")
    private Integer newSensitiveCount;

    @ApiModelProperty("更新敏感字段数")
    private Integer updatedSensitiveCount;

    @ApiModelProperty("扫描状态：SUCCESS/FAILED/PARTIAL")
    private String status;

    @ApiModelProperty("错误信息")
    private String errorMessage;

    @ApiModelProperty("发现的敏感字段列表")
    private List<SecColumnSensitivityVo> sensitiveFields;
}
