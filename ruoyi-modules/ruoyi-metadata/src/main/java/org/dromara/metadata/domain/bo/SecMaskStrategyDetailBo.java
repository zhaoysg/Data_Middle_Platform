package org.dromara.metadata.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 脱敏策略明细业务对象
 */
@Data
public class SecMaskStrategyDetailBo implements Serializable {

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
