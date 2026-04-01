package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据探查任务实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("dprofile_task")
public class DprofileTask extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 任务编码
     */
    private String taskCode;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String taskDesc;

    /**
     * 关联数据源ID
     */
    private Long dsId;

    /**
     * 探查级别：BASIC/DETAILED/FULL
     */
    private String profileLevel;

    /**
     * 表名匹配模式（支持通配符）
     */
    private String tablePattern;

    /**
     * 触发方式：MANUAL/SCHEDULE/API
     */
    private String triggerType;

    /**
     * Cron表达式
     */
    private String cronExpr;

    /**
     * 状态：DRAFT/RUNNING/SUCCESS/FAILED/STOPPED
     */
    private String status;

    /**
     * 最近运行时间
     */
    private LocalDateTime lastRunTime;

    /**
     * 最近运行状态
     */
    private String lastRunStatus;

    /**
     * 涉及表数量
     */
    private Integer totalTables;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
