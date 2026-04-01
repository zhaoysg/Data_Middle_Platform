package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字段敏感记录实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sec_column_sensitivity")
public class SecColumnSensitivity extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 字段名
     */
    private String columnName;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 敏感等级
     */
    private String levelCode;

    /**
     * 数据分类
     */
    private String classCode;

    /**
     * 识别方式 AUTO/MANUAL
     */
    private String identifiedBy;

    /**
     * 扫描任务ID
     */
    private Long scanTaskId;

    /**
     * 是否确认：0待确认 1已确认
     */
    private String confirmed;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
