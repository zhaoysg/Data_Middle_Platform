package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.DqcPlan;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据质量检查方案视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DqcPlan.class)
public class DqcPlanVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 方案名称
     */
    private String planName;

    /**
     * 方案编码
     */
    private String planCode;

    /**
     * 方案描述
     */
    private String planDesc;

    /**
     * 绑定类型：TABLE/DOMAIN/LAYER/PATTERN
     */
    private String bindType;

    /**
     * 绑定值
     */
    private String bindValue;

    /**
     * 数据层级编码
     */
    private String layerCode;

    /**
     * 触发类型：MANUAL/SCHEDULE/API
     */
    private String triggerType;

    /**
     * 触发Cron表达式
     */
    private String triggerCron;

    /**
     * 失败是否告警：1=告警
     */
    private String alertOnFailure;

    /**
     * 成功是否告警：1=告警
     */
    private String alertOnSuccess;

    /**
     * 是否阻塞：1=阻塞
     */
    private String autoBlock;

    /**
     * 敏感等级（L1/L2/L3/L4）
     */
    private String sensitivityLevel;

    /**
     * 状态：DRAFT/PUBLISHED/DISABLED
     */
    private String status;

    /**
     * 规则数量
     */
    private Integer ruleCount;

    /**
     * 表数量
     */
    private Integer tableCount;

    /**
     * 最后执行ID
     */
    private Long lastExecutionId;

    /**
     * 最后得分
     */
    private BigDecimal lastScore;

    /**
     * 最后得分（格式化字符串）
     */
    private String lastScoreStr;

    /**
     * 最后执行时间
     */
    private LocalDateTime lastExecutionTime;

    /**
     * 最后执行时间（格式化字符串）
     */
    private String lastExecutionTimeStr;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
