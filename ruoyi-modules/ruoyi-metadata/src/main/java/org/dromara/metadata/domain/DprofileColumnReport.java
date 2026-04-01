package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 列级探查报告实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("dprofile_column_report")
public class DprofileColumnReport extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 报告ID
     */
    private Long reportId;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 列名
     */
    private String columnName;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 列注释
     */
    private String columnComment;

    /**
     * 是否可空
     */
    private String nullable;

    /**
     * 是否主键
     */
    private String isPrimaryKey;

    /**
     * 总记录数
     */
    private Long totalCount;

    /**
     * 空值数
     */
    private Long nullCount;

    /**
     * 空值率
     */
    private BigDecimal nullRate;

    /**
     * 唯一值数量
     */
    private Long uniqueCount;

    /**
     * 唯一值比例
     */
    private BigDecimal uniqueRate;

    /**
     * 样本值（JSON数组）
     */
    private String sampleValues;

    /**
     * 高频值分布（JSON）
     */
    private String topValues;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
