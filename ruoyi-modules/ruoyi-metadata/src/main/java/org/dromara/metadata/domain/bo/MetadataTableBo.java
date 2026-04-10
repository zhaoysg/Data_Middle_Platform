package org.dromara.metadata.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.metadata.domain.MetadataTable;

import java.util.List;

/**
 * 元数据表业务对象
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = MetadataTable.class, reverseConvertGenerate = false)
public class MetadataTableBo extends BaseEntity {

    private Long id;

    private Long dsId;

    @NotBlank(message = "表名不能为空")
    @Size(max = 200, message = "表名长度不能超过{max}个字符")
    private String tableName;

    @Size(max = 200, message = "表别名长度不能超过{max}个字符")
    private String tableAlias;

    private String tableComment;
    private String tableType;
    private String dataLayer;
    private String dataDomain;
    private Long rowCount;
    private Long storageBytes;
    private String sensitivityLevel;
    private Long ownerId;
    private Long deptId;
    private Long catalogId;
    private String tags;
    private java.time.LocalDateTime lastScanTime;
    private String status;

    /** 关键字搜索 */
    private String keyword;

    /** 批量更新时的ID列表 */
    private List<Long> ids;

    public MetadataTableBo(Long id) {
        this.id = id;
    }
}
