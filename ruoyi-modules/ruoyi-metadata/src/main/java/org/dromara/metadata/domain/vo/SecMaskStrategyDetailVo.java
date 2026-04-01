package org.dromara.metadata.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 脱敏策略明细视图对象
 */
@Data
public class SecMaskStrategyDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long strategyId;

    private Long dsId;

    private String tableName;

    private String columnName;

    private String templateCode;

    private String outputAlias;
}
