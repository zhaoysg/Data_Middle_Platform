package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.GovGlossaryCategoryBo;
import org.dromara.metadata.domain.vo.GovGlossaryCategoryVo;

import java.util.List;

/**
 * 治理 Glossary 分类服务接口
 */
public interface IGovGlossaryCategoryService {

    /**
     * 分页查询分类列表 - RuoYi标准方法名
     */
    TableDataInfo<GovGlossaryCategoryVo> queryPageList(GovGlossaryCategoryBo bo, PageQuery pageQuery);

    /**
     * 获取分类选项列表 - RuoYi标准方法名
     */
    List<GovGlossaryCategoryVo> queryOptions();

    /**
     * 根据ID查询分类 - RuoYi标准方法名
     */
    GovGlossaryCategoryVo queryById(Long id);

    /**
     * 新增分类 - RuoYi标准方法名
     */
    Long insertByBo(GovGlossaryCategoryBo bo);

    /**
     * 修改分类 - RuoYi标准方法名
     */
    int updateByBo(GovGlossaryCategoryBo bo);

    /**
     * 删除分类 - RuoYi标准方法名
     */
    int deleteByIds(List<Long> ids);

    /**
     * 分页查询分类列表
     */
    TableDataInfo<GovGlossaryCategoryVo> pageCategoryList(GovGlossaryCategoryBo bo, PageQuery pageQuery);

    /**
     * 查询分类详情
     */
    GovGlossaryCategoryVo getCategoryById(Long id);

    /**
     * 新增分类
     */
    Long insertCategory(GovGlossaryCategoryBo bo);

    /**
     * 修改分类
     */
    int updateCategory(GovGlossaryCategoryBo bo);

    /**
     * 删除分类
     */
    int deleteCategory(Long[] ids);

    /**
     * 获取分类树结构
     */
    List<GovGlossaryCategoryVo> listTree();

    /**
     * 获取分类选项列表
     */
    List<GovGlossaryCategoryVo> options();

    /**
     * 查询分类列表
     */
    List<GovGlossaryCategoryVo> listCategory(GovGlossaryCategoryBo bo);
}
