package org.dromara.metadata.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 列表页仅切换启用/停用状态时的入参（避免走完整 {@link DqcRuleDefBo} 校验）
 */
@Data
public class DqcRuleDefStatusBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "规则ID不能为空")
    private Long id;

    /** 0=启用 1=停用 */
    @NotBlank(message = "状态不能为空")
    private String enabled;
}
