package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.vo.SecClassificationVo;

import java.util.List;

/**
 * 数据分类服务接口
 */
public interface ISecClassificationService {

    /**
     * 分页查询分类列表
     */
    TableDataInfo<SecClassificationVo> queryPageList(SecClassificationVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询分类
     */
    SecClassificationVo queryById(Long id);

    /**
     * 查询所有分类列表
     */
    List<SecClassificationVo> listAll();

    /**
     * 新增分类
     */
    Long insert(SecClassificationVo vo);

    /**
     * 修改分类
     */
    int update(SecClassificationVo vo);

    /**
     * 删除分类
     */
    int deleteByIds(Long[] ids);
}
