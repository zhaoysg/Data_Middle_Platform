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
     * 脱敏表达式/SQL 片段（执行层，可选）
     */
    private String maskExpr;

    /**
     * 掩码字符，默认 *
     */
    private String maskChar;

    /**
     * 遮蔽位置：ALL/HEAD/TAIL/CENTER
     */
    private String maskPosition;

    /**
     * 掩码时保留头部字符数
     */
    private Integer maskHeadKeep;

    /**
     * 掩码时保留尾部字符数
     */
    private Integer maskTailKeep;

    /**
     * 掩码正则或高级替换说明（与 bgdata maskPattern 对齐）
     */
    private String maskPattern;

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
