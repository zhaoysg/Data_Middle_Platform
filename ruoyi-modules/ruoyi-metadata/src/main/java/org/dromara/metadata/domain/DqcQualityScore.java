package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 数据质量评分实体
 * <p>
 * 按 数据源+表+日期 聚合，记录 6 个维度的质量得分及综合得分。
 * 由 DQC 执行完成后汇总写入，Track A/B/C 均可写入各自模块的评分。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("dqc_quality_score")
public class DqcQualityScore extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 质检日期
     */
    private LocalDate checkDate;

    /**
     * 目标数据源ID
     */
    private Long targetDsId;

    /**
     * 目标表名
     */
    private String targetTable;

    /**
     * 数据层编码：ODS/DWD/DWS/ADS
     */
    private String layerCode;

    /**
     * 质检执行ID
     */
    private Long executionId;

    /**
     * 质检方案ID
     */
    private Long planId;

    /**
     * 质检方案名称
     */
    private String planName;

    /**
     * 完整性得分（0-100）
     */
    private BigDecimal completenessScore;

    /**
     * 唯一性得分（0-100）
     */
    private BigDecimal uniquenessScore;

    /**
     * 准确性得分（0-100）
     */
    private BigDecimal accuracyScore;

    /**
     * 一致性得分（0-100）
     */
    private BigDecimal consistencyScore;

    /**
     * 及时性得分（0-100）
     */
    private BigDecimal timelinessScore;

    /**
     * 有效性得分（0-100）
     */
    private BigDecimal validityScore;

    /**
     * 综合得分（0-100）
     */
    private BigDecimal overallScore;

    /**
     * 规则通过率（0-100）
     */
    private BigDecimal rulePassRate;

    /**
     * 规则总数
     */
    private Integer ruleTotalCount;

    /**
     * 规则通过数
     */
    private Integer rulePassCount;

    /**
     * 规则失败数
     */
    private Integer ruleFailCount;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
