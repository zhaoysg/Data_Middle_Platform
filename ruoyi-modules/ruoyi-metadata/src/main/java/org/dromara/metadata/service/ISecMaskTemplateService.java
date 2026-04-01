package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.vo.SecMaskTemplateVo;

import java.util.List;
import java.util.Map;

/**
 * 脱敏模板服务接口
 */
public interface ISecMaskTemplateService {

    /**
     * 分页查询模板列表
     */
    TableDataInfo<SecMaskTemplateVo> queryPageList(SecMaskTemplateVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询模板
     */
    SecMaskTemplateVo queryById(Long id);

    /**
     * 查询所有启用的模板列表
     */
    List<SecMaskTemplateVo> listAllEnabled();

    /**
     * 查询所有模板列表
     */
    List<SecMaskTemplateVo> listAll();

    /**
     * 获取模板编码->表达式映射
     */
    Map<String, String> getTemplateExprMap();

    /**
     * 新增模板
     */
    Long insert(SecMaskTemplateVo vo);

    /**
     * 修改模板
     */
    int update(SecMaskTemplateVo vo);

    /**
     * 删除模板
     */
    int deleteByIds(Long[] ids);
}
