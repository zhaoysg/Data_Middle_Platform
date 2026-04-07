package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.GovGlossaryMapping;

import java.io.Serial;
import java.io.Serializable;

/**
 * 治理 Glossary 映射业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = GovGlossaryMapping.class, reverseConvertGenerate = false)
public class GovGlossaryMappingBo extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 术语ID
     */
    @NotNull(message = "术语ID不能为空")
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
    @NotNull(message = "数据源ID不能为空")
    private Long dsId;

    /**
     * 表名
     */
    @NotNull(message = "表名不能为空")
    @Size(max = 200, message = "表名长度不能超过{max}个字符")
    private String tableName;

    /**
     * 列名
     */
    @Size(max = 200, message = "列名长度不能超过{max}个字符")
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
}
