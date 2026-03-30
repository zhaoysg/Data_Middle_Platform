package com.bagdatahouse.dqc.engine.function;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Invoker for custom rule functions
 */
@Component
public class FunctionInvoker {

    private static final Logger log = LoggerFactory.getLogger(FunctionInvoker.class);

    @Autowired
    private CustomFunctionRegistry registry;

    /**
     * Invoke a custom function by class name
     */
    public RuleFunction.RuleFunctionResult invoke(String className, Map<String, Object> params, DataSourceAdapter adapter) {
        RuleFunction function = registry.getFunction(className);
        if (function == null) {
            log.warn("Function not found: {}", className);
            return RuleFunction.RuleFunctionResult.failed("Function not found: " + className, null);
        }

        try {
            log.debug("Invoking custom function: {}", function.getName());
            return function.execute(params, adapter);
        } catch (Exception e) {
            log.error("Error executing custom function: {}", className, e);
            return RuleFunction.RuleFunctionResult.failed("Execution error: " + e.getMessage(), null);
        }
    }

    /**
     * Invoke a custom function by name
     */
    public RuleFunction.RuleFunctionResult invokeByName(String name, Map<String, Object> params, DataSourceAdapter adapter) {
        RuleFunction function = registry.getFunctionByName(name);
        if (function == null) {
            log.warn("Function not found by name: {}", name);
            return RuleFunction.RuleFunctionResult.failed("Function not found: " + name, null);
        }

        try {
            log.debug("Invoking custom function: {}", function.getName());
            return function.execute(params, adapter);
        } catch (Exception e) {
            log.error("Error executing custom function by name: {}", name, e);
            return RuleFunction.RuleFunctionResult.failed("Execution error: " + e.getMessage(), null);
        }
    }

    /**
     * Check if a function is available
     */
    public boolean isFunctionAvailable(String className) {
        return registry.isRegistered(className);
    }

    /**
     * Get all available functions
     */
    public Map<String, RuleFunction> getAvailableFunctions() {
        return registry.getAllFunctions();
    }
}
