package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.vo.SecColumnSensitivityVo;

import java.util.List;

/**
 * 字段敏感记录服务接口
 */
public interface ISecColumnSensitivityService {

    /**
     * 分页查询敏感字段列表
     */
    TableDataInfo<SecColumnSensitivityVo> queryPageList(SecColumnSensitivityVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询敏感字段
     */
    SecColumnSensitivityVo queryById(Long id);

    /**
     * 自动扫描数据源的敏感字段
     */
    int scanColumns(Long dsId);

    /**
     * 确认敏感字段
     */
    int confirmColumns(List<Long> ids);

    /**
     * 新增敏感字段记录
     */
    Long insert(SecColumnSensitivityVo vo);

    /**
     * 修改敏感字段记录
     */
    int update(SecColumnSensitivityVo vo);

    /**
     * 删除敏感字段记录
     */
    int deleteByIds(Long[] ids);
}
