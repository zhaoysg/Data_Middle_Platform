package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 元数据扫描结果视图对象
 */
@Data
@ExcelIgnoreUnannotated
public class MetadataScanResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 扫描记录ID
     */
    private Long scanLogId;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 扫描状态：RUNNING / SUCCESS / FAILED / PARTIAL
     */
    private String status;

    /**
     * 总表数
     */
    private Integer totalTables;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 部分成功数
     */
    private Integer partialCount;

    /**
     * 失败数
     */
    private Integer failedCount;

    /**
     * 执行耗时（毫秒）
     */
    private Long elapsedMs;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 错误信息列表
     */
    private List<String> errors;
}
