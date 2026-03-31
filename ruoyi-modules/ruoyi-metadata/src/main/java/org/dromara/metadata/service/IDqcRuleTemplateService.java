package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.vo.DqcRuleTemplateVo;

import java.util.List;

/**
 * 数据质量规则模板服务接口
 */
public interface IDqcRuleTemplateService {

    /**
     * 分页查询模板列表 - RuoYi标准方法名
     */
    TableDataInfo<DqcRuleTemplateVo> queryPageList(DqcRuleTemplateVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询模板 - RuoYi标准方法名
     */
    DqcRuleTemplateVo queryById(Long id);

    /**
     * 查询所有模板列表
     */
    List<DqcRuleTemplateVo> listAll();

    /**
     * 分页查询模板列表
     */
    TableDataInfo<DqcRuleTemplateVo> pageTemplateList(DqcRuleTemplateVo vo, PageQuery pageQuery);

    /**
     * 根据质量维度筛选模板
     */
    List<DqcRuleTemplateVo> listByDimension(String dimension);

    /**
     * 获取模板详情
     */
    DqcRuleTemplateVo getTemplateById(Long id);
}
