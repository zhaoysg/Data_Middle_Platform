package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.DqcRuleTemplate;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据质量规则模板视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = DqcRuleTemplate.class)
public class DqcRuleTemplateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板描述
     */
    private String templateDesc;

    /**
     * 规则类型：NULL_CHECK/UNIQUE/REGEX/THRESHOLD/FLUCTUATION/SQL/CUSTOM
     */
    private String ruleType;

    /**
     * 应用层级：TABLE/COLUMN/CROSS_FIELD/CROSS_TABLE
     */
    private String applyLevel;

    /**
     * 默认表达式
     */
    private String defaultExpr;

    /**
     * 阈值JSON
     */
    private String thresholdJson;

    /**
     * 参数规格JSON
     */
    private String paramSpec;

    /**
     * 质量维度：COMPLETENESS/UNIQUENESS/ACCURACY/CONSISTENCY/VALIDITY/TIMELINESS/CUSTOM
     */
    private String dimension;

    /**
     * 是否内置：1=内置
     */
    private String builtin;

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
