package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据质量执行记录实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("dqc_execution")
public class DqcExecution extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
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
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
