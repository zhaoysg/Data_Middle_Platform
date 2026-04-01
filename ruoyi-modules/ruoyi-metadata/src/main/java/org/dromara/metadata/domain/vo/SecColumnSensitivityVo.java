package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.SecColumnSensitivity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字段敏感记录视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SecColumnSensitivity.class)
public class SecColumnSensitivityVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long dsId;

    private String tableName;

    private String columnName;

    private String dataType;

    private String levelCode;

    private String classCode;

    private String identifiedBy;

    private Long scanTaskId;

    private String confirmed;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
