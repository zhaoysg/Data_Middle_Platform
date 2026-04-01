package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.SecSensitivityRule;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 敏感识别规则视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SecSensitivityRule.class)
public class SecSensitivityRuleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String ruleCode;

    private String ruleName;

    private String ruleType;

    private String ruleExpr;

    private String targetLevelCode;

    private String targetClassCode;

    private String enabled;

    private String builtin;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
