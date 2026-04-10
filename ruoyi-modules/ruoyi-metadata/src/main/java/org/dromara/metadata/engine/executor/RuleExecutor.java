package org.dromara.metadata.engine.executor;

import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcRuleDef;

import java.util.function.Supplier;

/**
 * Rule executor interface for DQC.
 */
public interface RuleExecutor {

    /**
     * Get executor type
     *
     * @return rule type code
     */
    String getType();

    /**
     * Execute rule check (legacy compatibility)
     *
     * @param rule    rule definition
     * @param detail  execution detail
     * @param adapter datasource adapter
     */
    default void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter) {
        execute(rule, detail, adapter, MetadataContext.of(null, null, null, null), () -> false);
    }

    /**
     * Execute rule check with cancel support
     *
     * @param rule          rule definition
     * @param detail        execution detail
     * @param adapter       datasource adapter
     * @param cancelChecker cancel checker
     */
    default void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter,
                         Supplier<Boolean> cancelChecker) {
        execute(rule, detail, adapter, MetadataContext.of(null, null, null, null), cancelChecker);
    }

    /**
     * Execute rule check (metadata-driven)
     *
     * @param rule          rule definition
     * @param detail        execution detail
     * @param adapter       datasource adapter
     * @param context       metadata context
     * @param cancelChecker cancel checker
     */
    void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter,
                 MetadataContext context, Supplier<Boolean> cancelChecker);
}
