package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据质量执行明细实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("dqc_execution_detail")
public class DqcExecutionDetail extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 执行记录ID
     */
    private Long executionId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 质量维度
     */
    private String dimension;

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
     * 实际值（字符串）
     */
    private String actualValue;

    /**
     * 阈值值（字符串）
     */
    private String thresholdValue;

    /**
     * 结果数值
     */
    private BigDecimal resultValue;

    /**
     * 通过标志：1=通过 0=失败
     */
    private String passFlag;

    /**
     * 错误级别
     */
    private String errorLevel;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 执行的SQL
     */
    private String executeSql;

    /**
     * 执行时间
     */
    private LocalDateTime executeTime;

    /**
     * 执行耗时毫秒
     */
    private Long elapsedMs;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
