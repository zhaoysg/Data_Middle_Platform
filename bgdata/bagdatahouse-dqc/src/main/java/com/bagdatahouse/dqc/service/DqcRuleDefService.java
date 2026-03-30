package com.bagdatahouse.dqc.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqcRuleDefDTO;
import com.bagdatahouse.core.entity.DqcRuleDef;
import com.bagdatahouse.dqc.engine.ExecutionResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DQC规则定义服务接口
 */
public interface DqcRuleDefService {

    /**
     * 分页查询
     */
    Result<Page<DqcRuleDef>> page(Integer pageNum, Integer pageSize, String ruleName, String ruleType, String applyLevel, Long targetDsId, Integer enabled);

    /**
     * 分页查询（扩展参数）
     */
    Result<Page<DqcRuleDef>> page(Integer pageNum, Integer pageSize, String ruleName, String ruleType, String applyLevel, Long targetDsId, Integer enabled, String ruleStrength, String errorLevel);

    /**
     * 根据ID查询
     */
    Result<DqcRuleDef> getById(Long id);

    /**
     * 新增
     */
    Result<Long> create(DqcRuleDefDTO dto);

    /**
     * 更新
     */
    Result<Void> update(Long id, DqcRuleDefDTO dto);

    /**
     * 删除
     */
    Result<Void> delete(Long id);

    /**
     * 启用/禁用规则
     */
    Result<Void> toggleEnabled(Long id);

    /**
     * 复制规则
     */
    Result<Void> copy(Long id);

    /**
     * 根据数据源查询规则
     */
    Result<List<DqcRuleDef>> listByDsId(Long dsId);

    /**
     * 根据模板查询规则
     */
    Result<List<DqcRuleDef>> listByTemplateId(Long templateId);

    /**
     * 执行规则
     */
    Result<ExecutionResult> execute(Long id);

    /**
     * 校验规则表达式
     */
    Result<String> validateExpression(String ruleType, String ruleExpr, String targetTable,
                                     String targetColumn, String regexPattern,
                                     BigDecimal thresholdMin, BigDecimal thresholdMax);

    /**
     * 获取规则类型选项（枚举值）
     */
    Result<Map<String, Object>> getRuleTypeOptions();

    /**
     * 获取启用的规则列表（带模板信息）
     */
    Result<List<Map<String, Object>>> listEnabledWithTemplateInfo();
}
