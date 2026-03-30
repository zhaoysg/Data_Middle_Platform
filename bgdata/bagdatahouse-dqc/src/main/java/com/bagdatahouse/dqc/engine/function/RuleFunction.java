package com.bagdatahouse.dqc.engine.function;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;

import java.util.Collections;
import java.util.Map;

/**
 * Interface for custom rule functions
 * Users can implement this interface to create custom quality check functions
 */
public interface RuleFunction {

    /**
     * Get the function name
     */
    String getName();

    /**
     * Get the function description
     */
    String getDescription();

    /**
     * Get parameter specifications
     * Key: parameter name, Value: parameter description
     */
    default Map<String, String> getParameterSpecs() {
        return Collections.emptyMap();
    }

    /**
     * Execute the function with given parameters and data source adapter
     */
    RuleFunctionResult execute(Map<String, Object> params, DataSourceAdapter adapter);

    /**
     * Result of function execution
     */
    record RuleFunctionResult(
            boolean passed,
            String actualValue,
            String errorMessage,
            Map<String, Object> details
    ) {
        /**
         * Create a successful result
         */
        public static RuleFunctionResult success(String actualValue, Map<String, Object> details) {
            return new RuleFunctionResult(true, actualValue, null, details);
        }

        /**
         * Create a failed result
         */
        public static RuleFunctionResult failed(String errorMessage, Map<String, Object> details) {
            return new RuleFunctionResult(false, null, errorMessage, details);
        }

        /**
         * Create a failed result with actual value
         */
        public static RuleFunctionResult failed(String actualValue, String errorMessage, Map<String, Object> details) {
            return new RuleFunctionResult(false, actualValue, errorMessage, details);
        }
    }
}
