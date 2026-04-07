package org.dromara.metadata.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 方案绑定规则请求DTO（支持按表-字段细粒度绑定）
 */
@Data
public class DqcPlanRuleBindBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    @NotNull(message = "规则ID不能为空")
    private Long ruleId;

    /**
     * 排序号
     */
    private Integer ruleOrder = 1;

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 失败时是否跳过
     */
    private Boolean skipOnFailure = false;

    /**
     * 目标表名
     */
    private String targetTable;

    /**
     * 目标字段名
     */
    private String targetColumn;
}
