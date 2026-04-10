package org.dromara.metadata.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "敏感字段扫描结果")
public class SecScanResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "数据源ID")
    private Long dsId;

    @Schema(description = "数据源名称")
    private String dsName;

    @Schema(description = "扫描开始时间")
    private Long startTime;

    @Schema(description = "扫描结束时间")
    private Long endTime;

    @Schema(description = "扫描耗时（毫秒）")
    private Long costMs;

    @Schema(description = "总表数")
    private Integer totalTableCount;

    @Schema(description = "总字段数")
    private Integer totalColumnCount;

    @Schema(description = "已扫描表数")
    private Integer scannedTableCount;

    @Schema(description = "已扫描字段数")
    private Integer scannedColumnCount;

    @Schema(description = "发现敏感字段数")
    private Integer foundSensitiveCount;

    @Schema(description = "新增敏感字段数")
    private Integer newSensitiveCount;

    @Schema(description = "更新敏感字段数")
    private Integer updatedSensitiveCount;

    @Schema(description = "扫描状态：SUCCESS/FAILED/PARTIAL")
    private String status;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "发现的敏感字段列表")
    private List<SecColumnSensitivityVo> sensitiveFields;
}
