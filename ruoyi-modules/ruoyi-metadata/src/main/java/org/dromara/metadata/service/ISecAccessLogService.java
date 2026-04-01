package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.vo.SecAccessLogVo;

import java.util.List;

/**
 * 脱敏访问日志服务接口
 */
public interface ISecAccessLogService {

    /**
     * 分页查询访问日志
     */
    TableDataInfo<SecAccessLogVo> queryPageList(SecAccessLogVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询日志
     */
    SecAccessLogVo queryById(Long id);

    /**
     * 异步写入访问日志
     */
    void logAsync(Long dsId, String sql, String maskedSql, int rows,
                  List<String> maskedColumns, long elapsedMs, String status, String errorMsg,
                  Long userId, String userName, String ipAddress);

    /**
     * 查询指定用户的访问记录
     */
    List<SecAccessLogVo> queryByUserId(Long userId, int limit);

    /**
     * 查询指定数据源的访问记录
     */
    List<SecAccessLogVo> queryByDsId(Long dsId, int limit);
}
