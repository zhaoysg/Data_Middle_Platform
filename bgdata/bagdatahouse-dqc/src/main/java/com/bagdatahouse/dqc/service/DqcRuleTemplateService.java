package com.bagdatahouse.dqc.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.DqcRuleTemplateDTO;
import com.bagdatahouse.core.entity.DqcRuleTemplate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * DQC规则模板服务接口
 */
public interface DqcRuleTemplateService {

    /**
     * 分页查询
     */
    Result<Page<DqcRuleTemplate>> page(Integer pageNum, Integer pageSize, String templateName, String ruleType, String applyLevel, Integer builtin);

    /**
     * 根据ID查询
     */
    Result<DqcRuleTemplate> getById(Long id);

    /**
     * 新增
     */
    Result<Long> create(DqcRuleTemplateDTO dto);

    /**
     * 更新
     */
    Result<Void> update(Long id, DqcRuleTemplateDTO dto);

    /**
     * 删除
     */
    Result<Void> delete(Long id);

    /**
     * 获取全部启用的模板（用于下拉选择）
     */
    Result<List<DqcRuleTemplate>> listEnabled();

    /**
     * 获取全部模板（分类型）
     */
    Result<Map<String, List<DqcRuleTemplate>>> listGroupedByLevel();
}
