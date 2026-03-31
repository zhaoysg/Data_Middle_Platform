package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.MetadataCatalog;
import org.dromara.metadata.domain.bo.MetadataCatalogBo;
import org.dromara.metadata.domain.vo.MetadataCatalogVo;

import java.util.List;

/**
 * 资产目录服务接口
 */
public interface IMetadataCatalogService {

    /** 分页查询 */
    TableDataInfo<MetadataCatalogVo> pageCatalogList(MetadataCatalogBo bo, PageQuery pageQuery);

    /** 根据ID查询 */
    MetadataCatalogVo getCatalogById(Long id);

    /** 新增 */
    Long insertCatalog(MetadataCatalogBo bo);

    /** 修改 */
    int updateCatalog(MetadataCatalogBo bo);

    /** 删除 */
    int deleteCatalog(Long[] ids);

    /** 查询树形列表 */
    List<MetadataCatalogVo> listTree();

    /** 构建树形 */
    List<MetadataCatalogVo> buildTree(List<MetadataCatalogVo> list);

    /** 查询所有启用的目录（下拉框用） */
    List<MetadataCatalogVo> listEnabled();
}
