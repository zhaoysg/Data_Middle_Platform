package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据质量方案-规则关联实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "dqc_plan_rule", excludeProperty = {"createBy", "updateBy", "createDept"})
public class DqcPlanRule extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 方案ID
     */
    private Long planId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
