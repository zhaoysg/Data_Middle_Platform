package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.MetadataColumn;

/**
 * 元数据字段业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MetadataColumn.class, reverseConvertGenerate = false)
public class MetadataColumnBo extends BaseEntity {

    private Long id;
    private Long tableId;
    private Long dsId;

    @NotBlank(message = "字段名不能为空")
    @Size(max = 100, message = "字段名长度不能超过{max}个字符")
    private String columnName;

    @Size(max = 200, message = "字段别名长度不能超过{max}个字符")
    private String columnAlias;

    private String columnComment;
    private String dataType;
    private String isNullable;
    private String columnKey;
    private String defaultValue;
    private Boolean isPrimaryKey;
    private Boolean isForeignKey;
    private String fkReference;
    private Boolean isSensitive;
    private String sensitivityLevel;
    private Integer sortOrder;

    /** 关键字搜索 */
    private String keyword;

    public MetadataColumnBo(Long id) {
        this.id = id;
    }
}
