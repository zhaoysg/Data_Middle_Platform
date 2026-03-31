package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.MetadataColumn;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 元数据字段视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = MetadataColumn.class)
public class MetadataColumnVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tableId;
    private Long dsId;
    private String tableName;
    private String columnName;
    private String columnAlias;
    private String columnComment;
    private String dataType;
    private String isNullable;
    private String columnKey;
    private String defaultValue;
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private String fkReference;
    private Boolean isSensitive;
    private String sensitivityLevel;
    /** 字段排序（兼容前端 sortOrder） */
    private Integer sort;
    /** 字段排序（前端引用别名） */
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String createBy;
    private String updateBy;
}
