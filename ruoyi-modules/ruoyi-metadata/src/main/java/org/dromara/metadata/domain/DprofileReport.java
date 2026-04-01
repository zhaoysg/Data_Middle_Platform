package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据探查报告实体（表级）
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("dprofile_report")
public class DprofileReport extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 探查任务ID
     */
    private Long taskId;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 快照ID
     */
    private Long snapshotId;

    /**
     * 行数
     */
    private Long rowCount;

    /**
     * 列数
     */
    private Integer columnCount;

    /**
     * 数据大小字节
     */
    private Long dataSizeBytes;

    /**
     * 表注释
     */
    private String storageComment;

    /**
     * 表最后更新时间
     */
    private LocalDateTime lastModified;

    /**
     * 探查结果JSON
     */
    private String profileData;

    /**
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
