package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.DqcRuleDefBo;
import org.dromara.metadata.domain.vo.DqcRuleDefVo;

import java.util.List;

/**
 * 数据质量规则定义服务接口
 */
public interface IDqcRuleDefService {

    /**
     * 分页查询规则列表 - RuoYi标准方法名
     */
    TableDataInfo<DqcRuleDefVo> queryPageList(DqcRuleDefBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询规则 - RuoYi标准方法名
     */
    DqcRuleDefVo queryById(Long id);

    /**
     * 新增规则 - RuoYi标准方法名
     */
    Long insertByBo(DqcRuleDefBo bo);

    /**
     * 修改规则 - RuoYi标准方法名
     */
    int updateByBo(DqcRuleDefBo bo);

    /**
     * 删除规则 - RuoYi标准方法名
     */
    int deleteByIds(List<Long> ids);

    /**
     * 分页查询规则列表
     */
    TableDataInfo<DqcRuleDefVo> pageRuleList(DqcRuleDefBo bo, PageQuery pageQuery);

    /**
     * 查询规则详情
     */
    DqcRuleDefVo getRuleById(Long id);

    /**
     * 新增规则
     */
    Long insertRule(DqcRuleDefBo bo);

    /**
     * 修改规则
     */
    int updateRule(DqcRuleDefBo bo);

    /**
     * 删除规则
     */
    int deleteRule(Long[] ids);

    /**
     * 根据方案ID查询绑定的规则
     */
    List<DqcRuleDefVo> listByPlanId(Long planId);

    /**
     * 绑定规则到方案
     */
    int bindRules(Long planId, List<Long> ruleIds);

    /**
     * 查询规则列表
     */
    List<DqcRuleDefVo> listRule(DqcRuleDefBo bo);
}
