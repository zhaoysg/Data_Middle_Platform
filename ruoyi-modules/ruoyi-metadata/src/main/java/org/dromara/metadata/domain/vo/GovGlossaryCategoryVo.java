package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 治理 Glossary 分类视图对象
 */
@Data
@ExcelIgnoreUnannotated
@EqualsAndHashCode(callSuper = true)
public class GovGlossaryCategoryVo extends org.dromara.common.mybatis.core.domain.BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long parentId;
    private String categoryName;
    private String categoryCode;
    private Integer sortOrder;
    private String status;
    private String remark;

    /**
     * 子分类列表
     */
    private List<GovGlossaryCategoryVo> children;
}
