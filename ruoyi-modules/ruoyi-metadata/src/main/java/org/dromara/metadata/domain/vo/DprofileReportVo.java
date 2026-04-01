package org.dromara.metadata.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.metadata.domain.DprofileReport;

import java.time.LocalDateTime;

/**
 * 数据探查报告视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DprofileReportVo extends DprofileReport {

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 数据源名称
     */
    private String dsName;

    /**
     * 数据源编码
     */
    private String dsCode;

    /**
     * 探查耗时（毫秒）
     */
    private Long elapsedMs;

    /**
     * 探查时间文本
     */
    private String profileTimeText;
}
