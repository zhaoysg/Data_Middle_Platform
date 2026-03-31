package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.DataLayerBo;
import org.dromara.metadata.domain.vo.DataLayerVo;

import java.util.List;

/**
 * 数仓分层服务接口
 */
public interface IMetadataLayerService {

    /** 分页查询 */
    TableDataInfo<DataLayerVo> pageLayerList(DataLayerBo bo, PageQuery pageQuery);

    /** 根据ID查询 */
    DataLayerVo getLayerById(Long id);

    /** 新增 */
    Long insertLayer(DataLayerBo bo);

    /** 修改 */
    int updateLayer(DataLayerBo bo);

    /** 删除 */
    int deleteLayer(Long[] ids);

    /** 列表查询 */
    List<DataLayerVo> listLayer(DataLayerBo bo);

    /** 下拉框（所有分层） */
    List<DataLayerVo> listAll();

    /** 获取已启用的数仓分层列表 */
    List<DataLayerVo> listEnabled();
}
