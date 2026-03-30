package com.bagdatahouse.server.service;

import com.bagdatahouse.core.entity.DqcRuleDef;
import com.bagdatahouse.dqc.service.DqcRuleDefService;
import com.bagdatahouse.server.IntegrationTest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规则定义服务集成测试
 */
@IntegrationTest
@DisplayName("规则定义服务集成测试")
class DqcRuleDefServiceIT {

    @Autowired
    private DqcRuleDefService ruleDefService;

    @Test
    @DisplayName("创建规则定义-成功")
    void testCreateRuleDef() {
        IService<DqcRuleDef> service = (IService<DqcRuleDef>) ruleDefService;
        
        DqcRuleDef ruleDef = DqcRuleDef.builder()
                .ruleName("测试空值检查规则")
                .ruleCode("TEST_NULL_CHECK_001")
                .ruleType("NULL_CHECK")
                .applyLevel("COLUMN")
                .targetDsId(1L)
                .targetTable("test_table")
                .targetColumn("test_column")
                .errorLevel("MEDIUM")
                .ruleStrength("WEAK")
                .enabled(true)
                .build();

        boolean result = service.save(ruleDef);
        
        assertThat(result).isTrue();
        assertThat(ruleDef.getId()).isNotNull();
    }

    @Test
    @DisplayName("根据数据源查询规则")
    void testQueryRulesByDatasource() {
        IService<DqcRuleDef> service = (IService<DqcRuleDef>) ruleDefService;
        
        List<DqcRuleDef> rules = service.lambdaQuery()
                .eq(DqcRuleDef::getTargetDsId, 1L)
                .orderByAsc(DqcRuleDef::getSortOrder)
                .list();
        
        assertThat(rules).isNotNull();
    }
}
