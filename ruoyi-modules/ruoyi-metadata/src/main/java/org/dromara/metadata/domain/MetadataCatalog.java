package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 资产目录实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("metadata_catalog")
public class MetadataCatalog extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private String catalogName;
    private String catalogCode;
    private String catalogType;
    private Long parentId;
    @TableField("sort_order")
    private Integer sortOrder;
    private String status;
    private String remark;
    @TableLogic
    private String delFlag;

    public MetadataCatalog(Long id) {
        this.id = id;
    }
}
