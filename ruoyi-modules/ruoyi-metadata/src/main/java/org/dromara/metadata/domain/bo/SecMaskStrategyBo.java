package org.dromara.metadata.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 脱敏策略业务对象（包含明细）
 */
@Data
public class SecMaskStrategyBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String strategyCode;

    private String strategyName;

    private String strategyDesc;

    private Long dsId;

    private String enabled;

    private List<SecMaskStrategyDetailBo> details;
}
