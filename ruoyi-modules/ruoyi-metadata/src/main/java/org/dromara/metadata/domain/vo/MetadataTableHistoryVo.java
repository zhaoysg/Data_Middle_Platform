package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.MetadataTableHistory;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 元数据表历史快照视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = MetadataTableHistory.class)
public class MetadataTableHistoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
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
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 更新者
     */
    private String updateBy;
}
