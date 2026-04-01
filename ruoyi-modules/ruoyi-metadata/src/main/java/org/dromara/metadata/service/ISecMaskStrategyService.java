package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.SecMaskStrategyBo;
import org.dromara.metadata.domain.vo.SecMaskStrategyDetailVo;
import org.dromara.metadata.domain.vo.SecMaskStrategyVo;

import java.util.List;

/**
 * 脱敏策略服务接口
 */
public interface ISecMaskStrategyService {

    /**
     * 分页查询策略列表
     */
    TableDataInfo<SecMaskStrategyVo> queryPageList(SecMaskStrategyVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询策略
     */
    SecMaskStrategyVo queryById(Long id);

    /**
     * 根据策略ID查询明细列表
     */
    List<SecMaskStrategyDetailVo> queryDetailsByStrategyId(Long strategyId);

    /**
     * 查询所有启用的策略
     */
    List<SecMaskStrategyVo> listAllEnabled();

    /**
     * 新增策略（包含明细）
     */
    Long insertWithDetails(SecMaskStrategyBo bo);

    /**
     * 修改策略（包含明细）
     */
    int updateWithDetails(SecMaskStrategyBo bo);

    /**
     * 删除策略
     */
    int deleteByIds(Long[] ids);
}
