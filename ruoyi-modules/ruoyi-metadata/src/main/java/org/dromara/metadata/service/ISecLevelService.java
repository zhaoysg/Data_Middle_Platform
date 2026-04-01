package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.vo.SecLevelVo;

import java.util.List;

/**
 * 敏感等级服务接口
 */
public interface ISecLevelService {

    /**
     * 分页查询等级列表
     */
    TableDataInfo<SecLevelVo> queryPageList(SecLevelVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询等级
     */
    SecLevelVo queryById(Long id);

    /**
     * 查询所有等级列表
     */
    List<SecLevelVo> listAll();

    /**
     * 新增等级
     */
    Long insert(SecLevelVo vo);

    /**
     * 修改等级
     */
    int update(SecLevelVo vo);

    /**
     * 删除等级
     */
    int deleteByIds(Long[] ids);
}
