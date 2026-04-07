package org.dromara.metadata.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.metadata.domain.GovGlossaryMapping;
import org.dromara.metadata.domain.bo.GovGlossaryMappingBo;
import org.dromara.metadata.domain.vo.GovGlossaryMappingVo;

import java.util.List;

/**
 * 治理 Glossary 映射服务接口
 */
public interface IGovGlossaryMappingService {

    /**
     * 分页查询映射列表
     */
    IPage<GovGlossaryMappingVo> queryPageList(GovGlossaryMappingBo bo, PageQuery pageQuery);

    /**
     * 查询映射详情
     */
    GovGlossaryMappingVo queryById(Long id);

    /**
     * 新增映射
     */
    Long insertByBo(GovGlossaryMappingBo bo);

    /**
     * 修改映射
     */
    int updateByBo(GovGlossaryMappingBo bo);

    /**
     * 删除映射
     */
    int deleteByIds(List<Long> ids);

    /**
     * 根据术语ID查询映射列表
     */
    List<GovGlossaryMappingVo> listByTermId(Long termId);

    /**
     * 根据数据源ID和表名查询映射列表
     */
    List<GovGlossaryMappingVo> listByTable(Long dsId, String tableName);

    /**
     * 批量创建映射
     */
    int batchInsert(List<GovGlossaryMapping> mappings);

    /**
     * 查询列表（对应 Controller）
     */
    List<GovGlossaryMappingVo> listMapping(GovGlossaryMappingBo bo);

    /**
     * 根据ID查询（对应 Controller）
     */
    GovGlossaryMappingVo getMappingById(Long id);

    /**
     * 新增（对应 Controller）
     */
    Long insertMapping(GovGlossaryMappingBo bo);

    /**
     * 修改（对应 Controller）
     */
    int updateMapping(GovGlossaryMappingBo bo);

    /**
     * 删除（对应 Controller）
     */
    int deleteMapping(Long[] ids);
}
