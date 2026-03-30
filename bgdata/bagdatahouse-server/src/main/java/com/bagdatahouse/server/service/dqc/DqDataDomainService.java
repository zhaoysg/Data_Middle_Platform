package com.bagdatahouse.server.service.dqc;

import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.DqDataDomain;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 数据域服务接口
 */
public interface DqDataDomainService {

    /**
     * 分页查询数据域
     */
    Result<Page<DqDataDomain>> page(Integer pageNum, Integer pageSize, String domainName, String domainCode, Integer status);

    /**
     * 获取已启用的数据域列表
     */
    Result<List<DqDataDomain>> listEnabled();

    /**
     * 获取数据域树形结构
     */
    Result<List<DqDataDomain>> getTree();

    /**
     * 根据ID查询数据域
     */
    Result<DqDataDomain> getById(Long id);

    /**
     * 新增数据域
     */
    Result<Long> create(DqDataDomain dataDomain);

    /**
     * 更新数据域
     */
    Result<Void> update(Long id, DqDataDomain dataDomain);

    /**
     * 删除数据域
     */
    Result<Void> delete(Long id);

    /**
     * 修改数据域状态
     */
    Result<Void> updateStatus(Long id, Integer status);
}
