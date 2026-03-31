package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.MetadataCatalog;

/**
 * 资产目录业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MetadataCatalog.class, reverseConvertGenerate = false)
public class MetadataCatalogBo extends BaseEntity {

    private Long id;

    @NotBlank(message = "目录名称不能为空")
    @Size(max = 100, message = "目录名称长度不能超过{max}个字符")
    private String catalogName;

    @Size(max = 50, message = "目录编码长度不能超过{max}个字符")
    private String catalogCode;

    @NotBlank(message = "目录类型不能为空")
    private String catalogType;

    private Long parentId;
    private Integer sortOrder;
    private String status;
    private String remark;

    public MetadataCatalogBo(Long id) {
        this.id = id;
    }
}
