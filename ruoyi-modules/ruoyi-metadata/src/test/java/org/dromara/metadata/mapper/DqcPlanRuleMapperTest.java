package org.dromara.metadata.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.dromara.metadata.domain.DqcPlanRule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("local")
class DqcPlanRuleMapperTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private DqcPlanRuleMapper mapper;

    @Test
    void compatibleSelectShouldSkipOptionalColumnsWhenSchemaIsLegacy() {
        when(mapper.countColumn("target_table")).thenReturn(0L);
        when(mapper.countColumn("target_column")).thenReturn(0L);
        when(mapper.countColumn("enabled")).thenReturn(0L);
        when(mapper.countColumn("skip_on_failure")).thenReturn(0L);
        when(mapper.countColumn("update_time")).thenReturn(0L);

        QueryWrapper<DqcPlanRule> wrapper = mapper.compatibleSelect(new QueryWrapper<>());
        String sqlSelect = wrapper.getSqlSelect();

        assertTrue(sqlSelect.contains("plan_id"));
        assertTrue(sqlSelect.contains("rule_id"));
        assertFalse(sqlSelect.contains("target_table"));
        assertFalse(sqlSelect.contains("target_column"));
        assertFalse(sqlSelect.contains("enabled"));
        assertFalse(sqlSelect.contains("skip_on_failure"));
        assertFalse(sqlSelect.contains("update_time"));
    }

    @Test
    void compatibleSelectShouldIncludeOptionalColumnsWhenSchemaIsUpToDate() {
        when(mapper.countColumn("target_table")).thenReturn(1L);
        when(mapper.countColumn("target_column")).thenReturn(1L);
        when(mapper.countColumn("enabled")).thenReturn(1L);
        when(mapper.countColumn("skip_on_failure")).thenReturn(1L);
        when(mapper.countColumn("update_time")).thenReturn(1L);

        QueryWrapper<DqcPlanRule> wrapper = mapper.compatibleSelect(new QueryWrapper<>());
        String sqlSelect = wrapper.getSqlSelect();

        assertTrue(sqlSelect.contains("target_table"));
        assertTrue(sqlSelect.contains("target_column"));
        assertTrue(sqlSelect.contains("enabled"));
        assertTrue(sqlSelect.contains("skip_on_failure"));
        assertTrue(sqlSelect.contains("update_time"));
    }
}
