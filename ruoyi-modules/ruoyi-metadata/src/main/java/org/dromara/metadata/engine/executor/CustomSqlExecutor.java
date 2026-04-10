package org.dromara.metadata.engine.executor;

import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcRuleDef;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 自定义SQL执行器
 * <p>
 * 规则类型: CUSTOM_SQL
 * <p>
 * 执行用户自定义的SQL语句进行数据质量检查。
 * <p>
 * 元数据驱动：使用 MetadataContext 获取表名、字段名
 */
@Slf4j
public class CustomSqlExecutor extends AbstractRuleExecutor {

    public static final String TYPE = "CUSTOM_SQL";

    @Override
    protected String getRuleType() {
        return TYPE;
    }

    @Override
    public void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter) {
        execute(rule, detail, adapter, MetadataContext.of(null, null, null, null), () -> false);
    }

    @Override
    public void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter,
                        Supplier<Boolean> cancelChecker) {
        execute(rule, detail, adapter, MetadataContext.of(null, null, null, null), cancelChecker);
    }

    @Override
    public void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter,
                        MetadataContext context, Supplier<Boolean> cancelChecker) {
        long start = System.currentTimeMillis();

        // 渲染SQL（使用元数据上下文）
        String sql = renderSql(rule, adapter, context);
        CustomSqlSecuritySupport.validateRuleExpr(sql);
        detail.setExecuteSql(CustomSqlSecuritySupport.REDACTED_SQL);

        try {
            // 使用可取消的查询执行
            List<Map<String, Object>> resultSet = adapter.executeQueryCancellable(sql, cancelChecker);
            Object result = extractResultValue(resultSet);

            if (!CustomSqlSecuritySupport.isAllowedResultType(result)) {
                throw new IllegalArgumentException(CustomSqlSecuritySupport.RESULT_TYPE_ERROR);
            }

            detail.setActualValue(null);
            detail.setElapsedMs(System.currentTimeMillis() - start);

            var evaluation = evaluateResult(result, rule);
            detail.setPassFlag(evaluation.pass() ? "1" : "0");
            detail.setResultValue(evaluation.resultValue());
            detail.setThresholdValue(evaluation.thresholdValue() != null ? evaluation.thresholdValue().toPlainString() : null);

            if (!evaluation.pass()) {
                detail.setErrorLevel(rule.getErrorLevel());
                detail.setErrorMsg(evaluation.message());
            }

        } catch (SQLException e) {
            if ("查询已被取消".equals(e.getMessage())) {
                detail.setElapsedMs(System.currentTimeMillis() - start);
                detail.setPassFlag("0");
                detail.setErrorLevel(rule.getErrorLevel());
                detail.setErrorMsg("查询被用户取消");
            } else {
                log.error("CUSTOM_SQL执行失败: ruleId={}, error={}", rule.getId(), e.getMessage());
                detail.setElapsedMs(System.currentTimeMillis() - start);
                detail.setPassFlag("0");
                detail.setErrorLevel(rule.getErrorLevel());
                detail.setErrorMsg(CustomSqlSecuritySupport.EXECUTION_ERROR);
            }
        } catch (IllegalArgumentException e) {
            log.warn("CUSTOM_SQL校验失败: ruleId={}, error={}", rule.getId(), e.getMessage());
            detail.setElapsedMs(System.currentTimeMillis() - start);
            detail.setPassFlag("0");
            detail.setErrorLevel(rule.getErrorLevel());
            detail.setErrorMsg(e.getMessage());
        } catch (Exception e) {
            log.error("CUSTOM_SQL执行失败: ruleId={}, error={}", rule.getId(), e.getMessage());
            detail.setElapsedMs(System.currentTimeMillis() - start);
            detail.setPassFlag("0");
            detail.setErrorLevel(rule.getErrorLevel());
            detail.setErrorMsg(CustomSqlSecuritySupport.EXECUTION_ERROR);
        }
    }
}