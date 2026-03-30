package com.bagdatahouse.governance.glossary.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovGlossaryCategory;
import com.bagdatahouse.governance.glossary.dto.GlossaryCategoryQueryDTO;
import com.bagdatahouse.governance.glossary.dto.GlossaryCategorySaveDTO;
import com.bagdatahouse.governance.glossary.vo.GlossaryCategoryDetailVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryCategoryTreeVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryCategoryVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 术语分类管理服务接口
 */
public interface GlossaryCategoryService {

    /**
     * 新增分类
     */
    Result<GovGlossaryCategory> save(GlossaryCategorySaveDTO dto);

    /**
     * 更新分类
     */
    Result<Void> update(Long id, GlossaryCategorySaveDTO dto);

    /**
     * 删除分类（级联删除子分类和子分类下的术语关联）
     */
    Result<Void> delete(Long id);

    /**
     * 批量删除分类
     */
    Result<Void> batchDelete(List<Long> ids);

    /**
     * 根据ID查询分类
     */
    Result<GovGlossaryCategory> getById(Long id);

    /**
     * 分页查询分类
     */
    Result<Page<GlossaryCategoryVO>> page(Integer pageNum, Integer pageSize, GlossaryCategoryQueryDTO queryDTO);

    /**
     * 获取分类详情（含子分类列表）
     */
    Result<GlossaryCategoryDetailVO> getDetail(Long id);

    /**
     * 获取分类树（用于左侧树形展示）
     */
    Result<List<GlossaryCategoryTreeVO>> getTree(GlossaryCategoryQueryDTO queryDTO);

    /**
     * 获取全量分类树（不限条件）
     */
    Result<List<GlossaryCategoryTreeVO>> getFullTree();

    /**
     * 启用分类
     */
    Result<Void> enable(Long id);

    /**
     * 禁用分类
     */
    Result<Void> disable(Long id);

    /**
     * 移动分类（调整父节点）
     */
    Result<Void> move(Long id, Long newParentId);

    /**
     * 获取分类列表（不分页，用于下拉选择）
     */
    Result<List<GlossaryCategoryVO>> list(GlossaryCategoryQueryDTO queryDTO);
}
