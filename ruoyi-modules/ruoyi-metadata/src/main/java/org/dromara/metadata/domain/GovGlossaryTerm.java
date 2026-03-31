package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治理 Glossary 术语实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("gov_glossary_term")
public class GovGlossaryTerm extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
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
     * 标签ID列表，逗号分隔
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
     * 删除标志
     */
    @TableLogic
    private String delFlag;
}
