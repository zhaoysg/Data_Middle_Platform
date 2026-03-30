package com.bagdatahouse.dqc.engine.function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing custom rule functions
 */
@Component
public class CustomFunctionRegistry {

    private static final Logger log = LoggerFactory.getLogger(CustomFunctionRegistry.class);

    /**
     * Map: className -> RuleFunction instance
     */
    private final Map<String, RuleFunction> registeredFunctions = new ConcurrentHashMap<>();

    /**
     * Function metadata map
     */
    private final Map<String, FunctionMetadata> functionMetadata = new ConcurrentHashMap<>();

    /**
     * Initialize and register built-in functions
     */
    @PostConstruct
    public void scanAndRegister() {
        log.info("Initializing custom function registry...");
        log.info("Custom function registry initialized with {} functions", registeredFunctions.size());
    }

    /**
     * Register a custom function
     */
    public void registerFunction(String className, String description, Map<String, Object> params) {
        try {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            if (instance instanceof RuleFunction function) {
                registeredFunctions.put(className, function);
                functionMetadata.put(className, new FunctionMetadata(className, description, params));
                log.info("Registered custom function: {} - {}", className, description);
            } else {
                log.warn("Class {} does not implement RuleFunction interface", className);
            }
        } catch (ClassNotFoundException e) {
            log.error("Failed to register custom function, class not found: {}", className, e);
        } catch (Exception e) {
            log.error("Failed to register custom function: {}", className, e);
        }
    }

    /**
     * Register a RuleFunction instance directly
     */
    public void registerFunction(RuleFunction function) {
        String className = function.getClass().getName();
        registeredFunctions.put(className, function);
        Map<String, Object> params = new HashMap<>();
        Map<String, String> specParams = function.getParameterSpecs();
        if (specParams != null) {
            params.putAll(specParams);
        }
        functionMetadata.put(className, new FunctionMetadata(
                className,
                function.getDescription(),
                params
        ));
        log.info("Registered function: {} - {}", function.getName(), function.getDescription());
    }

    /**
     * Unregister a function
     */
    public void unregisterFunction(String className) {
        RuleFunction removed = registeredFunctions.remove(className);
        functionMetadata.remove(className);
        if (removed != null) {
            log.info("Unregistered function: {}", className);
        }
    }

    /**
     * Get a registered function by class name
     */
    public RuleFunction getFunction(String className) {
        return registeredFunctions.get(className);
    }

    /**
     * Get a function by name (checks registered instances)
     */
    public RuleFunction getFunctionByName(String name) {
        return registeredFunctions.values().stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get all registered functions
     */
    public Map<String, RuleFunction> getAllFunctions() {
        return Collections.unmodifiableMap(registeredFunctions);
    }

    /**
     * Get all function metadata
     */
    public Map<String, FunctionMetadata> getAllMetadata() {
        return Collections.unmodifiableMap(functionMetadata);
    }

    /**
     * Check if a function is registered
     */
    public boolean isRegistered(String className) {
        return registeredFunctions.containsKey(className);
    }

    /**
     * Get the number of registered functions
     */
    public int getFunctionCount() {
        return registeredFunctions.size();
    }

    /**
     * Clear all registered functions
     */
    public void clear() {
        registeredFunctions.clear();
        functionMetadata.clear();
        log.info("Cleared all registered functions");
    }

    /**
     * Function metadata record
     */
    public record FunctionMetadata(
            String className,
            String description,
            Map<String, Object> params
    ) {}
}
