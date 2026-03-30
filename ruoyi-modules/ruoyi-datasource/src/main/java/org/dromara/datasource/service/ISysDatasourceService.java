package org.dromara.datasource.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.connection.ConnectionTestResultVO;
import org.dromara.datasource.domain.bo.SysDatasourceBo;
import org.dromara.datasource.domain.vo.SysDatasourceVo;

import java.util.List;

/**
 * 数据源管理服务接口
 *
 * @author Lion Li
 */

public interface ISysDatasourceService {

    /**
     * 分页查询数据源列表
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 数据源分页列表
     */
    TableDataInfo<SysDatasourceVo> pageDatasourceList(SysDatasourceBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询数据源
     *
     * @param dsId 数据源ID
     * @return 数据源信息
     */
    SysDatasourceVo getDatasourceById(Long dsId);

    /**
     * 根据编码查询数据源
     *
     * @param dsCode 数据源编码
     * @return 数据源信息
     */
    SysDatasourceVo getDatasourceByCode(String dsCode);

    /**
     * 新增数据源
     *
     * @param bo 数据源信息
     * @return 新增数据源ID
     */
    Long insertDatasource(SysDatasourceBo bo);

    /**
     * 修改数据源
     *
     * @param bo 数据源信息
     * @return 结果
     */
    int updateDatasource(SysDatasourceBo bo);

    /**
     * 删除数据源
     *
     * @param dsIds 需要删除的数据源ID
     * @return 结果
     */
    int deleteDatasource(Long[] dsIds);

    /**
     * 测试数据源连接
     *
     * @param bo 数据源信息（不持久化）
     * @return 连接测试结果
     */
    ConnectionTestResultVO testConnection(SysDatasourceBo bo);

    /**
     * 修改数据源状态
     *
     * @param dsId 数据源ID
     * @param status 状态
     * @return 结果
     */
    int updateStatus(Long dsId, String status);

    /**
     * 获取已启用的数据源列表
     *
     * @return 数据源列表
     */
    List<SysDatasourceVo> listEnabledDatasource();

    /**
     * 获取数据源下的表列表
     *
     * @param dsId 数据源ID
     * @param schema Schema名称（PostgreSQL等）
     * @return 表名列表
     */
    List<String> getTables(Long dsId, String schema);

    /**
     * 获取数据源指定表的字段列表
     *
     * @param dsId 数据源ID
     * @param tableName 表名
     * @param schema Schema名称
     * @return 字段信息列表
     */
    List<?> getTableColumns(Long dsId, String tableName, String schema);

    /**
     * 导出数据源列表
     *
     * @param bo 查询条件
     * @return 数据源列表
     */
    List<SysDatasourceVo> listDatasource(SysDatasourceBo bo);
}
