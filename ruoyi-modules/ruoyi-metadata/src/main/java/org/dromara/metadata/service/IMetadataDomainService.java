package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.DataDomainBo;
import org.dromara.metadata.domain.vo.DataDomainVo;

import java.util.List;

/**
 * 数据域服务接口
 */
public interface IMetadataDomainService {

    /**
     * 分页查询数据域列表
     */
    TableDataInfo<DataDomainVo> pageDomainList(DataDomainBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询
     */
    DataDomainVo getDomainById(Long id);

    /**
     * 新增
     */
    Long insertDomain(DataDomainBo bo);

    /**
     * 修改
     */
    int updateDomain(DataDomainBo bo);

    /**
     * 删除
     */
    int deleteDomain(Long[] ids);

    /**
     * 列表查询
     */
    List<DataDomainVo> listDomain(DataDomainBo bo);

    /**
     * 下拉框（已启用的数据域）
     */
    List<DataDomainVo> listEnabled();

    /**
     * 查询所有（不分页）
     */
    List<DataDomainVo> listAll();
}
