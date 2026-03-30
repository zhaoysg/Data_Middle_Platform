package com.bagdatahouse.dqc.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("执行结果测试")
class ExecutionResultTest {

    @Nested
    @DisplayName("Builder模式测试")
    class BuilderTest {
        @Test
        @DisplayName("使用Builder构建成功结果")
        void testBuildSuccessResult() {
            ExecutionResult result = ExecutionResult.builder()
                    .success(true)
                    .ruleId(1L)
                    .ruleName("空值检查")
                    .ruleCode("NULL_CHECK_001")
                    .status(ExecutionResult.Status.SUCCESS)
                    .totalCount(1000L)
                    .passCount(950L)
                    .errorCount(50L)
                    .actualValue(new BigDecimal("0.05"))
                    .qualityScore(95)
                    .elapsedMs(1500L)
                    .startTime(LocalDateTime.now().minusSeconds(2))
                    .build();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getRuleId()).isEqualTo(1L);
            assertThat(result.getRuleName()).isEqualTo("空值检查");
            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.SUCCESS);
            assertThat(result.getTotalCount()).isEqualTo(1000L);
            assertThat(result.getPassCount()).isEqualTo(950L);
            assertThat(result.getErrorCount()).isEqualTo(50L);
            assertThat(result.getQualityScore()).isEqualTo(95);
        }

        @Test
        @DisplayName("使用Builder构建失败结果")
        void testBuildFailedResult() {
            ExecutionResult result = ExecutionResult.builder()
                    .success(false)
                    .ruleId(2L)
                    .ruleName("唯一性检查")
                    .status(ExecutionResult.Status.FAILED)
                    .errorDetail("字段存在重复值")
                    .qualityScore(0)
                    .build();

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.FAILED);
            assertThat(result.getErrorDetail()).isEqualTo("字段存在重复值");
            assertThat(result.getQualityScore()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTest {
        @Test
        @DisplayName("success静态工厂方法")
        void testSuccessFactory() {
            LocalDateTime startTime = LocalDateTime.now().minusSeconds(3);
            ExecutionResult result = ExecutionResult.success(
                    1L,
                    "空值检查",
                    "SELECT COUNT(*) FROM test WHERE col IS NULL",
                    1000L,
                    950L,
                    50L,
                    new BigDecimal("0.05"),
                    95,
                    1500L,
                    startTime
            );

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getRuleId()).isEqualTo(1L);
            assertThat(result.getRuleName()).isEqualTo("空值检查");
            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.SUCCESS);
            assertThat(result.getTotalCount()).isEqualTo(1000L);
            assertThat(result.getPassCount()).isEqualTo(950L);
            assertThat(result.getErrorCount()).isEqualTo(50L);
            assertThat(result.getQualityScore()).isEqualTo(95);
            assertThat(result.getBlocked()).isFalse();
            assertThat(result.getEndTime()).isNotNull();
        }

        @Test
        @DisplayName("failed静态工厂方法")
        void testFailedFactory() {
            LocalDateTime startTime = LocalDateTime.now().minusSeconds(2);
            ExecutionResult result = ExecutionResult.failed(
                    2L,
                    "唯一性检查",
                    "SELECT COUNT(*) FROM test GROUP BY col HAVING COUNT(*) > 1",
                    "字段存在重复值",
                    1000L,
                    startTime
            );

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.FAILED);
            assertThat(result.getErrorDetail()).isEqualTo("字段存在重复值");
            assertThat(result.getQualityScore()).isEqualTo(0);
            assertThat(result.getBlocked()).isFalse();
        }

        @Test
        @DisplayName("skipped静态工厂方法")
        void testSkippedFactory() {
            LocalDateTime startTime = LocalDateTime.now().minusSeconds(1);
            ExecutionResult result = ExecutionResult.skipped(
                    3L,
                    "行数检查",
                    "表不存在",
                    500L,
                    startTime
            );

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.SKIPPED);
            assertThat(result.getErrorDetail()).isEqualTo("表不存在");
            assertThat(result.getQualityScore()).isEqualTo(100);
            assertThat(result.getBlocked()).isFalse();
        }

        @Test
        @DisplayName("blocked静态工厂方法")
        void testBlockedFactory() {
            LocalDateTime startTime = LocalDateTime.now().minusSeconds(5);
            ExecutionResult result = ExecutionResult.blocked(
                    4L,
                    "主键唯一性检查",
                    "强规则触发阻塞",
                    3000L,
                    startTime
            );

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(ExecutionResult.Status.BLOCKED);
            assertThat(result.getErrorDetail()).isEqualTo("强规则触发阻塞");
            assertThat(result.getQualityScore()).isEqualTo(0);
            assertThat(result.getBlocked()).isTrue();
        }
    }

    @Nested
    @DisplayName("equals和hashCode测试")
    class EqualsHashCodeTest {
        @Test
        @DisplayName("相同属性的对象应该相等")
        void testEquals() {
            ExecutionResult result1 = ExecutionResult.builder()
                    .success(true)
                    .ruleId(1L)
                    .ruleName("test")
                    .ruleCode("TEST_001")
                    .status(ExecutionResult.Status.SUCCESS)
                    .build();

            ExecutionResult result2 = ExecutionResult.builder()
                    .success(true)
                    .ruleId(1L)
                    .ruleName("test")
                    .ruleCode("TEST_001")
                    .status(ExecutionResult.Status.SUCCESS)
                    .totalCount(100L)
                    .build();

            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("不同属性的对象不应该相等")
        void testNotEquals() {
            ExecutionResult result1 = ExecutionResult.builder()
                    .success(true)
                    .ruleId(1L)
                    .ruleName("test1")
                    .status(ExecutionResult.Status.SUCCESS)
                    .build();

            ExecutionResult result2 = ExecutionResult.builder()
                    .success(true)
                    .ruleId(2L)
                    .ruleName("test2")
                    .status(ExecutionResult.Status.SUCCESS)
                    .build();

            assertThat(result1).isNotEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("toString测试")
    class ToStringTest {
        @Test
        @DisplayName("toString应该包含关键信息")
        void testToString() {
            ExecutionResult result = ExecutionResult.builder()
                    .success(true)
                    .ruleId(1L)
                    .ruleName("test")
                    .ruleCode("NULL_CHECK_001")
                    .status(ExecutionResult.Status.SUCCESS)
                    .totalCount(1000L)
                    .passCount(950L)
                    .errorCount(50L)
                    .qualityScore(95)
                    .build();

            String str = result.toString();
            assertThat(str).contains("success=true");
            assertThat(str).contains("ruleId=1");
            assertThat(str).contains("status=SUCCESS");
        }
    }

    @Nested
    @DisplayName("Status枚举测试")
    class StatusEnumTest {
        @Test
        @DisplayName("所有状态值都存在")
        void testAllStatusValues() {
            ExecutionResult.Status[] statuses = ExecutionResult.Status.values();
            assertThat(statuses).hasSize(5);
            
            assertThat(ExecutionResult.Status.valueOf("SUCCESS")).isNotNull();
            assertThat(ExecutionResult.Status.valueOf("FAILED")).isNotNull();
            assertThat(ExecutionResult.Status.valueOf("SKIPPED")).isNotNull();
            assertThat(ExecutionResult.Status.valueOf("RUNNING")).isNotNull();
            assertThat(ExecutionResult.Status.valueOf("BLOCKED")).isNotNull();
        }
    }
}
