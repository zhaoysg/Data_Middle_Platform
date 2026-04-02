package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.metadata.domain.DprofileTask;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据探查任务业务对象
 */
@Data
@NoArgsConstructor
@AutoMapper(target = DprofileTask.class, reverseConvertGenerate = false)
public class DprofileTaskBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 任务名称
     */
    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称长度不能超过{max}个字符")
    private String taskName;

    /**
     * 任务描述
     */
    @Size(max = 500, message = "任务描述长度不能超过{max}个字符")
    private String taskDesc;

    /**
     * 关联数据源ID
     */
    @NotNull(message = "数据源ID不能为空")
    private Long dsId;

    /**
     * 探查级别：BASIC/DETAILED/FULL
     */
    @NotBlank(message = "探查级别不能为空")
    private String profileLevel;

    /**
     * 表名匹配模式（支持通配符）
     */
    @Size(max = 500, message = "表名匹配模式长度不能超过{max}个字符")
    private String tablePattern;

    /**
     * 触发方式：MANUAL/SCHEDULE/API
     */
    private String triggerType;

    /**
     * Cron表达式
     */
    @Size(max = 50, message = "Cron表达式长度不能超过{max}个字符")
    private String cronExpr;

    /**
     * 关键词搜索
     */
    private String keyword;
}
