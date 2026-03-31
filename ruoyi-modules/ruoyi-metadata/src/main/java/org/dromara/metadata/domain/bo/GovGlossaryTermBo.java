package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.GovGlossaryTerm;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治理 Glossary 术语业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = GovGlossaryTerm.class, reverseConvertGenerate = false)
public class GovGlossaryTermBo extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 术语名称
     */
    @NotBlank(message = "术语名称不能为空")
    @Size(max = 100, message = "术语名称长度不能超过{max}个字符")
    private String termName;

    /**
     * 术语别名
     */
    @Size(max = 200, message = "术语别名长度不能超过{max}个字符")
    private String termAlias;

    /**
     * 术语描述
     */
    @Size(max = 500, message = "术语描述长度不能超过{max}个字符")
    private String termDesc;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 业务负责人
     */
    private Long bizOwner;

    /**
     * 技术负责人
     */
    private Long techOwner;

    /**
     * 状态：DRAFT/PUBLISHED/DEPRECATED
     */
    private String status;

    /**
     * 标签ID列表
     */
    private String tagIds;

    /**
     * 来源
     */
    private String source;

    /**
     * 备注
     */
    private String remark;

    /**
     * 关键词搜索
     */
    private String keyword;
}
