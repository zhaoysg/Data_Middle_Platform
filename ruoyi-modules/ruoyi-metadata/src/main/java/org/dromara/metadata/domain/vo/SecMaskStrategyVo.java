package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.SecMaskStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 脱敏策略视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SecMaskStrategy.class)
public class SecMaskStrategyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String strategyCode;

    private String strategyName;

    private String strategyDesc;

    private Long dsId;

    private String enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
