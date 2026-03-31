package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治理 Glossary 映射实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("gov_glossary_mapping")
public class GovGlossaryMapping extends TenantEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 术语ID
     */
    private Long termId;

    /**
     * 术语名称
     */
    private String termName;

    /**
     * 资产类型：TABLE/COLUMN
     */
    private String assetType;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 数据源ID
     */
    private Long dsId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 列名
     */
    private String columnName;

    /**
     * 映射类型：CONTAINS/DEFINES/REFERENCES
     */
    private String mappingType;

    /**
     * 置信度：0-100
     */
    private Integer confidence;

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
