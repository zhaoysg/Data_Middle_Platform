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
 * 数据质量规则定义实体
 * <p>
 * 采用纯元数据驱动设计：
 * - 目标表/字段：通过 tableId、columnId 关联 metadata_table、metadata_column
 * - 对比表/字段：通过 compareTableId、compareColumnId 关联
 * - 数据源信息：通过元数据表间接获取（tableId → metadata_table.ds_id）
 * <p>
 * 执行时通过 ID 动态查询元数据获取真实表名、字段名、数据源信息
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "dqc_rule_def", excludeProperty = {"createBy", "updateBy", "createDept"})
public class DqcRuleDef extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则编码
     */
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
     * 字段级规则必填，表级规则可为空
     */
    private Long columnId;

    /**
     * 对比表元数据ID (metadata_table.id)
     * 跨表/跨字段规则需要填写
     */
    private Long compareTableId;

    /**
     * 对比字段元数据ID (metadata_column.id)
     * 跨字段规则需要填写
     */
    private Long compareColumnId;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 应用层级：TABLE / COLUMN / CROSS_FIELD / CROSS_TABLE
     */
    private String applyLevel;

    /**
     * 质量维度
     */
    private String dimensions;

    /**
     * 规则表达式/SQL（模板规则可为空）
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
     * 是否启用：0-否，1-是
     */
    private String enabled;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
