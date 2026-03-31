package org.dromara.metadata.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.metadata.domain.GovGlossaryTerm;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治理 Glossary 术语视图对象
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = GovGlossaryTerm.class)
public class GovGlossaryTermVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 术语名称
     */
    private String termName;

    /**
     * 术语别名
     */
    private String termAlias;

    /**
     * 术语描述
     */
    private String termDesc;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 业务负责人
     */
    private Long bizOwner;

    /**
     * 业务负责人名称
     */
    private String bizOwnerName;

    /**
     * 技术负责人
     */
    private Long techOwner;

    /**
     * 技术负责人名称
     */
    private String techOwnerName;

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
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

    /**
     * 更新时间
     */
    private java.time.LocalDateTime updateTime;
}
