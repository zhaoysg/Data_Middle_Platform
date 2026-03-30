package com.bagdatahouse.dqc.engine.executor;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.bagdatahouse.dqc.engine.RuleContext;

/**
 * Rule executor interface.
 * Implementations handle specific rule types and delegate SQL construction to SqlSyntaxHelper.
 */
public interface RuleExecutor {

    /**
     * Get the rule type this executor handles.
     * Used by RuleExecutorFactory to register the executor.
     */
    String getRuleType();

    /**
     * Execute the rule with the given context and data source adapter.
     * Implementations should use SqlSyntaxHelper for cross-database SQL syntax adaptation.
     */
    ExecutionResult execute(RuleContext context, DataSourceAdapter adapter);

    /**
     * Check if this executor supports the given rule type.
     */
    default boolean supports(String ruleType) {
        return getRuleType().equalsIgnoreCase(ruleType);
    }
}
