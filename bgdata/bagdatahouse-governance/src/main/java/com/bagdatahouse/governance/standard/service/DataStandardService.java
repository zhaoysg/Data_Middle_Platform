package com.bagdatahouse.governance.standard.service;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovDataStandard;
import com.bagdatahouse.governance.standard.dto.DataStandardQueryDTO;
import com.bagdatahouse.governance.standard.dto.DataStandardSaveDTO;
import com.bagdatahouse.governance.standard.vo.DataStandardBindingVO;
import com.bagdatahouse.governance.standard.vo.DataStandardDetailVO;
import com.bagdatahouse.governance.standard.vo.DataStandardStatsVO;
import com.bagdatahouse.governance.standard.vo.DataStandardVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 数据标准管理服务接口
 */
public interface DataStandardService {

    /**
     * 新增或更新标准
     */
    Result<GovDataStandard> save(DataStandardSaveDTO dto);

    /**
     * 更新标准
     */
    Result<Void> update(Long id, DataStandardSaveDTO dto);

    /**
     * 删除标准（级联删除绑定关系）
     */
    Result<Void> delete(Long id);

    /**
     * 批量删除标准
     */
    Result<Void> batchDelete(List<Long> ids);

    /**
     * 根据ID查询标准
     */
    Result<GovDataStandard> getById(Long id);

    /**
     * 分页查询标准
     */
    Result<Page<DataStandardVO>> page(Integer pageNum, Integer pageSize, DataStandardQueryDTO queryDTO);

    /**
     * 获取标准详情（含绑定列表）
     */
    Result<DataStandardDetailVO> getDetail(Long id);

    /**
     * 启用标准
     */
    Result<Void> enable(Long id);

    /**
     * 禁用标准
     */
    Result<Void> disable(Long id);

    /**
     * 发布标准
     */
    Result<Void> publish(Long id);

    /**
     * 废弃标准
     */
    Result<Void> deprecate(Long id);

    /**
     * 复制标准
     */
    Result<GovDataStandard> copy(Long id);

    /**
     * 获取统计信息
     */
    Result<DataStandardStatsVO> getStats();

    /**
     * 获取标准列表（不分页，用于下拉选择）
     */
    Result<List<DataStandardVO>> list(DataStandardQueryDTO queryDTO);

    /**
     * 绑定元数据到标准
     */
    Result<Void> bindMetadata(Long standardId, Long metadataId, String targetColumn, String enforceAction, Long createUser);

    /**
     * 批量绑定元数据到标准
     */
    Result<Void> batchBindMetadata(Long standardId, List<Long> metadataIds, String enforceAction, Long createUser);

    /**
     * 解绑元数据
     */
    Result<Void> unbindMetadata(Long bindingId);

    /**
     * 批量解绑
     */
    Result<Void> batchUnbindMetadata(List<Long> bindingIds);

    /**
     * 获取标准的绑定列表
     */
    Result<List<DataStandardBindingVO>> getBindings(Long standardId);

    /**
     * 获取元数据关联的标准列表
     */
    Result<List<DataStandardVO>> getStandardsByMetadata(Long metadataId);
}
