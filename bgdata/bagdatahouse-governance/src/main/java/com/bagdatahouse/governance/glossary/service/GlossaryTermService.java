package com.bagdatahouse.governance.glossary.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovGlossaryTerm;
import com.bagdatahouse.governance.glossary.dto.GlossaryTermQueryDTO;
import com.bagdatahouse.governance.glossary.dto.GlossaryTermSaveDTO;
import com.bagdatahouse.governance.glossary.vo.GlossaryMappingVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryTermDetailVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryTermVO;
import com.bagdatahouse.governance.glossary.vo.GlossaryStatsVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 术语管理服务接口
 */
public interface GlossaryTermService {

    /**
     * 新增术语
     */
    Result<GovGlossaryTerm> save(GlossaryTermSaveDTO dto);

    /**
     * 更新术语
     */
    Result<Void> update(Long id, GlossaryTermSaveDTO dto);

    /**
     * 删除术语（级联删除映射关系）
     */
    Result<Void> delete(Long id);

    /**
     * 批量删除术语
     */
    Result<Void> batchDelete(List<Long> ids);

    /**
     * 根据ID查询术语
     */
    Result<GovGlossaryTerm> getById(Long id);

    /**
     * 分页查询术语
     */
    Result<Page<GlossaryTermVO>> page(Integer pageNum, Integer pageSize, GlossaryTermQueryDTO queryDTO);

    /**
     * 获取术语详情（含映射列表）
     */
    Result<GlossaryTermDetailVO> getDetail(Long id);

    /**
     * 启用术语
     */
    Result<Void> enable(Long id);

    /**
     * 禁用术语
     */
    Result<Void> disable(Long id);

    /**
     * 发布术语
     */
    Result<Void> publish(Long id);

    /**
     * 废弃术语
     */
    Result<Void> deprecate(Long id);

    /**
     * 复制术语
     */
    Result<GovGlossaryTerm> copy(Long id);

    /**
     * 获取术语列表（不分页，用于下拉选择）
     */
    Result<List<GlossaryTermVO>> list(GlossaryTermQueryDTO queryDTO);

    /**
     * 获取统计信息
     */
    Result<GlossaryStatsVO> getStats();

    /**
     * 全局搜索术语（按名称/编码/英文名/别名模糊匹配）
     */
    Result<List<GlossaryTermVO>> search(String keyword);

    /**
     * 获取术语的映射列表
     */
    Result<List<GlossaryMappingVO>> getMappings(Long termId);

    /**
     * 批量导入术语（Excel）
     */
    Result<Map<String, Object>> importTerms(InputStream inputStream);

    /**
     * 导出术语（Excel）
     */
    byte[] exportTerms(GlossaryTermQueryDTO queryDTO);

    /**
     * 获取导入模板
     */
    byte[] getImportTemplate();
}
