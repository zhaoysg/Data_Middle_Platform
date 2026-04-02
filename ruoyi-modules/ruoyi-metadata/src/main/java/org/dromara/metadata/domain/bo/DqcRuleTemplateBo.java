package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.DqcRuleTemplate;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数据质量规则模板业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = DqcRuleTemplate.class, reverseConvertGenerate = false)
public class DqcRuleTemplateBo extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "模板编码不能为空")
    @Size(max = 50, message = "模板编码长度不能超过{max}个字符")
    private String templateCode;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过{max}个字符")
    private String templateName;

    @Size(max = 500, message = "模板描述长度不能超过{max}个字符")
    private String templateDesc;

    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    private String applyLevel;

    private String defaultExpr;

    private String thresholdJson;

    private String paramSpec;

    private String dimension;

    private String builtin;

    private String enabled;
}
