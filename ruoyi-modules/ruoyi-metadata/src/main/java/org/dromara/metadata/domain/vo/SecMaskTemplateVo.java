package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.SecMaskTemplate;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 脱敏模板视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SecMaskTemplate.class)
public class SecMaskTemplateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String templateCode;

    private String templateName;

    private String templateType;

    private String maskExpr;

    private String templateDesc;

    private String builtin;

    private String enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
