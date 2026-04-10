package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.MetadataColumn;
import org.dromara.metadata.domain.bo.MetadataColumnBo;
import org.dromara.metadata.domain.vo.MetadataColumnVo;

import java.util.List;

/**
 * 元数据字段服务接口
 */
public interface IMetadataColumnService {

    /** 分页查询 */
    TableDataInfo<MetadataColumnVo> pageColumnList(MetadataColumnBo bo, PageQuery pageQuery);

    /** 根据ID查询 */
    MetadataColumnVo getColumnById(Long id);

    /** 新增或更新 */
    void upsert(MetadataColumn column);

    /** 批量新增或更新 */
    void upsertBatch(Long tableId, Long dsId, String tableName, List<MetadataColumn> columns);

    /** 更新别名 */
    int updateAlias(Long id, String alias);

    /**
     * 更新字段（别名、注释、敏感等级）
     */
    int updateColumn(MetadataColumnBo bo);

    /** 删除 */
    int deleteColumn(Long[] ids);

    /** 根据表ID查询字段列表 */
    List<MetadataColumnVo> listByTableId(Long tableId);

    /** 导出 */
    List<MetadataColumnVo> listColumn(MetadataColumnBo bo);
}
