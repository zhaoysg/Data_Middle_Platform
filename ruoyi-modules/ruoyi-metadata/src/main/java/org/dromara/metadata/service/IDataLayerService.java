package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.DataLayerBo;
import org.dromara.metadata.domain.vo.DataLayerVo;

import java.util.List;

/**
 * 数仓分层服务接口
 */
public interface IDataLayerService {

    /**
     * 分页查询数仓分层列表
     */
    TableDataInfo<DataLayerVo> pageLayerList(DataLayerBo bo, PageQuery pageQuery);

    /**
     * 查询所有数仓分层
     */
    List<DataLayerVo> listAll();

    /**
     * 根据ID查询数仓分层
     */
    DataLayerVo getLayerById(Long id);

    /**
     * 新增数仓分层
     */
    Long insertLayer(DataLayerBo bo);

    /**
     * 修改数仓分层
     */
    int updateLayer(DataLayerBo bo);

    /**
     * 删除数仓分层
     */
    int deleteLayer(Long[] ids);

    /**
     * 列表查询
     */
    List<DataLayerVo> listLayer(DataLayerBo bo);

    /**
     * 获取已启用的数仓分层列表
     */
    List<DataLayerVo> listEnabled();
}
