package org.dromara.metadata.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.vo.DqcRuleDefVo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 鏁版嵁璐ㄩ噺瑙勫垯瀹氫箟Mapper鎺ュ彛
 */
@DS("bigdata")
public interface DqcRuleDefMapper extends BaseMapperPlus<DqcRuleDef, DqcRuleDefVo> {

    @InterceptorIgnore(dataPermission = "true", tenantLine = "true")
    @Select("""
        SELECT COUNT(1)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'dqc_rule_def'
          AND COLUMN_NAME = #{columnName}
        """)
    long countColumn(String columnName);

    default boolean hasColumn(String columnName) {
        return TenantHelper.ignore(() -> countColumn(columnName) > 0);
    }

    default boolean supportsCompareMetadataColumns() {
        return hasColumn("compare_table_id") && hasColumn("compare_column_id");
    }

    default QueryWrapper<DqcRuleDef> compatibleSelect(QueryWrapper<DqcRuleDef> wrapper) {
        List<String> columns = new ArrayList<>(List.of(
            "id",
            "rule_name",
            "rule_code",
            "template_id",
            "table_id",
            "column_id",
            "rule_type",
            "apply_level",
            "dimensions",
            "rule_expr",
            "threshold_min",
            "threshold_max",
            "fluctuation_threshold",
            "regex_pattern",
            "error_level",
            "rule_strength",
            "alert_receivers",
            "sort_order",
            "enabled",
            "del_flag",
            "tenant_id",
            "create_time",
            "update_time"
        ));
        if (supportsCompareMetadataColumns()) {
            columns.add("compare_table_id");
            columns.add("compare_column_id");
        }
        wrapper.select(columns.toArray(String[]::new));
        return wrapper;
    }

    default DqcRuleDef selectCompatibleById(Long id) {
        if (id == null) {
            return null;
        }
        QueryWrapper<DqcRuleDef> wrapper = compatibleSelect(new QueryWrapper<>());
        wrapper.eq("id", id);
        return selectOne(wrapper);
    }

    default List<DqcRuleDef> selectCompatibleBatchIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        QueryWrapper<DqcRuleDef> wrapper = compatibleSelect(new QueryWrapper<>());
        wrapper.in("id", ids);
        return selectList(wrapper);
    }
}
