package org.dromara.metadata.engine.executor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.metadata.mapper.DqcExecutionDetailMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则执行器工厂
 * <p>
 * 根据规则类型获取对应的执行器实例。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RuleExecutorFactory {

    private final DqcExecutionDetailMapper detailMapper;

    private final Map<String, RuleExecutor> executorMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 注册所有执行器
        register(new NullCheckExecutor());
        register(new UniqueCheckExecutor());
        register(new RegexCheckExecutor());
        register(new ThresholdCheckExecutor());
        register(new FluctuationCheckExecutor(detailMapper));
        register(new CrossFieldCheckExecutor());
        register(new CustomSqlExecutor());
        register(new UpdateTimelinessExecutor());

        log.info("RuleExecutorFactory初始化完成，已注册{}个执行器", executorMap.size());
    }

    /**
     * 注册执行器
     */
    private void register(RuleExecutor executor) {
        executorMap.put(executor.getType().toUpperCase(), executor);
        log.debug("注册执行器: type={}", executor.getType());
    }

    /**
     * 获取执行器
     *
     * @param type 规则类型
     * @return 对应的执行器，如果不存在返回null
     */
    public RuleExecutor getExecutor(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return executorMap.get(type.toUpperCase());
    }

    /**
     * 检查执行器是否存在
     *
     * @param type 规则类型
     * @return true if executor exists
     */
    public boolean hasExecutor(String type) {
        return executorMap.containsKey(type != null ? type.toUpperCase() : null);
    }

    /**
     * 获取所有已注册的执行器类型
     *
     * @return 规则类型列表
     */
    public java.util.List<String> getRegisteredTypes() {
        return java.util.List.copyOf(executorMap.keySet());
    }
}
