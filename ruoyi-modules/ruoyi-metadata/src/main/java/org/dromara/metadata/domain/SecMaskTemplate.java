package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 脱敏模板实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sec_mask_template", excludeProperty = {"createBy", "updateBy", "createDept"})
public class SecMaskTemplate extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
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
     * 脱敏类型 ENCRYPT/MASK/HIDE/DELETE/SHUFFLE/CUSTOM
     */
    private String templateType;

    /**
     * 脱敏表达式 如 MASK(name, '*', 3, 4)
     */
    private String maskExpr;

    /**
     * 模板描述
     */
    private String templateDesc;

    /**
     * 是否内置：0否 1是
     */
    private String builtin;

    /**
     * 是否启用：0否 1是
     */
    private String enabled;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
