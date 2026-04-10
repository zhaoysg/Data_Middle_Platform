package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.MetadataColumn;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.domain.bo.DqcRuleDefBo;
import org.dromara.metadata.domain.vo.DqcRuleDefVo;

import java.util.List;

/**
 * 数据质量规则定义服务接口
 * <p>
 * 支持元数据驱动的规则配置：
 * - 目标表/字段：通过 tableId、columnId 关联元数据
 * - 对比表/字段：通过 compareTableId、compareColumnId 关联元数据
 * - 数据源信息：通过元数据间接获取
 */
public interface IDqcRuleDefService {

    // ==================== RuoYi 标准方法名 ====================

    /**
     * 分页查询规则列表
     */
    TableDataInfo<DqcRuleDefVo> queryPageList(DqcRuleDefBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询规则
     */
    DqcRuleDefVo queryById(Long id);

    /**
     * 新增规则
     */
    Long insertByBo(DqcRuleDefBo bo);

    /**
     * 修改规则
     */
    int updateByBo(DqcRuleDefBo bo);

    /**
     * 删除规则
     */
    int deleteByIds(List<Long> ids);

    // ==================== 业务方法 ====================

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
     * 仅更新启用状态（列表开关，不要求完整 BO 字段）
     */
    int updateEnabled(Long id, String enabled);

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

    // ==================== 元数据查询接口 ====================

    /**
     * 获取规则关联的元数据表信息
     */
    MetadataTable getMetadataTable(Long tableId);

    /**
     * 获取规则关联的元数据字段信息
     */
    MetadataColumn getMetadataColumn(Long columnId);

    /**
     * 获取表的所有字段列表（用于前端下拉选择）
     */
    List<MetadataColumn> getTableColumns(Long tableId);

    /**
     * 获取对比表信息（跨表/跨字段规则）
     */
    MetadataTable getCompareTable(Long compareTableId);

    /**
     * 获取对比表的所有字段列表（用于前端下拉选择）
     */
    List<MetadataColumn> getCompareTableColumns(Long compareTableId);
}
