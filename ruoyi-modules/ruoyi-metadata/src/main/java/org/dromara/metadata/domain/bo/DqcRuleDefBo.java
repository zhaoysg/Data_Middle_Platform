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
 * <p>
 * 采用纯元数据驱动设计：
 * - 目标表/字段：通过 tableId、columnId 选择
 * - 对比表/字段：通过 compareTableId、compareColumnId 选择
 * <p>
 * 数据源信息通过元数据间接获取，无需手动填写
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
     * 模板ID (dqc_rule_template.id)
     */
    private Long templateId;

    /**
     * 目标表元数据ID (metadata_table.id)
     */
    private Long tableId;

    /**
     * 目标字段元数据ID (metadata_column.id)
     */
    private Long columnId;

    /**
     * 对比表元数据ID (metadata_table.id)
     */
    private Long compareTableId;

    /**
     * 对比字段元数据ID (metadata_column.id)
     */
    private Long compareColumnId;

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
     * 规则表达式/SQL
     */
    private String ruleExpr;

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
     * 错误级别：LOW / MEDIUM / HIGH / CRITICAL
     */
    private String errorLevel;

    /**
     * 规则强度：STRONG / WEAK
     */
    private String ruleStrength;

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