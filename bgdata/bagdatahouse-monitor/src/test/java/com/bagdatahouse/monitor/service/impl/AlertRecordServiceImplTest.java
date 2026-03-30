package com.bagdatahouse.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SensityAlertDTO;
import com.bagdatahouse.core.entity.MonitorAlertRecord;
import com.bagdatahouse.core.mapper.MonitorAlertRecordMapper;
import com.bagdatahouse.monitor.enums.AlertLevelEnum;
import com.bagdatahouse.monitor.enums.AlertStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AlertRecordServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertRecordServiceImpl 单元测试")
class AlertRecordServiceImplTest {

    @InjectMocks
    private AlertRecordServiceImpl alertRecordService;

    @Mock
    private MonitorAlertRecordMapper baseMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(alertRecordService, "baseMapper", baseMapper);
    }

    // ==================== listAlertRecords / page 测试 ====================

    @Nested
    @DisplayName("page - 分页查询告警记录")
    class PageTests {

        @Test
        @DisplayName("1. 分页查询应返回分页结果")
        void page_returnsPaginatedResults() {
            Page<MonitorAlertRecord> page = new Page<>(1, 10);
            page.setTotal(2);
            page.setRecords(List.of(
                    MonitorAlertRecord.builder().id(1L).alertTitle("告警1").alertLevel("WARN").build(),
                    MonitorAlertRecord.builder().id(2L).alertTitle("告警2").alertLevel("ERROR").build()
            ));

            when(baseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Result<Page<MonitorAlertRecord>> result = alertRecordService.page(1, 10, null, null, null, null, null, null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getTotal()).isEqualTo(2);
            assertThat(result.getData().getRecords()).hasSize(2);
        }

        @Test
        @DisplayName("2. 按告警级别过滤应正确工作")
        void page_filtersByAlertLevel() {
            Page<MonitorAlertRecord> page = new Page<>(1, 10);
            page.setTotal(1);
            page.setRecords(List.of(
                    MonitorAlertRecord.builder().id(1L).alertTitle("严重告警").alertLevel("CRITICAL").build()
            ));

            when(baseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Result<Page<MonitorAlertRecord>> result = alertRecordService.page(1, 10, null, "CRITICAL", null, null, null, null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getRecords()).hasSize(1);
            assertThat(result.getData().getRecords().get(0).getAlertLevel()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("3. 按状态过滤应正确工作")
        void page_filtersByStatus() {
            Page<MonitorAlertRecord> page = new Page<>(1, 10);
            page.setTotal(1);
            page.setRecords(List.of(
                    MonitorAlertRecord.builder().id(1L).alertTitle("待处理告警").status("PENDING").build()
            ));

            when(baseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Result<Page<MonitorAlertRecord>> result = alertRecordService.page(1, 10, null, null, "PENDING", null, null, null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getRecords()).hasSize(1);
            assertThat(result.getData().getRecords().get(0).getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("4. 按规则名称模糊查询应正确工作")
        void page_filtersByRuleName() {
            Page<MonitorAlertRecord> page = new Page<>(1, 10);
            page.setTotal(1);
            page.setRecords(List.of(
                    MonitorAlertRecord.builder().id(1L).ruleName("质量规则告警").build()
            ));

            when(baseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Result<Page<MonitorAlertRecord>> result = alertRecordService.page(1, 10, "质量", null, null, null, null, null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getRecords()).hasSize(1);
        }
    }

    // ==================== saveAlertRecord / createSensityAlert 测试 ====================

    @Nested
    @DisplayName("createSensityAlert - 创建敏感告警记录")
    class CreateSensityAlertTests {

        @Test
        @DisplayName("5. dto为null时应返回失败")
        void createSensityAlert_returnsFail_whenDtoIsNull() {
            Result<Long> result = alertRecordService.createSensityAlert(null);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("告警信息不能为空");
        }

        @Test
        @DisplayName("6. 成功创建时应生成alertNo")
        void createSensityAlert_generatesAlertNo() {
            SensityAlertDTO dto = SensityAlertDTO.builder()
                    .alertTitle("敏感字段突增告警")
                    .alertLevel("WARN")
                    .alertContent("检测到敏感字段数突增")
                    .sensitivityLevel("L2")
                    .sensitivityTable("user_info")
                    .sensitivityColumn("id_card")
                    .build();

            when(baseMapper.insert(any(MonitorAlertRecord.class))).thenAnswer(invocation -> {
                MonitorAlertRecord record = invocation.getArgument(0);
                record.setId(1L);
                return 1;
            });

            Result<Long> result = alertRecordService.createSensityAlert(dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRecord> captor = ArgumentCaptor.forClass(MonitorAlertRecord.class);
            verify(baseMapper).insert(captor.capture());
            assertThat(captor.getValue().getAlertNo()).startsWith("ALERT_");
        }

        @Test
        @DisplayName("7. 成功创建时应设置默认告警级别为WARN")
        void createSensityAlert_setsDefaultAlertLevelToWarn() {
            SensityAlertDTO dto = SensityAlertDTO.builder()
                    .alertTitle("敏感字段突增告警")
                    .alertLevel(null)
                    .sensitivityLevel("L2")
                    .sensitivityTable("user_info")
                    .build();

            when(baseMapper.insert(any(MonitorAlertRecord.class))).thenAnswer(invocation -> {
                MonitorAlertRecord record = invocation.getArgument(0);
                record.setId(1L);
                return 1;
            });

            Result<Long> result = alertRecordService.createSensityAlert(dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRecord> captor = ArgumentCaptor.forClass(MonitorAlertRecord.class);
            verify(baseMapper).insert(captor.capture());
            assertThat(captor.getValue().getAlertLevel()).isEqualTo("WARN");
        }

        @Test
        @DisplayName("8. 成功创建时应设置状态为PENDING")
        void createSensityAlert_setsStatusToPending() {
            SensityAlertDTO dto = SensityAlertDTO.builder()
                    .alertTitle("敏感字段突增告警")
                    .alertLevel("ERROR")
                    .sensitivityLevel("L2")
                    .sensitivityTable("user_info")
                    .build();

            when(baseMapper.insert(any(MonitorAlertRecord.class))).thenAnswer(invocation -> {
                MonitorAlertRecord record = invocation.getArgument(0);
                record.setId(1L);
                return 1;
            });

            Result<Long> result = alertRecordService.createSensityAlert(dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRecord> captor = ArgumentCaptor.forClass(MonitorAlertRecord.class);
            verify(baseMapper).insert(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(AlertStatusEnum.PENDING.getCode());
        }

        @Test
        @DisplayName("9. 告警字段应正确映射")
        void createSensityAlert_mapsFieldsCorrectly() {
            SensityAlertDTO dto = SensityAlertDTO.builder()
                    .alertTitle("敏感字段突增告警")
                    .alertLevel("ERROR")
                    .alertContent("检测到敏感字段数突增超过阈值")
                    .alertType("SENSITIVE_FIELD_SPIKE")
                    .sensitivityLevel("L3")
                    .sensitivityTable("employee")
                    .sensitivityColumn("salary")
                    .sensitivityDsId(100L)
                    .scanBatchNo("BATCH_20260326_001")
                    .triggerValue(new BigDecimal("150.5"))
                    .thresholdValue(new BigDecimal("100.0"))
                    .build();

            when(baseMapper.insert(any(MonitorAlertRecord.class))).thenAnswer(invocation -> {
                MonitorAlertRecord record = invocation.getArgument(0);
                record.setId(1L);
                return 1;
            });

            Result<Long> result = alertRecordService.createSensityAlert(dto);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRecord> captor = ArgumentCaptor.forClass(MonitorAlertRecord.class);
            verify(baseMapper).insert(captor.capture());

            MonitorAlertRecord record = captor.getValue();
            assertThat(record.getRuleName()).isEqualTo("敏感字段突增告警");
            assertThat(record.getAlertTitle()).isEqualTo("敏感字段突增告警");
            assertThat(record.getAlertContent()).isEqualTo("检测到敏感字段数突增超过阈值");
            assertThat(record.getSensitivityLevel()).isEqualTo("L3");
            assertThat(record.getSensitivityTable()).isEqualTo("employee");
            assertThat(record.getSensitivityColumn()).isEqualTo("salary");
            assertThat(record.getSensitivityDsId()).isEqualTo(100L);
            assertThat(record.getScanBatchNo()).isEqualTo("BATCH_20260326_001");
            assertThat(record.getTriggerValue()).isEqualByComparingTo(new BigDecimal("150.5"));
            assertThat(record.getThresholdValue()).isEqualByComparingTo(new BigDecimal("100.0"));
        }
    }

    // ==================== updateAlertRecord / resolve 测试 ====================

    @Nested
    @DisplayName("resolve - 解决告警记录")
    class ResolveTests {

        @Test
        @DisplayName("10. 解决不存在的记录应返回失败")
        void resolve_returnsFail_whenRecordNotFound() {
            when(baseMapper.selectById(999L)).thenReturn(null);

            Result<Void> result = alertRecordService.resolve(999L, 1L, "已处理");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("告警记录不存在");
        }

        @Test
        @DisplayName("11. 重复解决应返回失败")
        void resolve_returnsFail_whenAlreadyResolved() {
            MonitorAlertRecord record = MonitorAlertRecord.builder()
                    .id(1L)
                    .status(AlertStatusEnum.RESOLVED.getCode())
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(record);

            Result<Void> result = alertRecordService.resolve(1L, 1L, "已处理");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("该告警已经解决");
        }

        @Test
        @DisplayName("12. 成功解决应更新状态和解决信息")
        void resolve_updatesStatusAndResolveInfo() {
            MonitorAlertRecord record = MonitorAlertRecord.builder()
                    .id(1L)
                    .status(AlertStatusEnum.PENDING.getCode())
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(record);
            when(baseMapper.updateById(any(MonitorAlertRecord.class))).thenReturn(1);

            Result<Void> result = alertRecordService.resolve(1L, 100L, "已排查原因并处理");

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRecord> captor = ArgumentCaptor.forClass(MonitorAlertRecord.class);
            verify(baseMapper).updateById(captor.capture());

            MonitorAlertRecord updated = captor.getValue();
            assertThat(updated.getStatus()).isEqualTo(AlertStatusEnum.RESOLVED.getCode());
            assertThat(updated.getResolveUser()).isEqualTo(100L);
            assertThat(updated.getResolveComment()).isEqualTo("已排查原因并处理");
            assertThat(updated.getResolvedTime()).isNotNull();
        }
    }

    // ==================== markAsRead 测试 ====================

    @Nested
    @DisplayName("markAsRead - 标记告警为已读")
    class MarkAsReadTests {

        @Test
        @DisplayName("13. 标记不存在的记录应返回失败")
        void markAsRead_returnsFail_whenRecordNotFound() {
            when(baseMapper.selectById(999L)).thenReturn(null);

            Result<Void> result = alertRecordService.markAsRead(999L, 1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("告警记录不存在");
        }

        @Test
        @DisplayName("14. 标记已解决的记录应返回失败")
        void markAsRead_returnsFail_whenRecordResolved() {
            MonitorAlertRecord record = MonitorAlertRecord.builder()
                    .id(1L)
                    .status(AlertStatusEnum.RESOLVED.getCode())
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(record);

            Result<Void> result = alertRecordService.markAsRead(1L, 1L);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("已解决的告警无法标记为已读");
        }

        @Test
        @DisplayName("15. 已读的记录再次标记应直接返回成功")
        void markAsRead_returnsSuccess_whenAlreadyRead() {
            MonitorAlertRecord record = MonitorAlertRecord.builder()
                    .id(1L)
                    .status(AlertStatusEnum.READ.getCode())
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(record);

            Result<Void> result = alertRecordService.markAsRead(1L, 1L);

            assertThat(result.isSuccess()).isTrue();
            verify(baseMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("16. 成功标记应为已读状态并记录读取用户和时间")
        void markAsRead_setsReadStatusAndInfo() {
            MonitorAlertRecord record = MonitorAlertRecord.builder()
                    .id(1L)
                    .status(AlertStatusEnum.PENDING.getCode())
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(record);
            when(baseMapper.updateById(any(MonitorAlertRecord.class))).thenReturn(1);

            Result<Void> result = alertRecordService.markAsRead(1L, 100L);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<MonitorAlertRecord> captor = ArgumentCaptor.forClass(MonitorAlertRecord.class);
            verify(baseMapper).updateById(captor.capture());

            MonitorAlertRecord updated = captor.getValue();
            assertThat(updated.getStatus()).isEqualTo(AlertStatusEnum.READ.getCode());
            assertThat(updated.getReadUser()).isEqualTo(100L);
            assertThat(updated.getReadTime()).isNotNull();
        }
    }

    // ==================== deleteAlertRecord 测试 ====================

    @Nested
    @DisplayName("delete - 删除告警记录")
    class DeleteTests {

        @Test
        @DisplayName("17. ServiceImpl应继承baseMapper的deleteById")
        void delete_delegatesToBaseMapper() {
            when(baseMapper.deleteById(1L)).thenReturn(1);

            alertRecordService.removeById(1L);

            verify(baseMapper).deleteById(1L);
        }
    }

    // ==================== AlertRecord字段映射测试 ====================

    @Nested
    @DisplayName("告警记录字段映射测试")
    class AlertRecordFieldMappingTests {

        @Test
        @DisplayName("18. MonitorAlertRecord实体字段应正确设置")
        void alertRecordFieldsSetCorrectly() {
            MonitorAlertRecord record = MonitorAlertRecord.builder()
                    .id(1L)
                    .alertNo("ALERT_20260326120000_0001")
                    .ruleId(10L)
                    .ruleName("质量规则告警")
                    .ruleCode("QUALITY_RULE_001")
                    .alertLevel("ERROR")
                    .alertTitle("数据质量告警")
                    .alertContent("检测到数据异常")
                    .targetType("TABLE")
                    .targetId("user_info")
                    .targetName("用户信息表")
                    .status("PENDING")
                    .sentTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .build();

            assertThat(record.getId()).isEqualTo(1L);
            assertThat(record.getAlertNo()).isEqualTo("ALERT_20260326120000_0001");
            assertThat(record.getRuleId()).isEqualTo(10L);
            assertThat(record.getRuleName()).isEqualTo("质量规则告警");
            assertThat(record.getAlertLevel()).isEqualTo("ERROR");
            assertThat(record.getTargetType()).isEqualTo("TABLE");
            assertThat(record.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("19. 敏感字段相关字段应正确设置")
        void sensitivityFieldsSetCorrectly() {
            MonitorAlertRecord record = MonitorAlertRecord.builder()
                    .sensitivityLevel("L4")
                    .sensitivityTable("employee")
                    .sensitivityColumn("ssn")
                    .sensitivityDsId(5L)
                    .scanBatchNo("SCAN_20260326_001")
                    .triggerValue(new BigDecimal("200.0"))
                    .thresholdValue(new BigDecimal("100.0"))
                    .build();

            assertThat(record.getSensitivityLevel()).isEqualTo("L4");
            assertThat(record.getSensitivityTable()).isEqualTo("employee");
            assertThat(record.getSensitivityColumn()).isEqualTo("ssn");
            assertThat(record.getSensitivityDsId()).isEqualTo(5L);
            assertThat(record.getScanBatchNo()).isEqualTo("SCAN_20260326_001");
        }
    }

    // ==================== AlertStatusEnum 测试 ====================

    @Nested
    @DisplayName("AlertStatusEnum 枚举测试")
    class AlertStatusEnumTests {

        @Test
        @DisplayName("20. AlertStatusEnum各状态值应正确")
        void alertStatusEnumValuesAreCorrect() {
            assertThat(AlertStatusEnum.PENDING.getCode()).isEqualTo("PENDING");
            assertThat(AlertStatusEnum.SENT.getCode()).isEqualTo("SENT");
            assertThat(AlertStatusEnum.READ.getCode()).isEqualTo("READ");
            assertThat(AlertStatusEnum.RESOLVED.getCode()).isEqualTo("RESOLVED");
        }

        @Test
        @DisplayName("21. AlertStatusEnum应有正确的枚举总数")
        void alertStatusEnumCountIsCorrect() {
            assertThat(AlertStatusEnum.values()).hasSize(4);
        }
    }

    // ==================== AlertLevelEnum 测试 ====================

    @Nested
    @DisplayName("AlertLevelEnum 枚举测试")
    class AlertLevelEnumTests {

        @Test
        @DisplayName("22. AlertLevelEnum各级别值应正确")
        void alertLevelEnumValuesAreCorrect() {
            assertThat(AlertLevelEnum.INFO.getCode()).isEqualTo("INFO");
            assertThat(AlertLevelEnum.WARN.getCode()).isEqualTo("WARN");
            assertThat(AlertLevelEnum.ERROR.getCode()).isEqualTo("ERROR");
            assertThat(AlertLevelEnum.CRITICAL.getCode()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("23. AlertLevelEnum应有正确的枚举总数")
        void alertLevelEnumCountIsCorrect() {
            assertThat(AlertLevelEnum.values()).hasSize(4);
        }
    }

    // ==================== 批量操作测试 ====================

    @Nested
    @DisplayName("批量操作测试")
    class BatchOperationTests {

        @Test
        @DisplayName("24. batchMarkAsRead应逐条处理")
        void batchMarkAsRead_processesEachRecord() {
            MonitorAlertRecord record1 = MonitorAlertRecord.builder()
                    .id(1L)
                    .status(AlertStatusEnum.PENDING.getCode())
                    .build();
            MonitorAlertRecord record2 = MonitorAlertRecord.builder()
                    .id(2L)
                    .status(AlertStatusEnum.PENDING.getCode())
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(record1);
            when(baseMapper.selectById(2L)).thenReturn(record2);
            when(baseMapper.updateById(any(MonitorAlertRecord.class))).thenReturn(1);

            Result<Void> result = alertRecordService.batchMarkAsRead(new Long[]{1L, 2L}, 100L);

            assertThat(result.isSuccess()).isTrue();
            verify(baseMapper, times(2)).updateById(any(MonitorAlertRecord.class));
        }

        @Test
        @DisplayName("25. batchResolve应逐条处理")
        void batchResolve_processesEachRecord() {
            MonitorAlertRecord record1 = MonitorAlertRecord.builder()
                    .id(1L)
                    .status(AlertStatusEnum.PENDING.getCode())
                    .build();
            MonitorAlertRecord record2 = MonitorAlertRecord.builder()
                    .id(2L)
                    .status(AlertStatusEnum.PENDING.getCode())
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(record1);
            when(baseMapper.selectById(2L)).thenReturn(record2);
            when(baseMapper.updateById(any(MonitorAlertRecord.class))).thenReturn(1);

            Result<Void> result = alertRecordService.batchResolve(new Long[]{1L, 2L}, 100L, "批量处理完成");
        }
    }

    // ==================== getAlertOverview 测试 ====================

    @Nested
    @DisplayName("getAlertOverview - 获取告警统计概览")
    class GetAlertOverviewTests {

        @Test
        @DisplayName("26. 应返回今日待处理数量")
        void getAlertOverview_returnsPendingCount() {
            when(baseMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L, 20L, 5L, 3L);

            Result<Map<String, Object>> result = alertRecordService.getAlertOverview();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsKey("pendingCount");
            assertThat(result.getData()).containsKey("todayTotal");
            assertThat(result.getData()).containsKey("todayResolved");
            assertThat(result.getData()).containsKey("criticalCount");
        }
    }

    // ==================== getAlertLevelDistribution 测试 ====================

    @Nested
    @DisplayName("getAlertLevelDistribution - 获取告警级别分布")
    class GetAlertLevelDistributionTests {

        @Test
        @DisplayName("27. 应返回各级别告警数量")
        void getAlertLevelDistribution_returnsLevelCounts() {
            List<MonitorAlertRecord> records = List.of(
                    MonitorAlertRecord.builder().alertLevel("INFO").build(),
                    MonitorAlertRecord.builder().alertLevel("WARN").build(),
                    MonitorAlertRecord.builder().alertLevel("WARN").build(),
                    MonitorAlertRecord.builder().alertLevel("ERROR").build()
            );

            when(baseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(records);

            Result<Map<String, Object>> result = alertRecordService.getAlertLevelDistribution(null, null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsKey("INFO");
            assertThat(result.getData()).containsKey("WARN");
            assertThat(result.getData()).containsKey("ERROR");
            assertThat(result.getData()).containsKey("CRITICAL");
        }
    }

    // ==================== getAlertTrend 测试 ====================

    @Nested
    @DisplayName("getAlertTrend - 获取告警趋势")
    class GetAlertTrendTests {

        @Test
        @DisplayName("28. 应返回每日告警统计")
        void getAlertTrend_returnsDailyStats() {
            LocalDateTime now = LocalDateTime.now();
            List<MonitorAlertRecord> records = List.of(
                    MonitorAlertRecord.builder().alertLevel("WARN").createTime(now).build(),
                    MonitorAlertRecord.builder().alertLevel("ERROR").createTime(now).build()
            );

            when(baseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(records);

            Result<Map<String, Object>> result = alertRecordService.getAlertTrend("7");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsKey("dailyStats");
        }

        @Test
        @DisplayName("29. 默认天数应为7天")
        void getAlertTrend_usesDefaultDays() {
            when(baseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            Result<Map<String, Object>> result = alertRecordService.getAlertTrend(null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).containsKey("dailyStats");
        }
    }

    // ==================== createSensityAlertBatch 测试 ====================

    @Nested
    @DisplayName("createSensityAlertBatch - 批量创建敏感告警")
    class CreateSensityAlertBatchTests {

        @Test
        @DisplayName("30. 空列表应返回0")
        void createSensityAlertBatch_returnsZero_whenEmpty() {
            Result<Integer> result = alertRecordService.createSensityAlertBatch(List.of());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(0);
        }

        @Test
        @DisplayName("31. 成功批量创建应返回正确数量")
        void createSensityAlertBatch_returnsSuccessCount() {
            List<SensityAlertDTO> alerts = List.of(
                    SensityAlertDTO.builder().alertTitle("告警1").sensitivityLevel("L3").sensitivityTable("t1").build(),
                    SensityAlertDTO.builder().alertTitle("告警2").sensitivityLevel("L4").sensitivityTable("t2").build()
            );

            when(baseMapper.insert(any(MonitorAlertRecord.class))).thenReturn(1);

            Result<Integer> result = alertRecordService.createSensityAlertBatch(alerts);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(2);
            verify(baseMapper, times(2)).insert(any(MonitorAlertRecord.class));
        }

        @Test
        @DisplayName("32. 列表为null时应返回0")
        void createSensityAlertBatch_returnsZero_whenNull() {
            Result<Integer> result = alertRecordService.createSensityAlertBatch(null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEqualTo(0);
        }
    }

    // ==================== getById 测试补充 ====================

    @Nested
    @DisplayName("getById - 根据ID查询（补充）")
    class GetByIdExtraTests {

        @Test
        @DisplayName("33. 查询已存在的记录应返回完整数据")
        void getById_returnsFullData() {
            MonitorAlertRecord record = MonitorAlertRecord.builder()
                    .id(1L)
                    .alertNo("ALERT_TEST")
                    .alertTitle("测试告警")
                    .alertLevel("WARN")
                    .status("PENDING")
                    .build();

            when(baseMapper.selectById(1L)).thenReturn(record);

            Result<MonitorAlertRecord> result = alertRecordService.getById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getId()).isEqualTo(1L);
            assertThat(result.getData().getAlertNo()).isEqualTo("ALERT_TEST");
        }
    }
}
