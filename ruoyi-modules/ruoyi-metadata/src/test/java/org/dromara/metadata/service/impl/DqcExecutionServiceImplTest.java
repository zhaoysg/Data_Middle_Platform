package org.dromara.metadata.service.impl;

import org.dromara.metadata.domain.vo.DqcExecutionDetailVo;
import org.dromara.metadata.domain.DqcExecution;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcPlan;
import org.dromara.metadata.domain.DqcPlanRule;
import org.dromara.metadata.domain.DqcRuleDef;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.engine.executor.CustomSqlSecuritySupport;
import org.dromara.metadata.engine.executor.MetadataContext;
import org.dromara.metadata.mapper.DqcExecutionDetailMapper;
import org.dromara.metadata.mapper.DqcExecutionMapper;
import org.dromara.metadata.mapper.MetadataColumnMapper;
import org.dromara.metadata.mapper.MetadataTableMapper;
import org.dromara.metadata.mapper.DqcPlanMapper;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.mapper.DqcRuleDefMapper;
import org.dromara.metadata.service.IDqcQualityScoreService;
import org.dromara.metadata.support.DatasourceHelper;
import org.dromara.metadata.engine.executor.RuleExecutorFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
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

    @Mock
    private MetadataTableMapper metadataTableMapper;

    @Mock
    private MetadataColumnMapper metadataColumnMapper;

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

    @Test
    void buildMetadataContextShouldUsePlanRuleBindingAndPlanDatasource() {
        DqcRuleDef rule = new DqcRuleDef();
        rule.setId(1L);
        rule.setTableId(99L);

        DqcPlan plan = new DqcPlan();
        plan.setId(2L);
        plan.setBindValue("{\"dsId\":\"2038582379342303234\"}");

        DqcPlanRule planRule = new DqcPlanRule();
        planRule.setRuleId(1L);
        planRule.setTargetTable("chat_message");
        planRule.setTargetColumn("id");

        MetadataTable metadataTable = new MetadataTable();
        metadataTable.setId(99L);
        metadataTable.setDsId(100L);
        metadataTable.setDsCode("legacy_ds");
        metadataTable.setTableName("legacy_table");
        when(metadataTableMapper.selectById(99L)).thenReturn(metadataTable);

        MetadataContext context = ReflectionTestUtils.invokeMethod(
            service, "buildMetadataContext", rule, plan, planRule
        );

        assertEquals("chat_message", context.getTableName());
        assertEquals("id", context.getColumnName());
        assertEquals(2038582379342303234L, context.getDsId());
    }

    @Test
    void createExecutionDetailShouldPreferPlanRuleTarget() {
        DqcExecution execution = new DqcExecution();
        execution.setId(10L);

        DqcRuleDef rule = new DqcRuleDef();
        rule.setId(11L);
        rule.setTableId(88L);
        rule.setRuleName("手机号空值检查");
        rule.setRuleCode("NULL_CHECK_PHONE");
        rule.setRuleType("NULL_CHECK");
        rule.setDimensions("COMPLETENESS");

        DqcPlan plan = new DqcPlan();
        plan.setId(12L);
        plan.setBindValue("{\"dsId\":\"2038582379342303234\"}");

        DqcPlanRule planRule = new DqcPlanRule();
        planRule.setRuleId(11L);
        planRule.setTargetTable("chat_message");
        planRule.setTargetColumn("id");

        MetadataTable metadataTable = new MetadataTable();
        metadataTable.setId(88L);
        metadataTable.setDsId(100L);
        metadataTable.setTableName("legacy_table");
        when(metadataTableMapper.selectById(88L)).thenReturn(metadataTable);

        DqcExecutionDetail detail = ReflectionTestUtils.invokeMethod(
            service, "createExecutionDetail", execution, rule, plan, planRule
        );

        assertEquals("chat_message", detail.getTargetTable());
        assertEquals("id", detail.getTargetColumn());
        assertEquals(2038582379342303234L, detail.getTargetDsId());
    }
}
