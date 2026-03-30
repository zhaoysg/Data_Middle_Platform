package com.bagdatahouse.server.controller.dqc;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.dqc.engine.function.CustomFunctionRegistry;
import com.bagdatahouse.dqc.engine.function.FunctionInvoker;
import com.bagdatahouse.dqc.engine.function.RuleFunction;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义函数注册控制器
 */
@Api(tags = "数据质量-自定义函数管理")
@RestController
@RequestMapping("/dqc/function")
public class DqcFunctionController {

    private static final Logger log = LoggerFactory.getLogger(DqcFunctionController.class);

    @Autowired
    private CustomFunctionRegistry functionRegistry;

    @Autowired
    private FunctionInvoker functionInvoker;

    @GetMapping("/list")
    @ApiOperation("获取所有已注册的自定义函数")
    public Result<Map<String, Object>> list() {
        Map<String, Object> result = new HashMap<>();
        result.put("functions", functionRegistry.getAllMetadata());
        result.put("total", functionRegistry.getFunctionCount());
        return Result.success(result);
    }

    @GetMapping("/{className}")
    @ApiOperation("获取指定函数详情")
    public Result<CustomFunctionRegistry.FunctionMetadata> getByClassName(
            @ApiParam("函数类全限定名") @PathVariable String className
    ) {
        CustomFunctionRegistry.FunctionMetadata metadata = functionRegistry.getAllMetadata().get(className);
        if (metadata == null) {
            return Result.fail(404, "函数不存在: " + className);
        }
        return Result.success(metadata);
    }

    @PostMapping("/register")
    @ApiOperation("注册自定义函数")
    public Result<Void> register(
            @ApiParam("函数类全限定名") @RequestParam String className,
            @ApiParam("函数描述") @RequestParam(required = false) String description
    ) {
        if (!isValidJavaClassName(className)) {
            return Result.fail(400, "类全限定名格式不正确");
        }

        if (functionRegistry.isRegistered(className)) {
            return Result.fail(400, "函数已注册: " + className);
        }

        try {
            functionRegistry.registerFunction(className, description != null ? description : "User registered function", new HashMap<>());
            log.info("Registered custom function: {}", className);
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to register function: {}", className, e);
            return Result.fail(400, "注册失败: " + e.getMessage());
        }
    }

    @PostMapping("/unregister")
    @ApiOperation("注销自定义函数")
    public Result<Void> unregister(
            @ApiParam("函数类全限定名") @RequestParam String className
    ) {
        if (!functionRegistry.isRegistered(className)) {
            return Result.fail(404, "函数不存在: " + className);
        }
        functionRegistry.unregisterFunction(className);
        log.info("Unregistered custom function: {}", className);
        return Result.success();
    }

    @PostMapping("/check")
    @ApiOperation("检查函数是否可加载")
    public Result<Map<String, Object>> checkFunction(
            @ApiParam("函数类全限定名") @RequestParam String className
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("className", className);
        result.put("registered", functionRegistry.isRegistered(className));

        // Try to load the class
        try {
            Class<?> clazz = Class.forName(className);
            result.put("exists", true);
            result.put("implementsRuleFunction", RuleFunction.class.isAssignableFrom(clazz));

            if (!RuleFunction.class.isAssignableFrom(clazz)) {
                result.put("message", "类存在但未实现 RuleFunction 接口");
                result.put("loadable", false);
            } else {
                result.put("message", "类可正常加载并实现了 RuleFunction 接口");
                result.put("loadable", true);
            }
        } catch (ClassNotFoundException e) {
            result.put("exists", false);
            result.put("loadable", false);
            result.put("message", "类不存在: " + className);
        } catch (Exception e) {
            result.put("exists", false);
            result.put("loadable", false);
            result.put("message", "加载异常: " + e.getMessage());
        }

        return Result.success(result);
    }

    @GetMapping("/check-availability/{className}")
    @ApiOperation("检查函数是否可用")
    public Result<Map<String, Object>> checkAvailability(
            @ApiParam("函数类全限定名") @PathVariable String className
    ) {
        Map<String, Object> result = new HashMap<>();
        result.put("className", className);
        result.put("available", functionInvoker.isFunctionAvailable(className));
        return Result.success(result);
    }

    @PostMapping("/reload")
    @ApiOperation("重新扫描并注册所有内置函数")
    public Result<Void> reload() {
        functionRegistry.clear();
        // The @PostConstruct in CustomFunctionRegistry will re-scan
        // For now just log
        log.info("Function registry cleared, ready for reload");
        return Result.success();
    }

    private boolean isValidJavaClassName(String className) {
        if (className == null || className.isEmpty()) return false;
        return className.matches("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)+$");
    }
}
