package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.GovGlossaryCategory;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治理 Glossary 分类业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = GovGlossaryCategory.class, reverseConvertGenerate = false)
public class GovGlossaryCategoryBo extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过{max}个字符")
    private String categoryName;

    /**
     * 分类编码
     */
    @Size(max = 50, message = "分类编码长度不能超过{max}个字符")
    private String categoryCode;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态：0=正常 1=停用
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
