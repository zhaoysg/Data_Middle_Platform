package org.dromara.metadata.engine.executor;

import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DqcExecutionDetail;
import org.dromara.metadata.domain.DqcRuleDef;

import java.util.function.Supplier;

/**
 * 数据质量规则执行器接口
 */
public interface RuleExecutor {

    /**
     * 获取执行器类型
     *
     * @return 规则类型编码
     */
    String getType();

    /**
     * 执行规则检查
     *
     * @param rule 规则定义
     * @param detail 执行明细（用于存储结果）
     * @param adapter 数据源适配器
     */
    void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter);

    /**
     * 执行规则检查（支持取消）
     * @param rule 规则定义
     * @param detail 执行明细
     * @param adapter 数据源适配器
     * @param cancelChecker 取消检查器，返回true表示应中断执行
     */
    default void execute(DqcRuleDef rule, DqcExecutionDetail detail, DataSourceAdapter adapter,
                         Supplier<Boolean> cancelChecker) {
        execute(rule, detail, adapter);
    }
}
