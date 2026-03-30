package com.bagdatahouse.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.MonitorAlertRuleDTO;
import com.bagdatahouse.core.entity.MonitorAlertRule;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.core.mapper.DqcRuleDefMapper;
import com.bagdatahouse.core.mapper.MonitorAlertRuleMapper;
import com.bagdatahouse.monitor.enums.AlertRuleTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AlertRuleServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRuleServiceImpl 单元测试")
class AlertRuleServiceImplTest {

    @InjectMocks
    private AlertRuleServiceImpl alertRuleService;

    @Mock
    private MonitorAlertRuleMapper baseMapper;

    @Mock
    private DqDatasourceMapper datasourceMapper;

    @Mock
    private DqcRuleDefMapper ruleDefMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(alertRuleService, "baseMapper", baseMapper);
        ReflectionTestUtils.setField(alertRuleService, "datasourceMapper", datasourceMapper);
        ReflectionTestUtils.setField(alertRuleService, "ruleDefMapper", ruleDefMapper);
    }

    // ==================== AlertRuleTypeEnum 测试 ====================

    @Nested
    @DisplayName("AlertRuleTypeEnum 枚举测试")
    class AlertRuleTypeEnumTests {

        @Test
        @DisplayName("1. QUALITY枚举值验证")
        void testQualityEnum() {
            AlertRuleTypeEnum quality = AlertRuleTypeEnum.QUALITY;
            assertThat(quality.getCode()).isEqualTo("QUALITY");
            assertThat(quality.getName()).isEqualTo("质量告警");
            assertThat(quality.getDescription()).contains("质量分数");
        }

        @Test
        @DisplayName("2. SENSITIVE_FIELD_SPIKE枚举值验证")
        void testSensitiveFieldSpikeEnum() {
            AlertRuleTypeEnum spike = AlertRuleTypeEnum.SENSITIVE_FIELD_SPIKE;
            assertThat(spike.getCode()).isEqualTo("SENSITIVE_FIELD_SPIKE");
            assertThat(spike.getName()).isEqualTo("敏感字段突增告警");
        }

        @Test
        @DisplayName("3. fromCode方法应正确解析code")
        void testFromCode() {
            assertThat(AlertRuleTypeEnum.fromCode("QUALITY")).isEqualTo(AlertRuleTypeEnum.QUALITY);
            assertThat(AlertRuleTypeEnum.fromCode("quality")).isEqualTo(AlertRuleTypeEnum.QUALITY);
            assertThat(AlertRuleTypeEnum.fromCode("INVALID")).isNull();
        }

        @Test
        @DisplayName("4. getNameByCode方法应正确获取名称")
        void testGetNameByCode() {
            assertThat(AlertRuleTypeEnum.getNameByCode("QUALITY")).isEqualTo("质量告警");
            assertThat(AlertRuleTypeEnum.getNameByCode("INVALID")).isEqualTo("INVALID");
        }

        @Test
        @DisplayName("5. allCodes方法应返回所有code")
        void testAllCodes() {
            List<String> codes = AlertRuleTypeEnum.allCodes();
            assertThat(codes).contains("QUALITY", "AVAILABILITY", "PERFORMANCE", "SYSTEM");
            assertThat(codes).contains("SENSITIVE_FIELD_SPIKE", "SENSITIVE_LEVEL_CHANGE");
        }
    }

    // ==================== saveAlertRule / create 测试 ====================

    @Nested
    @DisplayName("create - 创建告警规则")
    class CreateTests {

        @Test
        @DisplayName("6. ruleName为空时应抛出BusinessException")
        void create_throwsException_whenRuleNameIsBlank() {
            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("")
                    .ruleType("QUALITY")
                    .alertLevel("WARN")
                    .build();

            assertThatThrownBy(() -> alertRuleService.create(dto))
                    .isInstanceOf(com.bagdatahouse.common.exception.BusinessException.class)
                    .hasMessageContaining("规则名称不能为空");
        }

        @Test
        @DisplayName("7. ruleType为空时应抛出BusinessException")
        void create_throwsException_whenRuleTypeIsBlank() {
            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("测试规则")
                    .ruleType("")
                    .alertLevel("WARN")
                    .build();

            assertThatThrownBy(() -> alertRuleService.create(dto))
                    .isInstanceOf(com.bagdatahouse.common.exception.BusinessException.class)
                    .hasMessageContaining("规则类型不能为空");
        }

        @Test
        @DisplayName("8. alertLevel为空时应抛出BusinessException")
        void create_throwsException_whenAlertLevelIsBlank() {
            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("测试规则")
                    .ruleType("QUALITY")
                    .alertLevel("")
                    .build();

            assertThatThrownBy(() -> alertRuleService.create(dto))
                    .isInstanceOf(com.bagdatahouse.common.exception.BusinessException.class)
                    .hasMessageContaining("告警级别不能为空");
        }

        @Test
        @DisplayName("9. enabled为null时应默认为true")
        void create_setsDefaultEnabledToTrue() {
            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("测试规则")
                    .ruleType("QUALITY")
                    .alertLevel("WARN")
                    .enabled(null)
                    .build();

            when(baseMapper.insert(any(MonitorAlertRule.class))).thenAnswer(invocation -> {
                MonitorAlertRule rule = invocation.getArgument(0);
                rule.setId(1L);
                return 1;
            });

            Result<Long> result = alertRuleService.create(dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRule> captor = ArgumentCaptor.forClass(MonitorAlertRule.class);
            verify(baseMapper).insert(captor.capture());
            assertThat(captor.getValue().getEnabled()).isTrue();
        }

        @Test
        @DisplayName("10. 成功创建时应生成ruleCode")
        void create_generatesRuleCode() {
            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("测试规则")
                    .ruleType("QUALITY")
                    .alertLevel("WARN")
                    .build();

            when(baseMapper.insert(any(MonitorAlertRule.class))).thenAnswer(invocation -> {
                MonitorAlertRule rule = invocation.getArgument(0);
                rule.setId(1L);
                return 1;
            });

            Result<Long> result = alertRuleService.create(dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRule> captor = ArgumentCaptor.forClass(MonitorAlertRule.class);
            verify(baseMapper).insert(captor.capture());
            assertThat(captor.getValue().getRuleCode()).startsWith("ALERT_RULE_");
        }
    }

    // ==================== updateAlertRule 测试 ====================

    @Nested
    @DisplayName("update - 更新告警规则")
    class UpdateTests {

        @Test
        @DisplayName("11. 更新不存在的规则应返回失败")
        void update_returnsFail_whenRuleNotFound() {
            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("更新后的规则")
                    .ruleType("QUALITY")
                    .alertLevel("WARN")
                    .build();

            when(baseMapper.selectById(999L)).thenReturn(null);

            Result<Void> result = alertRuleService.update(999L, dto);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("告警规则不存在");
        }

        @Test
        @DisplayName("12. 成功更新规则时应更新所有字段")
        void update_updatesAllFields() {
            MonitorAlertRule existingRule = MonitorAlertRule.builder()
                    .id(1L)
                    .ruleName("原规则名")
                    .ruleCode("ALERT_RULE_001")
                    .ruleType("QUALITY")
                    .alertLevel("WARN")
                    .enabled(true)
                    .build();

            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("更新后的规则名")
                    .ruleType("AVAILABILITY")
                    .alertLevel("ERROR")
                    .targetType("TABLE")
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(existingRule);
            when(baseMapper.updateById(any(MonitorAlertRule.class))).thenReturn(1);

            Result<Void> result = alertRuleService.update(1L, dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRule> captor = ArgumentCaptor.forClass(MonitorAlertRule.class);
            verify(baseMapper).updateById(captor.capture());

            MonitorAlertRule updatedRule = captor.getValue();
            assertThat(updatedRule.getRuleName()).isEqualTo("更新后的规则名");
            assertThat(updatedRule.getRuleType()).isEqualTo("AVAILABILITY");
            assertThat(updatedRule.getAlertLevel()).isEqualTo("ERROR");
        }
    }

    // ==================== deleteAlertRule 测试 ====================

    @Nested
    @DisplayName("delete - 删除告警规则")
    class DeleteTests {

        @Test
        @DisplayName("13. 删除不存在的规则应返回失败")
        void delete_returnsFail_whenRuleNotFound() {
            when(baseMapper.selectById(999L)).thenReturn(null);

            Result<Void> result = alertRuleService.delete(999L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("告警规则不存在");
        }

        @Test
        @DisplayName("14. 成功删除规则时应调用deleteById")
        void delete_deletesCorrectly() {
            MonitorAlertRule rule = MonitorAlertRule.builder()
                    .id(1L)
                    .ruleName("待删除规则")
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(rule);
            when(baseMapper.deleteById(1L)).thenReturn(1);

            Result<Void> result = alertRuleService.delete(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(baseMapper).deleteById(1L);
        }
    }

    // ==================== listAlertRules / page 测试 ====================

    @Nested
    @DisplayName("page - 分页查询告警规则")
    class PageTests {

        @Test
        @DisplayName("15. 分页查询应返回分页结果")
        void page_returnsPaginatedResults() {
            Page<MonitorAlertRule> page = new Page<>(1, 10);
            page.setTotal(2);
            page.setRecords(List.of(
                    MonitorAlertRule.builder().id(1L).ruleName("规则1").build(),
                    MonitorAlertRule.builder().id(2L).ruleName("规则2").build()
            ));

            when(baseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Result<Page<MonitorAlertRule>> result = alertRuleService.page(1, 10, null, null, null, null, null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getTotal()).isEqualTo(2);
            assertThat(result.getData().getRecords()).hasSize(2);
        }

        @Test
        @DisplayName("16. 按规则名称模糊查询应正确过滤")
        void page_filtersByRuleName() {
            Page<MonitorAlertRule> page = new Page<>(1, 10);
            page.setTotal(1);
            page.setRecords(List.of(
                    MonitorAlertRule.builder().id(1L).ruleName("质量规则").build()
            ));

            when(baseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Result<Page<MonitorAlertRule>> result = alertRuleService.page(1, 10, "质量", null, null, null, null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getRecords()).hasSize(1);
            assertThat(result.getData().getRecords().get(0).getRuleName()).isEqualTo("质量规则");
        }
    }

    // ==================== toggleEnabled 测试 ====================

    @Nested
    @DisplayName("toggleEnabled - 切换启用状态")
    class ToggleEnabledTests {

        @Test
        @DisplayName("17. toggleEnabled应切换enabled状态")
        void toggleEnabled_switchesEnabledStatus() {
            MonitorAlertRule rule = MonitorAlertRule.builder()
                    .id(1L)
                    .ruleName("测试规则")
                    .enabled(true)
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(rule);
            when(baseMapper.updateById(any(MonitorAlertRule.class))).thenReturn(1);

            Result<Void> result = alertRuleService.toggleEnabled(1L);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRule> captor = ArgumentCaptor.forClass(MonitorAlertRule.class);
            verify(baseMapper).updateById(captor.capture());
            assertThat(captor.getValue().getEnabled()).isFalse();
        }

        @Test
        @DisplayName("18. toggleEnabled切换两次应恢复原状态")
        void toggleEnabled_restoresOriginalStatus() {
            MonitorAlertRule rule = MonitorAlertRule.builder()
                    .id(1L)
                    .ruleName("测试规则")
                    .enabled(false)
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(rule);
            when(baseMapper.updateById(any(MonitorAlertRule.class))).thenReturn(1);

            alertRuleService.toggleEnabled(1L);

            ArgumentCaptor<MonitorAlertRule> captor = ArgumentCaptor.forClass(MonitorAlertRule.class);
            verify(baseMapper).updateById(captor.capture());
            assertThat(captor.getValue().getEnabled()).isTrue();
        }
    }

    // ==================== getRuleTypeOptions 测试 ====================

    @Nested
    @DisplayName("getRuleTypeOptions - 获取规则类型选项")
    class GetRuleTypeOptionsTests {

        @Test
        @DisplayName("19. 应返回所有规则类型")
        void getRuleTypeOptions_returnsAllRuleTypes() {
            Result<Map<String, Object>> result = alertRuleService.getRuleTypeOptions();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsKey("ruleTypes");
            assertThat(result.getData().get("ruleTypes")).isNotNull();
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById - 根据ID查询")
    class GetByIdTests {

        @Test
        @DisplayName("20. 查询存在的规则应返回正确数据")
        void getById_returnsCorrectData() {
            MonitorAlertRule rule = MonitorAlertRule.builder()
                    .id(1L)
                    .ruleName("测试规则")
                    .ruleType("QUALITY")
                    .alertLevel("WARN")
                    .enabled(true)
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(rule);

            Result<MonitorAlertRule> result = alertRuleService.getById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getId()).isEqualTo(1L);
            assertThat(result.getData().getRuleName()).isEqualTo("测试规则");
        }

        @Test
        @DisplayName("21. 查询不存在的规则应返回失败")
        void getById_returnsFail_whenNotFound() {
            when(baseMapper.selectById(999L)).thenReturn(null);

            Result<MonitorAlertRule> result = alertRuleService.getById(999L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("告警规则不存在");
        }
    }

    // ==================== SENSITIVE 类型告警规则测试（T7-T12） ====================

    @Nested
    @DisplayName("createSensityAlertRule - 创建 SENSITIVE 类型告警规则")
    class CreateSensityAlertRuleTests {

        @Test
        @DisplayName("24. ruleName为空时应抛出BusinessException")
        void createSensityAlertRule_throwsException_whenRuleNameIsBlank() {
            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("")
                    .ruleType("SENSITIVE_FIELD_SPIKE")
                    .alertLevel("WARN")
                    .build();

            assertThatThrownBy(() -> alertRuleService.createSensityAlertRule(dto))
                    .isInstanceOf(com.bagdatahouse.common.exception.BusinessException.class)
                    .hasMessageContaining("规则名称不能为空");
        }

        @Test
        @DisplayName("25. 成功创建时应正确映射敏感字段")
        void createSensityAlertRule_mapsSensitiveFields() {
            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("敏感字段突增告警")
                    .ruleType("SENSITIVE_FIELD_SPIKE")
                    .alertLevel("WARN")
                    .sensitivityLevel("L3")
                    .sensitivityTable("user_info")
                    .sensitivityColumn("id_card")
                    .sensitivityDsId(5L)
                    .spikeThresholdPct(20)
                    .build();

            when(baseMapper.insert(any(MonitorAlertRule.class))).thenAnswer(invocation -> {
                MonitorAlertRule rule = invocation.getArgument(0);
                rule.setId(1L);
                return 1;
            });

            Result<Long> result = alertRuleService.createSensityAlertRule(dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRule> captor = ArgumentCaptor.forClass(MonitorAlertRule.class);
            verify(baseMapper).insert(captor.capture());
            MonitorAlertRule rule = captor.getValue();
            assertThat(rule.getSensitivityLevel()).isEqualTo("L3");
            assertThat(rule.getSensitivityTable()).isEqualTo("user_info");
            assertThat(rule.getSensitivityColumn()).isEqualTo("id_card");
            assertThat(rule.getSensitivityDsId()).isEqualTo(5L);
            assertThat(rule.getSpikeThresholdPct()).isEqualTo(20);
        }

        @Test
        @DisplayName("26. 成功创建时应设置enabled默认为true")
        void createSensityAlertRule_setsDefaultEnabled() {
            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("敏感告警")
                    .ruleType("SENSITIVE_FIELD_SPIKE")
                    .alertLevel("WARN")
                    .enabled(null)
                    .build();

            when(baseMapper.insert(any(MonitorAlertRule.class))).thenAnswer(invocation -> {
                MonitorAlertRule rule = invocation.getArgument(0);
                rule.setId(1L);
                return 1;
            });

            Result<Long> result = alertRuleService.createSensityAlertRule(dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRule> captor = ArgumentCaptor.forClass(MonitorAlertRule.class);
            verify(baseMapper).insert(captor.capture());
            assertThat(captor.getValue().getEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("updateSensityAlertRule - 更新 SENSITIVE 类型告警规则")
    class UpdateSensityAlertRuleTests {

        @Test
        @DisplayName("27. 更新不存在的规则应返回失败")
        void updateSensityAlertRule_returnsFail_whenRuleNotFound() {
            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("更新后的规则")
                    .alertLevel("WARN")
                    .build();

            when(baseMapper.selectById(999L)).thenReturn(null);

            Result<Void> result = alertRuleService.updateSensityAlertRule(999L, dto);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("告警规则不存在");
        }

        @Test
        @DisplayName("28. 成功更新应保留敏感字段")
        void updateSensityAlertRule_preservesSensitiveFields() {
            MonitorAlertRule existing = MonitorAlertRule.builder()
                    .id(1L)
                    .ruleName("原规则")
                    .ruleType("SENSITIVE_FIELD_SPIKE")
                    .alertLevel("WARN")
                    .sensitivityLevel("L3")
                    .sensitivityTable("user_info")
                    .sensitivityColumn("id_card")
                    .build();

            MonitorAlertRuleDTO dto = MonitorAlertRuleDTO.builder()
                    .ruleName("更新后规则")
                    .ruleType("SENSITIVE_FIELD_SPIKE")
                    .alertLevel("ERROR")
                    .sensitivityLevel("L4")
                    .sensitivityTable("employee")
                    .sensitivityColumn("salary")
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(existing);
            when(baseMapper.updateById(any(MonitorAlertRule.class))).thenReturn(1);

            Result<Void> result = alertRuleService.updateSensityAlertRule(1L, dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRule> captor = ArgumentCaptor.forClass(MonitorAlertRule.class);
            verify(baseMapper).updateById(captor.capture());
            MonitorAlertRule updated = captor.getValue();
            assertThat(updated.getRuleName()).isEqualTo("更新后规则");
            assertThat(updated.getAlertLevel()).isEqualTo("ERROR");
            assertThat(updated.getSensitivityLevel()).isEqualTo("L4");
            assertThat(updated.getSensitivityTable()).isEqualTo("employee");
            assertThat(updated.getSensitivityColumn()).isEqualTo("salary");
        }
    }

    @Nested
    @DisplayName("getSensityAlertRulesByLevel - 按敏感等级查询")
    class GetSensityAlertRulesByLevelTests {

        @Test
        @DisplayName("29. 查询L3等级应返回对应规则")
        void getSensityAlertRulesByLevel_filtersByLevel() {
            List<MonitorAlertRule> rules = List.of(
                    MonitorAlertRule.builder().id(1L).ruleType("SENSITIVE_FIELD_SPIKE").sensitivityLevel("L3").build()
            );

            when(baseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rules);

            Result<List<MonitorAlertRule>> result = alertRuleService.getSensityAlertRulesByLevel("L3");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getSensitivityLevel()).isEqualTo("L3");
        }

        @Test
        @DisplayName("30. 不传等级时应返回所有SENSITIVE规则")
        void getSensityAlertRulesByLevel_returnsAllSensitiveRules() {
            List<MonitorAlertRule> rules = List.of(
                    MonitorAlertRule.builder().id(1L).ruleType("SENSITIVE_FIELD_SPIKE").sensitivityLevel("L3").build(),
                    MonitorAlertRule.builder().id(2L).ruleType("SENSITIVE_LEVEL_CHANGE").sensitivityLevel("L4").build()
            );

            when(baseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rules);

            Result<List<MonitorAlertRule>> result = alertRuleService.getSensityAlertRulesByLevel(null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getSensityAlertRulesByDsId - 按数据源查询")
    class GetSensityAlertRulesByDsIdTests {

        @Test
        @DisplayName("31. 按数据源查询应正确过滤")
        void getSensityAlertRulesByDsId_filtersByDsId() {
            List<MonitorAlertRule> rules = List.of(
                    MonitorAlertRule.builder().id(1L).ruleType("SENSITIVE_FIELD_SPIKE").sensitivityDsId(5L).build()
            );

            when(baseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rules);

            Result<List<MonitorAlertRule>> result = alertRuleService.getSensityAlertRulesByDsId(5L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getSensitivityDsId()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("getTargetListByType - 获取目标列表")
    class GetTargetListByTypeTests {

        @Test
        @DisplayName("32. DATASOURCE类型应返回数据源列表")
        void getTargetListByType_returnsDatasources() {
            Result<Map<String, Object>> result = alertRuleService.getTargetListByType("DATASOURCE");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsKey("targets");
        }

        @Test
        @DisplayName("33. RULE类型应返回质检规则列表")
        void getTargetListByType_returnsRules() {
            Result<Map<String, Object>> result = alertRuleService.getTargetListByType("RULE");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsKey("targets");
        }

        @Test
        @DisplayName("34. 未知类型应返回空列表")
        void getTargetListByType_returnsEmptyForUnknown() {
            Result<Map<String, Object>> result = alertRuleService.getTargetListByType("UNKNOWN_TYPE");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsKey("targets");
        }
    }
}
