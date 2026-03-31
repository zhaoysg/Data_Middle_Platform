package org.dromara.metadata.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Insert;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.metadata.domain.MetadataColumn;
import org.dromara.metadata.domain.vo.MetadataColumnVo;

import java.util.List;

/**
 * 元数据字段Mapper接口
 */
@DS("bigdata")
public interface MetadataColumnMapper extends BaseMapperPlus<MetadataColumn, MetadataColumnVo> {

    @Insert("""
        INSERT INTO metadata_column (
            tenant_id, table_id, ds_id, table_name, column_name, column_alias, column_comment, data_type,
            is_nullable, column_key, default_value, is_primary_key, is_foreign_key, fk_reference,
            is_sensitive, sensitivity_level, sort_order, del_flag, create_dept, create_by,
            create_time, update_by, update_time
        ) VALUES (
            #{tenantId}, #{tableId}, #{dsId}, #{tableName}, #{columnName}, #{columnAlias}, #{columnComment}, #{dataType},
            #{isNullable}, #{columnKey}, #{defaultValue}, #{isPrimaryKey}, #{isForeignKey}, #{fkReference},
            #{isSensitive}, #{sensitivityLevel}, #{sortOrder}, #{delFlag}, #{createDept}, #{createBy},
            #{createTime}, #{updateBy}, #{updateTime}
        )
        ON DUPLICATE KEY UPDATE
            id = LAST_INSERT_ID(id),
            table_id = VALUES(table_id),
            table_name = VALUES(table_name),
            column_alias = COALESCE(VALUES(column_alias), column_alias),
            column_comment = VALUES(column_comment),
            data_type = VALUES(data_type),
            is_nullable = VALUES(is_nullable),
            column_key = COALESCE(VALUES(column_key), column_key),
            default_value = COALESCE(VALUES(default_value), default_value),
            is_primary_key = COALESCE(VALUES(is_primary_key), is_primary_key),
            is_foreign_key = COALESCE(VALUES(is_foreign_key), is_foreign_key),
            fk_reference = COALESCE(VALUES(fk_reference), fk_reference),
            is_sensitive = COALESCE(VALUES(is_sensitive), is_sensitive),
            sensitivity_level = COALESCE(VALUES(sensitivity_level), sensitivity_level),
            sort_order = VALUES(sort_order),
            del_flag = '0',
            create_dept = COALESCE(VALUES(create_dept), create_dept),
            update_by = COALESCE(VALUES(update_by), update_by),
            update_time = COALESCE(VALUES(update_time), NOW())
        """)
    int upsert(MetadataColumn column);

    /**
     * 根据表ID查询字段列表
     */
    default List<MetadataColumn> selectByTableId(Long tableId) {
        return this.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetadataColumn>()
                .eq(MetadataColumn::getTableId, tableId)
                .orderByAsc(MetadataColumn::getSortOrder)
        );
    }

    /**
     * 删除表的所有字段
     */
    default int deleteByTableId(Long tableId) {
        return this.delete(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetadataColumn>()
                .eq(MetadataColumn::getTableId, tableId)
        );
    }
}
