package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据分类实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sec_classification")
public class SecClassification extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 分类编码
     */
    private String classCode;

    /**
     * 分类名称
     */
    private String className;

    /**
     * 分类描述
     */
    private String classDesc;

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
