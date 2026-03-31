package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Insert;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.domain.vo.MetadataTableVo;

import java.util.List;

/**
 * 元数据表Mapper接口
 */
@DS("bigdata")
public interface MetadataTableMapper extends BaseMapperPlus<MetadataTable, MetadataTableVo> {

    @Insert("""
        INSERT INTO metadata_table (
            tenant_id, ds_id, ds_name, ds_code, table_name, table_alias, table_comment, table_type,
            data_layer, data_domain, row_count, storage_bytes, source_update_time, sensitivity_level,
            owner_id, dept_id, catalog_id, tags, last_scan_time, status, del_flag, create_dept,
            create_by, create_time, update_by, update_time
        ) VALUES (
            #{tenantId}, #{dsId}, #{dsName}, #{dsCode}, #{tableName}, #{tableAlias}, #{tableComment}, #{tableType},
            #{dataLayer}, #{dataDomain}, #{rowCount}, #{storageBytes}, #{sourceUpdateTime}, #{sensitivityLevel},
            #{ownerId}, #{deptId}, #{catalogId}, #{tags}, #{lastScanTime}, #{status}, #{delFlag}, #{createDept},
            #{createBy}, #{createTime}, #{updateBy}, #{updateTime}
        )
        ON DUPLICATE KEY UPDATE
            id = LAST_INSERT_ID(id),
            ds_name = VALUES(ds_name),
            ds_code = VALUES(ds_code),
            table_alias = COALESCE(VALUES(table_alias), table_alias),
            table_comment = VALUES(table_comment),
            table_type = VALUES(table_type),
            data_layer = COALESCE(VALUES(data_layer), data_layer),
            data_domain = COALESCE(VALUES(data_domain), data_domain),
            row_count = VALUES(row_count),
            storage_bytes = COALESCE(VALUES(storage_bytes), storage_bytes),
            source_update_time = COALESCE(VALUES(source_update_time), source_update_time),
            sensitivity_level = COALESCE(VALUES(sensitivity_level), sensitivity_level),
            owner_id = COALESCE(VALUES(owner_id), owner_id),
            dept_id = COALESCE(VALUES(dept_id), dept_id),
            catalog_id = COALESCE(VALUES(catalog_id), catalog_id),
            tags = COALESCE(VALUES(tags), tags),
            last_scan_time = VALUES(last_scan_time),
            status = VALUES(status),
            del_flag = '0',
            create_dept = COALESCE(VALUES(create_dept), create_dept),
            update_by = COALESCE(VALUES(update_by), update_by),
            update_time = COALESCE(VALUES(update_time), NOW())
        """)
    int upsert(MetadataTable table);

    /**
     * 根据数据源ID和表名查询
     */
    default MetadataTable selectByDsIdAndTableName(Long dsId, String tableName) {
        return this.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetadataTable>()
                .eq(MetadataTable::getDsId, dsId)
                .eq(MetadataTable::getTableName, tableName)
        );
    }

    /**
     * 批量根据数据源ID和表名查询
     */
    default List<MetadataTable> selectByDsIdAndTableNames(Long dsId, List<String> tableNames) {
        return this.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetadataTable>()
                .eq(MetadataTable::getDsId, dsId)
                .in(MetadataTable::getTableName, tableNames)
        );
    }

    default MetadataTable selectByTenantDsIdAndTableName(String tenantId, Long dsId, String tableName) {
        return this.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetadataTable>()
                .eq(MetadataTable::getTenantId, tenantId)
                .eq(MetadataTable::getDsId, dsId)
                .eq(MetadataTable::getTableName, tableName)
        );
    }
}
