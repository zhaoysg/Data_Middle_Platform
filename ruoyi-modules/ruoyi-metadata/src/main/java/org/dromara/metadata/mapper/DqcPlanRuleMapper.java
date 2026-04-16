package org.dromara.metadata.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.metadata.domain.DqcPlanRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据质量方案-规则关联Mapper接口
 */
@DS("bigdata")
public interface DqcPlanRuleMapper extends BaseMapperPlus<DqcPlanRule, DqcPlanRule> {

    @InterceptorIgnore(dataPermission = "true", tenantLine = "true")
    @Select("""
        SELECT COUNT(1)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'dqc_plan_rule'
          AND COLUMN_NAME = #{columnName}
        """)
    long countColumn(String columnName);

    default boolean hasColumn(String columnName) {
        return TenantHelper.ignore(() -> countColumn(columnName) > 0);
    }

    default QueryWrapper<DqcPlanRule> compatibleSelect(QueryWrapper<DqcPlanRule> wrapper) {
        boolean hasTargetTable = hasColumn("target_table");
        boolean hasTargetColumn = hasColumn("target_column");
        boolean hasEnabled = hasColumn("enabled");
        boolean hasSkipOnFailure = hasColumn("skip_on_failure");
        boolean hasUpdateTime = hasColumn("update_time");
        List<String> columns = new ArrayList<>(List.of(
            "id",
            "plan_id",
            "rule_id",
            "sort_order",
            "del_flag",
            "create_time"
        ));
        if (hasTargetTable) {
            columns.add(3, "target_table");
        }
        if (hasTargetColumn) {
            columns.add(hasTargetTable ? 4 : 3, "target_column");
        }
        if (hasEnabled) {
            columns.add("enabled");
        }
        if (hasSkipOnFailure) {
            columns.add("skip_on_failure");
        }
        if (hasUpdateTime) {
            columns.add("update_time");
        }
        wrapper.select(columns.toArray(String[]::new));
        return wrapper;
    }

    default List<DqcPlanRule> selectCompatibleByPlanId(Long planId) {
        if (planId == null) {
            return List.of();
        }
        QueryWrapper<DqcPlanRule> wrapper = compatibleSelect(new QueryWrapper<>());
        wrapper.eq("plan_id", planId).orderByAsc("sort_order");
        return selectList(wrapper);
    }
}
