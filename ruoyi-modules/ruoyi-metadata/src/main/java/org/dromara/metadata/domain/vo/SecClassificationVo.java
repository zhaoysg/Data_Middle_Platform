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

    private String enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
