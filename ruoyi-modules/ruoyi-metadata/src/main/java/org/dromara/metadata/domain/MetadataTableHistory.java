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
 * 元数据表历史快照实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("metadata_table_history")
public class MetadataTableHistory extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 表ID
     */
    private Long tableId;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 快照时间
     */
    private LocalDateTime snapshotTime;

    /**
     * 行数
     */
    private Long rowCount;

    /**
     * 列数
     */
    private Integer columnCount;

    /**
     * 列信息哈希
     */
    private String columnHash;

    /**
     * 变更类型：ADD/DROP/MODIFY/UNCHANGED
     */
    private String changeType;

    /**
     * 变更详情
     */
    private String changeDetail;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
