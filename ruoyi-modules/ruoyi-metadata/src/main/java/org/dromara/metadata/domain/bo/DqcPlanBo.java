package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.DqcPlan;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据质量检查方案业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DqcPlan.class, reverseConvertGenerate = false)
public class DqcPlanBo extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 方案名称
     */
    @NotBlank(message = "方案名称不能为空")
    @Size(max = 100, message = "方案名称长度不能超过{max}个字符")
    private String planName;

    /**
     * 方案编码
     */
    @Size(max = 50, message = "方案编码长度不能超过{max}个字符")
    private String planCode;

    /**
     * 方案描述
     */
    @Size(max = 500, message = "方案描述长度不能超过{max}个字符")
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
     * 关键词搜索
     */
    private String keyword;
}
