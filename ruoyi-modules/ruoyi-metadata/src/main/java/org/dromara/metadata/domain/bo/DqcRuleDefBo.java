package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.DqcRuleDef;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据质量规则定义业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DqcRuleDef.class, reverseConvertGenerate = false)
public class DqcRuleDefBo extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称长度不能超过{max}个字符")
    private String ruleName;

    /**
     * 规则编码
     */
    @NotBlank(message = "规则编码不能为空")
    @Size(max = 50, message = "规则编码长度不能超过{max}个字符")
    private String ruleCode;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 应用层级
     */
    private String applyLevel;

    /**
     * 质量维度
     */
    private String dimensions;

    /**
     * 兼容旧前端的单值维度字段
     */
    private String dimension;

    /**
     * 规则表达式/SQL
     */
    private String ruleExpr;

    /**
     * 兼容旧前端的规则参数JSON
     */
    private String ruleParams;

    /**
     * 目标数据源ID
     */
    private Long targetDsId;

    /**
     * 目标表名
     */
    private String targetTable;

    /**
     * 目标列名
     */
    private String targetColumn;

    /**
     * 对比数据源ID
     */
    private Long compareDsId;

    /**
     * 对比表名
     */
    private String compareTable;

    /**
     * 对比列名
     */
    private String compareColumn;

    /**
     * 阈值最小值
     */
    private BigDecimal thresholdMin;

    /**
     * 阈值最大值
     */
    private BigDecimal thresholdMax;

    /**
     * 波动阈值百分比
     */
    private BigDecimal fluctuationThreshold;

    /**
     * 正则表达式
     */
    private String regexPattern;

    /**
     * 错误级别：LOW/MEDIUM/HIGH/CRITICAL
     */
    private String errorLevel;

    /**
     * 告警通知人
     */
    private String alertReceivers;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private String enabled;
}
