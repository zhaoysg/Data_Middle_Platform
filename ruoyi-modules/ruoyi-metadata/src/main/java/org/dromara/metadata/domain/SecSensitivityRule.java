package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 敏感识别规则实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sec_sensitivity_rule")
public class SecSensitivityRule extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 识别类型 REGEX/COLUMN_NAME/DATA_TYPE/CUSTOM
     */
    private String ruleType;

    /**
     * 匹配表达式 JSON格式
     */
    private String ruleExpr;

    /**
     * 关联的敏感等级
     */
    private String targetLevelCode;

    /**
     * 关联的数据分类
     */
    private String targetClassCode;

    /**
     * 是否启用：0否 1是
     */
    private String enabled;

    /**
     * 是否内置规则：0否 1是
     */
    private String builtin;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
