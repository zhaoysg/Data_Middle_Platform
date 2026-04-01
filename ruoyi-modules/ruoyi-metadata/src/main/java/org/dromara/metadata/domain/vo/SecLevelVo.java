package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.SecLevel;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 敏感等级视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SecLevel.class)
public class SecLevelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String levelCode;

    private String levelName;

    private Integer levelValue;

    private String levelDesc;

    private String color;

    private Integer sortOrder;

    private String enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
