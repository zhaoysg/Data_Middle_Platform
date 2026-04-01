package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据质量检查方案实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "dqc_plan", excludeProperty = {"createBy", "updateBy", "createDept"})
public class DqcPlan extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
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
     * 是否阻塞：1=阻塞
     */
    private String autoBlock;

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
     * 最后执行时间
     */
    private java.time.LocalDateTime lastExecutionTime;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
