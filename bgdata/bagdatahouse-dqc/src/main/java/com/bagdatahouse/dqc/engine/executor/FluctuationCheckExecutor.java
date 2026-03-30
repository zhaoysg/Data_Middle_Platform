package com.bagdatahouse.dqc.engine.executor;

import com.bagdatahouse.core.entity.DqcQualityScore;
import com.bagdatahouse.core.mapper.DqcQualityScoreMapper;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.bagdatahouse.dqc.engine.RuleContext;
import com.bagdatahouse.dqc.engine.util.SqlSyntaxHelper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Executor for ROW_COUNT_FLUCTUATION and FLUCTUATION rule types.
 * Uses SqlSyntaxHelper for cross-database SQL syntax adaptation.
 */
@Component
public class FluctuationCheckExecutor implements RuleExecutor {

    private static final Logger log = LoggerFactory.getLogger(FluctuationCheckExecutor.class);

    private static final BigDecimal DEFAULT_FLUCTUATION_THRESHOLD = BigDecimal.valueOf(20.0);

    @Autowired
    private DqcQualityScoreMapper qualityScoreMapper;

    @Override
    public String getRuleType() {
        return "ROW_COUNT_FLUCTUATION";
    }

    @Override
    public ExecutionResult execute(RuleContext context, DataSourceAdapter adapter) {
        LocalDateTime startTime = LocalDateTime.now();
        String ruleType = context.getRuleType().toUpperCase();

        try {
            String tableName = SqlSyntaxHelper.validateTableName(context.getTargetTable());

            Long currentCount = getRowCount(adapter, tableName);
            Long previousCount = getPreviousCount(
                    context.getTargetDsId(),
                    tableName,
                    context.getTargetColumn());

            BigDecimal fluctuationThreshold = context.getFluctuationThreshold() != null ?
                    context.getFluctuationThreshold() : DEFAULT_FLUCTUATION_THRESHOLD;

            String sql = SqlSyntaxHelper.buildRowCountSql(adapter, tableName);

            if (previousCount == null || previousCount == 0) {
                return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                        currentCount, currentCount, 0L, BigDecimal.ZERO, 100,
                        getElapsedMs(startTime), startTime);
            }

            BigDecimal fluctuationPct = calculateFluctuationPercentage(currentCount, previousCount);

            boolean passed = fluctuationPct.compareTo(fluctuationThreshold) <= 0;
            String errorDetail = null;
            int qualityScore;
            Long errorCount = passed ? 0L : 1L;
            Long passCount = passed ? currentCount : 0L;

            if (passed) {
                qualityScore = 100;
            } else {
                qualityScore = 0;
                errorDetail = String.format(
                        "Row count fluctuation %.2f%% exceeds threshold %.2f%% (current: %d, previous: %d)",
                        fluctuationPct, fluctuationThreshold, currentCount, previousCount);
            }

            if (passed) {
                return ExecutionResult.success(context.getRuleId(), context.getRuleName(), sql,
                        currentCount, passCount, errorCount, fluctuationPct, qualityScore,
                        getElapsedMs(startTime), startTime);
            } else {
                return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), sql,
                        errorDetail, getElapsedMs(startTime), startTime);
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for fluctuation check: {}", e.getMessage());
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Invalid parameters: " + e.getMessage(), getElapsedMs(startTime), startTime);
        } catch (Exception e) {
            log.error("Error executing fluctuation check rule: {}", context.getRuleId(), e);
            return ExecutionResult.failed(context.getRuleId(), context.getRuleName(), null,
                    "Execution error: " + e.getMessage(), getElapsedMs(startTime), startTime);
        }
    }

    private Long getRowCount(DataSourceAdapter adapter, String tableName) {
        String sql = SqlSyntaxHelper.buildRowCountSql(adapter, tableName);
        log.debug("Executing row count query: {}", sql);
        List<Map<String, Object>> results = adapter.executeQuery(sql);
        if (results != null && !results.isEmpty()) {
            Object value = results.get(0).get("rowCount");
            return extractLongValue(value);
        }
        return 0L;
    }

    private Long getPreviousCount(Long targetDsId, String tableName, String columnName) {
        try {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            String checkDateStr = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            LambdaQueryWrapper<DqcQualityScore> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DqcQualityScore::getTargetDsId, targetDsId)
                    .eq(DqcQualityScore::getTargetTable, tableName)
                    .le(DqcQualityScore::getCheckDate, yesterday)
                    .orderByDesc(DqcQualityScore::getCheckDate)
                    .last("LIMIT 1");

            List<DqcQualityScore> scores = qualityScoreMapper.selectList(wrapper);
            if (scores != null && !scores.isEmpty()) {
                DqcQualityScore lastScore = scores.get(0);
                if (lastScore.getOverallScore() != null) {
                    return lastScore.getOverallScore().multiply(BigDecimal.valueOf(100)).longValue();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get previous count for dsId={}, table={}", targetDsId, tableName, e);
        }
        return null;
    }

    private BigDecimal calculateFluctuationPercentage(Long currentCount, Long previousCount) {
        if (previousCount == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal diff = BigDecimal.valueOf(Math.abs(currentCount - previousCount));
        BigDecimal previous = BigDecimal.valueOf(previousCount);
        return diff.divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Long extractLongValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private long getElapsedMs(LocalDateTime startTime) {
        return System.currentTimeMillis() - startTime.toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli();
    }
}
