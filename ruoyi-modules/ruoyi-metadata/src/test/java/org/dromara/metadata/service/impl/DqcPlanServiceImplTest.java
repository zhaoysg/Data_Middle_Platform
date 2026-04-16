package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import org.dromara.metadata.domain.DqcPlan;
import org.dromara.metadata.domain.DqcPlanRule;
import org.dromara.metadata.mapper.DqcPlanMapper;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.service.IDqcExecutionService;
import org.dromara.metadata.support.DatasourceHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("local")
class DqcPlanServiceImplTest {

    @Mock
    private DqcPlanMapper baseMapper;

    @Mock
    private DqcPlanRuleMapper planRuleMapper;

    @Mock
    private IDqcExecutionService executionService;

    @Mock
    private DatasourceHelper datasourceHelper;

    @InjectMocks
    private DqcPlanServiceImpl service;

    @Test
    void bindRulesShouldStayPinnedToBigdataWithinTransaction() throws NoSuchMethodException {
        DS classDs = DqcPlanServiceImpl.class.getAnnotation(DS.class);
        assertNotNull(classDs);
        assertEquals("bigdata", classDs.value());

        Method bindRules = DqcPlanServiceImpl.class.getMethod("bindRules", Long.class, List.class);
        DS methodDs = bindRules.getAnnotation(DS.class);
        assertNotNull(methodDs);
        assertEquals("bigdata", methodDs.value());
        assertNotNull(bindRules.getAnnotation(DSTransactional.class));
    }

    @Test
    void getBoundRulesShouldUseCompatibleMapperQuery() {
        DqcPlanRule rule = new DqcPlanRule();
        rule.setPlanId(1L);
        List<DqcPlanRule> expected = List.of(rule);
        when(planRuleMapper.selectCompatibleByPlanId(1L)).thenReturn(expected);

        List<DqcPlanRule> actual = service.getBoundRules(1L);

        assertSame(expected, actual);
        verify(planRuleMapper).selectCompatibleByPlanId(1L);
    }

    @Test
    void publishShouldValidatePlanBindingsWithCompatibleMapperQuery() {
        DqcPlan plan = new DqcPlan();
        plan.setId(1L);
        plan.setStatus("DRAFT");
        when(baseMapper.selectById(1L)).thenReturn(plan);
        when(datasourceHelper.listAccessibleDatasourceIds()).thenReturn(List.of(100L));
        when(planRuleMapper.selectCompatibleByPlanId(1L)).thenReturn(List.of());
        when(baseMapper.updateById(plan)).thenReturn(1);

        int updated = service.publish(1L);

        assertEquals(1, updated);
        verify(planRuleMapper).selectCompatibleByPlanId(1L);
        verify(baseMapper).updateById(plan);
    }
}
