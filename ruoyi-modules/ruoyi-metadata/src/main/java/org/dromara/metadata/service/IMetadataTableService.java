package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.MetadataTable;
import org.dromara.metadata.domain.bo.MetadataTableBo;
import org.dromara.metadata.domain.vo.MetadataTableVo;

import java.util.List;

/**
 * 元数据表服务接口
 */
public interface IMetadataTableService {

    /**
     * 分页查询元数据表列表
     */
    TableDataInfo<MetadataTableVo> pageTableList(MetadataTableBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询元数据表
     */
    MetadataTableVo getTableById(Long id);

    /**
     * 新增元数据表
     */
    Long insertTable(MetadataTableBo bo);

    /**
     * 修改元数据表
     */
    int updateTable(MetadataTableBo bo);

    /**
     * 删除元数据表
     */
    int deleteTable(Long[] ids);

    /**
     * 根据数据源ID查询表列表
     */
    List<MetadataTableVo> listByDsId(Long dsId);

    /**
     * 导出元数据表列表
     */
    List<MetadataTableVo> listTable(MetadataTableBo bo);

    /**
     * 查询已使用的标签选项
     */
    List<String> listTagOptions();

    /**
     * 新增或更新（ON DUPLICATE KEY UPDATE）
     */
    void upsert(MetadataTable table);

    /**
     * 批量新增或更新
     */
    void upsertBatch(List<MetadataTable> tables);

    /**
     * 根据数据源ID和表名查询
     */
    MetadataTable getByDsIdAndTableName(Long dsId, String tableName);

    /**
     * 更新别名
     */
    int updateAlias(Long id, String alias);

    /**
     * 更新状态
     */
    int updateStatus(Long id, String status);
}
