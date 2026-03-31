package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.GovGlossaryTermBo;
import org.dromara.metadata.domain.vo.GovGlossaryTermVo;

import java.util.List;

/**
 * 治理 Glossary 术语服务接口
 */
public interface IGovGlossaryTermService {

    /**
     * 分页查询术语列表 - RuoYi标准方法名
     */
    TableDataInfo<GovGlossaryTermVo> queryPageList(GovGlossaryTermBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询术语 - RuoYi标准方法名
     */
    GovGlossaryTermVo queryById(Long id);

    /**
     * 新增术语 - RuoYi标准方法名
     */
    Long insertByBo(GovGlossaryTermBo bo);

    /**
     * 修改术语 - RuoYi标准方法名
     */
    int updateByBo(GovGlossaryTermBo bo);

    /**
     * 删除术语 - RuoYi标准方法名
     */
    int deleteByIds(List<Long> ids);

    /**
     * 分页查询术语列表
     */
    TableDataInfo<GovGlossaryTermVo> pageTermList(GovGlossaryTermBo bo, PageQuery pageQuery);

    /**
     * 查询术语详情
     */
    GovGlossaryTermVo getTermById(Long id);

    /**
     * 新增术语
     */
    Long insertTerm(GovGlossaryTermBo bo);

    /**
     * 修改术语
     */
    int updateTerm(GovGlossaryTermBo bo);

    /**
     * 删除术语
     */
    int deleteTerm(Long[] ids);

    /**
     * 发布术语
     */
    int publish(Long id);

    /**
     * 根据关键词搜索术语
     */
    List<GovGlossaryTermVo> listByKeyword(String keyword);

    /**
     * 查询术语列表
     */
    List<GovGlossaryTermVo> listTerm(GovGlossaryTermBo bo);
}
