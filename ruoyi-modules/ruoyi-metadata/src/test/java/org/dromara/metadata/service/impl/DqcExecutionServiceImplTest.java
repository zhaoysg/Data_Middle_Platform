package org.dromara.metadata.service.impl;

import org.dromara.metadata.domain.vo.DqcExecutionDetailVo;
import org.dromara.metadata.engine.executor.CustomSqlSecuritySupport;
import org.dromara.metadata.mapper.DqcExecutionDetailMapper;
import org.dromara.metadata.mapper.DqcExecutionMapper;
import org.dromara.metadata.mapper.DqcPlanMapper;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.mapper.DqcRuleDefMapper;
import org.dromara.metadata.service.IDqcQualityScoreService;
import org.dromara.metadata.support.DatasourceHelper;
import org.dromara.metadata.engine.executor.RuleExecutorFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("local")
class DqcExecutionServiceImplTest {

    @Mock
    private DqcExecutionMapper executionMapper;

    @Mock
    private DqcExecutionDetailMapper detailMapper;

    @Mock
    private DqcPlanMapper planMapper;

    @Mock
    private DqcRuleDefMapper ruleDefMapper;

    @Mock
    private DqcPlanRuleMapper planRuleMapper;

    @Mock
    private DatasourceHelper datasourceHelper;

    @Mock
    private RuleExecutorFactory executorFactory;

    @Mock
    private IDqcQualityScoreService qualityScoreService;

    @InjectMocks
    private DqcExecutionServiceImpl service;

    @Test
    void listDetailsByExecutionIdShouldRedactCustomSqlDetails() {
        DqcExecutionDetailVo custom = new DqcExecutionDetailVo();
        custom.setRuleType("CUSTOM_SQL");
        custom.setExecuteSql("SELECT phone FROM user_info");
        custom.setActualValue("13812345678");
        custom.setErrorMsg("syntax error near SELECT phone FROM user_info");

        DqcExecutionDetailVo normal = new DqcExecutionDetailVo();
        normal.setRuleType("NULL_CHECK");
        normal.setExecuteSql("SELECT COUNT(*) FROM orders");
        normal.setActualValue("12");

        when(detailMapper.selectVoList(any())).thenReturn(List.of(custom, normal));

        List<DqcExecutionDetailVo> details = service.listDetailsByExecutionId(100L);

        assertEquals(CustomSqlSecuritySupport.REDACTED_SQL, details.get(0).getExecuteSql());
        assertNull(details.get(0).getActualValue());
        assertEquals(CustomSqlSecuritySupport.EXECUTION_ERROR, details.get(0).getErrorMsg());
        assertEquals("SELECT COUNT(*) FROM orders", details.get(1).getExecuteSql());
        assertEquals("12", details.get(1).getActualValue());
    }
}
