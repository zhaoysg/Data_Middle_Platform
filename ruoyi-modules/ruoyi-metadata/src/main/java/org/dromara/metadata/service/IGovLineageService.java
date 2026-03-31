package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.GovLineageBo;
import org.dromara.metadata.domain.vo.GovLineageVo;

import java.util.List;

/**
 * 治理血缘关系服务接口
 */
public interface IGovLineageService {

    /**
     * 分页查询血缘关系列表 - RuoYi标准方法名
     */
    TableDataInfo<GovLineageVo> queryPageList(GovLineageBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询血缘关系 - RuoYi标准方法名
     */
    GovLineageVo queryById(Long id);

    /**
     * 新增血缘关系 - RuoYi标准方法名
     */
    Long insertByBo(GovLineageBo bo);

    /**
     * 修改血缘关系 - RuoYi标准方法名
     */
    int updateByBo(GovLineageBo bo);

    /**
     * 删除血缘关系 - RuoYi标准方法名
     */
    int deleteByIds(List<Long> ids);

    /**
     * 分页查询血缘关系列表
     */
    TableDataInfo<GovLineageVo> pageLineageList(GovLineageBo bo, PageQuery pageQuery);

    /**
     * 查询血缘关系详情
     */
    GovLineageVo getLineageById(Long id);

    /**
     * 新增血缘关系
     */
    Long insertLineage(GovLineageBo bo);

    /**
     * 修改血缘关系
     */
    int updateLineage(GovLineageBo bo);

    /**
     * 删除血缘关系
     */
    int deleteLineage(Long[] ids);

    /**
     * 查询上游血缘
     */
    List<GovLineageVo> listUpstream(Long dsId, String tableName, int depth);

    /**
     * 查询下游血缘
     */
    List<GovLineageVo> listDownstream(Long dsId, String tableName, int depth);

    /**
     * 查询血缘关系列表
     */
    List<GovLineageVo> listLineage(GovLineageBo bo);
}
