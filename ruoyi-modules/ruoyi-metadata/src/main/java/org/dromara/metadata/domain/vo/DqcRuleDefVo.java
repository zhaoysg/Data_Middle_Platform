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
     * 目标数据源ID
     */
    private Long targetDsId;

    /**
     * 目标数据源名称
     */
    private String targetDsName;

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

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

    /**
     * 更新时间
     */
    private java.time.LocalDateTime updateTime;
}
