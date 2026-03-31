package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 元数据字段实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("metadata_column")
public class MetadataColumn extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long tableId;
    private Long dsId;
    private String tableName;
    private String columnName;
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
    @TableLogic
    private String delFlag;

    public MetadataColumn(Long id) {
        this.id = id;
    }
}
