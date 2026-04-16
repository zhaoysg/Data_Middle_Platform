package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.DqcPlan;
import org.dromara.metadata.domain.DqcPlanRule;
import org.dromara.metadata.domain.bo.DqcPlanBo;
import org.dromara.metadata.domain.bo.DqcPlanRuleBindBo;
import org.dromara.metadata.domain.vo.DqcPlanVo;

import java.util.List;

/**
 * 数据质量检查方案服务接口
 */
public interface IDqcPlanService {

    /**
     * 分页查询方案列表 - RuoYi标准方法名
     */
    TableDataInfo<DqcPlanVo> queryPageList(DqcPlanBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询方案 - RuoYi标准方法名
     */
    DqcPlanVo queryById(Long id);

    /**
     * 新增方案 - RuoYi标准方法名
     */
    Long insertByBo(DqcPlanBo bo);

    /**
     * 修改方案 - RuoYi标准方法名
     */
    int updateByBo(DqcPlanBo bo);

    /**
     * 删除方案 - RuoYi标准方法名
     */
    int deleteByIds(List<Long> ids);

    /**
     * 发布方案
     */
    int publish(Long id);

    /**
     * 停用方案
     */
    int disable(Long id);

    /**
     * 查询已发布的方案列表
     */
    List<DqcPlan> queryPublishedPlans();

    /**
     * 执行方案
     */
    int execute(Long planId);

    /**
     * 分页查询方案列表
     */
    TableDataInfo<DqcPlanVo> pagePlanList(DqcPlanBo bo, PageQuery pageQuery);

    /**
     * 查询方案详情
     */
    DqcPlanVo getPlanById(Long id);

    /**
     * 新增方案
     */
    Long insertPlan(DqcPlanBo bo);

    /**
     * 修改方案
     */
    int updatePlan(DqcPlanBo bo);

    /**
     * 删除方案
     */
    int deletePlan(Long[] ids);

    /**
     * 绑定规则到方案
     */
    int bindRules(Long planId, List<DqcPlanRuleBindBo> ruleBindings);

    /**
     * 获取方案已绑定的规则列表
     */
    List<DqcPlanRule> getBoundRules(Long planId);

    /**
     * 查询方案列表
     */
    List<DqcPlanVo> listPlan(DqcPlanBo bo);

    /**
     * 获取方案选项列表
     */
    List<DqcPlanVo> options();

    /**
     * 为新扫描的表关联默认质检方案
     *
     * @param dsId      数据源ID
     * @param tableName 表名
     * @return 绑定数量
     */
    int associateDefaultPlan(Long dsId, String tableName);
}
