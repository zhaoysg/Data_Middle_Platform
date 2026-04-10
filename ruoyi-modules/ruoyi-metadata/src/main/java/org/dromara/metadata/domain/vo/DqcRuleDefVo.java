package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.DqcRuleDef;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据质量规则定义视图对象
 * <p>
 * 用于前端展示，字段信息通过元数据 JOIN 查询获取
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DqcRuleDef.class)
public class DqcRuleDefVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
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
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称（用于展示）
     */
    private String templateName;

    // ===== 元数据关联字段 =====

    /**
     * 目标表元数据ID (metadata_table.id)
     */
    private Long tableId;

    /**
     * 目标表名称（通过元数据 JOIN 获取）
     */
    private String tableName;

    /**
     * 目标字段元数据ID (metadata_column.id)
     */
    private Long columnId;

    /**
     * 目标字段名称（通过元数据 JOIN 获取）
     */
    private String columnName;

    /**
     * 对比表元数据ID (metadata_table.id)
     */
    private Long compareTableId;

    /**
     * 对比表名称（通过元数据 JOIN 获取）
     */
    private String compareTableName;

    /**
     * 对比字段元数据ID (metadata_column.id)
     */
    private Long compareColumnId;

    /**
     * 对比字段名称（通过元数据 JOIN 获取）
     */
    private String compareColumnName;

    // ===== 数据源信息（通过元数据获取） =====

    /**
     * 数据源ID（通过 tableId → metadata_table.ds_id 获取）
     */
    private Long dsId;

    /**
     * 数据源名称（用于展示）
     */
    private String dsName;

    // ===== 规则配置字段 =====

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

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

    /**
     * 更新时间
     */
    private java.time.LocalDateTime updateTime;
}
