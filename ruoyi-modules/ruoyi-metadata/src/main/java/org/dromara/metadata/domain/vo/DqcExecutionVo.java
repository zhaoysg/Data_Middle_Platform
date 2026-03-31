package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.DqcExecution;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据质量执行记录视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DqcExecution.class)
public class DqcExecutionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 执行编号
     */
    private String executionNo;

    /**
     * 方案ID
     */
    private Long planId;

    /**
     * 方案名称
     */
    private String planName;

    /**
     * 层级编码
     */
    private String layerCode;

    /**
     * 触发类型：MANUAL/SCHEDULE/API
     */
    private String triggerType;

    /**
     * 触发用户
     */
    private Long triggerUser;

    /**
     * 触发用户名称
     */
    private String triggerUserName;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 耗时毫秒
     */
    private Long elapsedMs;

    /**
     * 总规则数
     */
    private Integer totalRules;

    /**
     * 通过数
     */
    private Integer passedCount;

    /**
     * 失败数
     */
    private Integer failedCount;

    /**
     * 阻塞数
     */
    private Integer blockedCount;

    /**
     * 总得分
     */
    private BigDecimal overallScore;

    /**
     * 状态：RUNNING/SUCCESS/FAILED/PARTIAL
     */
    private String status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
