package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 敏感等级实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sec_level")
public class SecLevel extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 敏感等级编码
     */
    private String levelCode;

    /**
     * 敏感等级名称
     */
    private String levelName;

    /**
     * 等级值 1-4
     */
    private Integer levelValue;

    /**
     * 等级描述
     */
    private String levelDesc;

    /**
     * 前端展示颜色
     */
    private String color;

    /**
     * 排序
     */
    private Integer sortOrder;

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
