package com.bagdatahouse.dqc.engine;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.dqc.engine.executor.RuleExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory for managing and retrieving rule executors
 */
@Component
public class RuleExecutorFactory {

    private static final Logger log = LoggerFactory.getLogger(RuleExecutorFactory.class);

    private final Map<String, RuleExecutor> executorMap = new HashMap<>();

    @Autowired
    public RuleExecutorFactory(List<RuleExecutor> executors) {
        for (RuleExecutor executor : executors) {
            executorMap.put(executor.getRuleType().toUpperCase(), executor);
            log.info("Registered rule executor: {} -> {}", executor.getRuleType(), executor.getClass().getSimpleName());
        }
        log.info("RuleExecutorFactory initialized with {} executors", executorMap.size());
    }

    /**
     * Get executor for the given rule type
     */
    public RuleExecutor getExecutor(String ruleType) {
        RuleExecutor executor = executorMap.get(ruleType.toUpperCase());
        if (executor == null) {
            throw new IllegalArgumentException("Unsupported rule type: " + ruleType);
        }
        return executor;
    }

    /**
     * Check if an executor exists for the given rule type
     */
    public boolean hasExecutor(String ruleType) {
        return executorMap.containsKey(ruleType.toUpperCase());
    }

    /**
     * Get all registered rule types
     */
    public Map<String, String> getRegisteredExecutors() {
        Map<String, String> result = new HashMap<>();
        executorMap.forEach((type, executor) ->
            result.put(type, executor.getClass().getSimpleName()));
        return result;
    }
}
