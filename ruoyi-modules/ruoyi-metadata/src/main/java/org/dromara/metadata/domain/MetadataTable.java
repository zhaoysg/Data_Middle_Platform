package org.dromara.metadata.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * 元数据表实体
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("metadata_table")
public class MetadataTable extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long dsId;
    private String dsName;
    private String dsCode;
    private String tableName;
    private String tableAlias;
    private String tableComment;
    private String tableType;
    private String dataLayer;
    private String dataDomain;
    private Long rowCount;
    private Long storageBytes;
    private java.time.LocalDateTime sourceUpdateTime;
    private String sensitivityLevel;
    private Long ownerId;
    private Long deptId;
    private Long catalogId;
    private String tags;
    private java.time.LocalDateTime lastScanTime;
    private String status;
    @TableLogic
    private String delFlag;

    public MetadataTable(Long id) {
        this.id = id;
    }
}
