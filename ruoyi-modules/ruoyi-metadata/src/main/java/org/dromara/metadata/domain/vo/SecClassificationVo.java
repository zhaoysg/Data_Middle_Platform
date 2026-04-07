package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.SecClassification;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据分类视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SecClassification.class)
public class SecClassificationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String classCode;

    private String className;

    private String classDesc;

    private Integer sortOrder;

    /**
     * 默认关联敏感等级编码
     */
    private String defaultLevelCode;

    /**
     * 等级名称（列表展示，非表字段）
     */
    private String defaultLevelName;

    /**
     * 等级颜色（列表 Tag，非表字段）
     */
    private String defaultLevelColor;

    /**
     * 指向该分类的敏感识别规则数量（聚合，非表字段）
     */
    private Long ruleCount;

    /**
     * 该分类下的敏感字段记录数量（聚合，非表字段）
     */
    private Long fieldCount;

    private String enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
