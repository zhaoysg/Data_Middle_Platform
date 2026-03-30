package com.bagdatahouse.governance.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.common.util.DataMaskingEngine;
import com.bagdatahouse.common.util.WaterMarker;
import com.bagdatahouse.core.entity.*;
import com.bagdatahouse.core.mapper.*;
import com.bagdatahouse.governance.security.dto.*;
import com.bagdatahouse.governance.security.enums.*;
import com.bagdatahouse.governance.security.vo.*;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * MaskServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MaskServiceImpl 单元测试")
class MaskServiceImplTest {

    @InjectMocks
    private MaskServiceImpl maskService;

    @Mock
    private SecMaskTaskMapper maskTaskMapper;

    @Mock
    private SecMaskExecutionLogMapper executionLogMapper;

    @Mock
    private SecMaskStrategyMapper strategyMapper;

    @Mock
    private SecMaskWhitelistMapper whitelistMapper;

    @Mock
    private DqDatasourceMapper datasourceMapper;

    @Mock
    private DataMaskingEngine maskingEngine;

    @Mock
    private WaterMarker waterMarker;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(maskService, "maskTaskMapper", maskTaskMapper);
        ReflectionTestUtils.setField(maskService, "executionLogMapper", executionLogMapper);
        ReflectionTestUtils.setField(maskService, "strategyMapper", strategyMapper);
        ReflectionTestUtils.setField(maskService, "whitelistMapper", whitelistMapper);
        ReflectionTestUtils.setField(maskService, "datasourceMapper", datasourceMapper);
        ReflectionTestUtils.setField(maskService, "maskingEngine", maskingEngine);
        ReflectionTestUtils.setField(maskService, "waterMarker", waterMarker);
    }

    // ==================== saveMaskTask 测试 ====================

    @Nested
    @DisplayName("saveMaskTask - 脱敏任务保存")
    class SaveMaskTaskTests {

        @Test
        @DisplayName("1. taskCode为空时应抛出BusinessException")
        void saveMaskTask_throwsException_whenTaskCodeIsNull() {
            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("测试任务")
                    .taskCode("")
                    .build();

            assertThatThrownBy(() -> maskService.saveMaskTask(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("任务编码不能为空");
        }

        @Test
        @DisplayName("2. taskCode已存在时应抛出BusinessException")
        void saveMaskTask_throwsException_whenTaskCodeDuplicate() {
            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("测试任务")
                    .taskCode("TASK_001")
                    .build();

            when(maskTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            assertThatThrownBy(() -> maskService.saveMaskTask(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("任务编码已存在");
        }

        @Test
        @DisplayName("3. 成功创建任务时应设置默认状态为DRAFT")
        void saveMaskTask_setsDefaultStatusToDraft() {
            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("测试任务")
                    .taskCode("TASK_001")
                    .taskType("STATIC")
                    .build();

            when(maskTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(maskTaskMapper.insert(any(SecMaskTask.class))).thenAnswer(invocation -> {
                SecMaskTask task = invocation.getArgument(0);
                task.setId(1L);
                return 1;
            });

            Result<SecMaskTask> result = maskService.saveMaskTask(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getStatus()).isEqualTo(MaskTaskStatusEnum.DRAFT.getCode());
        }

        @Test
        @DisplayName("4. 未设置batchSize时应默认为1000")
        void saveMaskTask_setsDefaultBatchSizeTo1000() {
            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("测试任务")
                    .taskCode("TASK_002")
                    .build();

            when(maskTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(maskTaskMapper.insert(any(SecMaskTask.class))).thenAnswer(invocation -> {
                SecMaskTask task = invocation.getArgument(0);
                task.setId(1L);
                return 1;
            });

            Result<SecMaskTask> result = maskService.saveMaskTask(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getBatchSize()).isEqualTo(1000);
        }

        @Test
        @DisplayName("5. 未设置triggerType时应默认为MANUAL")
        void saveMaskTask_setsDefaultTriggerTypeToManual() {
            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("测试任务")
                    .taskCode("TASK_003")
                    .build();

            when(maskTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(maskTaskMapper.insert(any(SecMaskTask.class))).thenAnswer(invocation -> {
                SecMaskTask task = invocation.getArgument(0);
                task.setId(1L);
                return 1;
            });

            Result<SecMaskTask> result = maskService.saveMaskTask(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getTriggerType()).isEqualTo(MaskTriggerTypeEnum.MANUAL.getCode());
        }

        @Test
        @DisplayName("6. 未设置targetMode时应默认为APPEND")
        void saveMaskTask_setsDefaultTargetModeToAppend() {
            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("测试任务")
                    .taskCode("TASK_004")
                    .build();

            when(maskTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(maskTaskMapper.insert(any(SecMaskTask.class))).thenAnswer(invocation -> {
                SecMaskTask task = invocation.getArgument(0);
                task.setId(1L);
                return 1;
            });

            Result<SecMaskTask> result = maskService.saveMaskTask(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getTargetMode()).isEqualTo(TargetModeEnum.APPEND.getCode());
        }

        @Test
        @DisplayName("7. 总运行次数应初始化为0")
        void saveMaskTask_initializesTotalRunCountToZero() {
            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("测试任务")
                    .taskCode("TASK_005")
                    .build();

            when(maskTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(maskTaskMapper.insert(any(SecMaskTask.class))).thenAnswer(invocation -> {
                SecMaskTask task = invocation.getArgument(0);
                task.setId(1L);
                return 1;
            });

            Result<SecMaskTask> result = maskService.saveMaskTask(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getTotalRunCount()).isEqualTo(0);
        }
    }

    // ==================== updateMaskTask 测试 ====================

    @Nested
    @DisplayName("updateMaskTask - 脱敏任务更新")
    class UpdateMaskTaskTests {

        @Test
        @DisplayName("8. 更新不存在的任务应抛出BusinessException")
        void updateMaskTask_throwsException_whenTaskNotFound() {
            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("更新后的任务")
                    .build();

            when(maskTaskMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> maskService.updateMaskTask(999L, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("脱敏任务不存在");
        }

        @Test
        @DisplayName("9. 成功更新任务时应保留原有taskCode")
        void updateMaskTask_preservesOriginalTaskCode() {
            SecMaskTask existingTask = SecMaskTask.builder()
                    .id(1L)
                    .taskCode("ORIGINAL_CODE")
                    .taskName("原任务名")
                    .status(MaskTaskStatusEnum.DRAFT.getCode())
                    .build();

            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("更新后的任务名")
                    .taskCode("NEW_CODE_SHOULD_BE_IGNORED")
                    .build();

            when(maskTaskMapper.selectById(1L)).thenReturn(existingTask);
            when(maskTaskMapper.updateById(any(SecMaskTask.class))).thenReturn(1);

            maskService.updateMaskTask(1L, dto);

            ArgumentCaptor<SecMaskTask> captor = ArgumentCaptor.forClass(SecMaskTask.class);
            verify(maskTaskMapper).updateById(captor.capture());

            SecMaskTask updatedTask = captor.getValue();
            assertThat(updatedTask.getTaskCode()).isEqualTo("ORIGINAL_CODE");
            assertThat(updatedTask.getTaskName()).isEqualTo("更新后的任务名");
        }
    }

    // ==================== saveMaskStrategy 测试 ====================

    @Nested
    @DisplayName("saveMaskStrategy - 脱敏策略保存")
    class SaveMaskStrategyTests {

        @Test
        @DisplayName("10. strategyCode为空时应抛出BusinessException")
        void saveStrategy_throwsException_whenStrategyCodeIsNull() {
            SecMaskStrategySaveDTO dto = SecMaskStrategySaveDTO.builder()
                    .strategyName("测试策略")
                    .strategyCode("")
                    .build();

            assertThatThrownBy(() -> maskService.saveMaskStrategy(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("策略编码不能为空");
        }

        @Test
        @DisplayName("11. strategyCode已存在时应抛出BusinessException")
        void saveStrategy_throwsException_whenStrategyCodeDuplicate() {
            SecMaskStrategySaveDTO dto = SecMaskStrategySaveDTO.builder()
                    .strategyName("测试策略")
                    .strategyCode("STRATEGY_001")
                    .build();

            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            assertThatThrownBy(() -> maskService.saveMaskStrategy(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("策略编码已存在");
        }

        @Test
        @DisplayName("12. 成功保存策略时应设置默认状态为ENABLED")
        void saveStrategy_setsDefaultStatusToEnabled() {
            SecMaskStrategySaveDTO dto = SecMaskStrategySaveDTO.builder()
                    .strategyName("测试策略")
                    .strategyCode("STRATEGY_001")
                    .sceneType("DEVELOP_SHOW")
                    .build();

            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(strategyMapper.insert(any(SecMaskStrategy.class))).thenAnswer(invocation -> {
                SecMaskStrategy strategy = invocation.getArgument(0);
                strategy.setId(1L);
                return 1;
            });

            Result<SecMaskStrategy> result = maskService.saveMaskStrategy(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getStatus()).isEqualTo("ENABLED");
        }

        @Test
        @DisplayName("13. 未设置priority时应默认为100")
        void saveStrategy_setsDefaultPriorityTo100() {
            SecMaskStrategySaveDTO dto = SecMaskStrategySaveDTO.builder()
                    .strategyName("测试策略")
                    .strategyCode("STRATEGY_002")
                    .build();

            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(strategyMapper.insert(any(SecMaskStrategy.class))).thenAnswer(invocation -> {
                SecMaskStrategy strategy = invocation.getArgument(0);
                strategy.setId(1L);
                return 1;
            });

            Result<SecMaskStrategy> result = maskService.saveMaskStrategy(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getPriority()).isEqualTo(100);
        }

        @Test
        @DisplayName("14. 未设置conflictCheck时应默认为1（启用冲突检测）")
        void saveStrategy_setsDefaultConflictCheckTo1() {
            SecMaskStrategySaveDTO dto = SecMaskStrategySaveDTO.builder()
                    .strategyName("测试策略")
                    .strategyCode("STRATEGY_003")
                    .build();

            when(strategyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(strategyMapper.insert(any(SecMaskStrategy.class))).thenAnswer(invocation -> {
                SecMaskStrategy strategy = invocation.getArgument(0);
                strategy.setId(1L);
                return 1;
            });

            Result<SecMaskStrategy> result = maskService.saveMaskStrategy(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getConflictCheck()).isEqualTo(1);
        }
    }

    // ==================== 批量操作测试 ====================

    @Nested
    @DisplayName("批量操作测试")
    class BatchOperationTests {

        @Test
        @DisplayName("15. deleteMaskTask应正确删除任务")
        void deleteMaskTask_deletesCorrectly() {
            when(maskTaskMapper.deleteById(1L)).thenReturn(1);

            Result<Void> result = maskService.deleteMaskTask(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(maskTaskMapper).deleteById(1L);
        }

        @Test
        @DisplayName("16. batchDeleteMaskTask应批量删除任务")
        void batchDeleteMaskTask_deletesCorrectly() {
            List<Long> ids = List.of(1L, 2L, 3L);

            maskService.batchDeleteMaskTask(ids);

            verify(maskTaskMapper).deleteBatchIds(ids);
        }
    }

    // ==================== 分页查询测试 ====================

    @Nested
    @DisplayName("分页查询测试")
    class PaginationTests {

        @Test
        @DisplayName("17. pageMaskTask应返回分页结果")
        void pageMaskTask_returnsPaginatedResults() {
            Page<SecMaskTask> page = new Page<>(1, 10);
            page.setTotal(2);
            page.setRecords(List.of(
                    SecMaskTask.builder().id(1L).taskName("任务1").build(),
                    SecMaskTask.builder().id(2L).taskName("任务2").build()
            ));

            when(maskTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

            Result<Page<SecMaskTaskVO>> result = maskService.pageMaskTask(1, 10, null, null, null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getTotal()).isEqualTo(2);
            assertThat(result.getData().getRecords()).hasSize(2);
        }

        @Test
        @DisplayName("18. pageMaskStrategy应按场景类型过滤")
        void pageMaskStrategy_filtersBySceneType() {
            Page<SecMaskStrategy> page = new Page<>(1, 10);
            page.setTotal(1);
            page.setRecords(List.of(
                    SecMaskStrategy.builder().id(1L).strategyName("策略1").build()
            ));

            when(strategyMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
            when(whitelistMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            Result<Page<SecMaskStrategyVO>> result = maskService.pageMaskStrategy(1, 10, "DEVELOP_SHOW", null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getRecords()).hasSize(1);
        }
    }

    // ==================== 启用禁用测试 ====================

    @Nested
    @DisplayName("启用禁用测试")
    class EnableDisableTests {

        @Test
        @DisplayName("19. enableMaskTask应设置状态为PUBLISHED")
        void enableMaskTask_setsStatusToPublished() {
            SecMaskTask task = SecMaskTask.builder()
                    .id(1L)
                    .taskCode("TASK_001")
                    .status(MaskTaskStatusEnum.DRAFT.getCode())
                    .build();

            when(maskTaskMapper.selectById(1L)).thenReturn(task);
            when(maskTaskMapper.updateById(any(SecMaskTask.class))).thenReturn(1);

            Result<Void> result = maskService.enableMaskTask(1L);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<SecMaskTask> captor = ArgumentCaptor.forClass(SecMaskTask.class);
            verify(maskTaskMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(MaskTaskStatusEnum.PUBLISHED.getCode());
        }

        @Test
        @DisplayName("20. disableMaskTask应设置状态为DRAFT")
        void disableMaskTask_setsStatusToDraft() {
            SecMaskTask task = SecMaskTask.builder()
                    .id(1L)
                    .taskCode("TASK_001")
                    .status(MaskTaskStatusEnum.PUBLISHED.getCode())
                    .build();

            when(maskTaskMapper.selectById(1L)).thenReturn(task);
            when(maskTaskMapper.updateById(any(SecMaskTask.class))).thenReturn(1);

            Result<Void> result = maskService.disableMaskTask(1L);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<SecMaskTask> captor = ArgumentCaptor.forClass(SecMaskTask.class);
            verify(maskTaskMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(MaskTaskStatusEnum.DRAFT.getCode());
        }
    }

    // ==================== 白名单测试 ====================

    @Nested
    @DisplayName("白名单管理测试")
    class WhitelistTests {

        @Test
        @DisplayName("21. saveMaskWhitelist应验证必填字段")
        void saveMaskWhitelist_validatesRequiredFields() {
            SecMaskWhitelistSaveDTO dto = SecMaskWhitelistSaveDTO.builder()
                    .strategyId(null)
                    .entityType(null)
                    .entityId(null)
                    .build();

            assertThatThrownBy(() -> maskService.saveMaskWhitelist(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("关联策略ID不能为空");
        }

        @Test
        @DisplayName("22. saveMaskWhitelist成功时应设置默认状态为ACTIVE")
        void saveMaskWhitelist_setsDefaultStatusToActive() {
            SecMaskWhitelistSaveDTO dto = SecMaskWhitelistSaveDTO.builder()
                    .strategyId(1L)
                    .entityType("USER")
                    .entityId(100L)
                    .build();

            when(whitelistMapper.insert(any(SecMaskWhitelist.class))).thenAnswer(invocation -> {
                SecMaskWhitelist wl = invocation.getArgument(0);
                wl.setId(1L);
                return 1;
            });

            Result<SecMaskWhitelist> result = maskService.saveMaskWhitelist(dto);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getStatus()).isEqualTo(WhitelistStatusEnum.ACTIVE.getCode());
        }
    }

    // ==================== @Transactional 行为测试 ====================

    @Nested
    @DisplayName("@Transactional 事务行为测试")
    class TransactionTests {

        @Test
        @DisplayName("23. saveMaskTask执行成功时应调用mapper.insert")
        void saveMaskTask_callsInsertOnSuccess() {
            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("事务测试任务")
                    .taskCode("TX_TEST_001")
                    .build();

            when(maskTaskMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(maskTaskMapper.insert(any(SecMaskTask.class))).thenReturn(1);

            maskService.saveMaskTask(dto);

            verify(maskTaskMapper).insert(any(SecMaskTask.class));
        }

        @Test
        @DisplayName("24. updateMaskTask执行成功时应调用mapper.updateById")
        void updateMaskTask_callsUpdateByIdOnSuccess() {
            SecMaskTask existingTask = SecMaskTask.builder()
                    .id(1L)
                    .taskCode("TX_TEST_002")
                    .build();

            SecMaskTaskSaveDTO dto = SecMaskTaskSaveDTO.builder()
                    .taskName("更新后的任务")
                    .build();

            when(maskTaskMapper.selectById(1L)).thenReturn(existingTask);
            when(maskTaskMapper.updateById(any(SecMaskTask.class))).thenReturn(1);

            maskService.updateMaskTask(1L, dto);

            verify(maskTaskMapper).updateById(any(SecMaskTask.class));
        }
    }
}
