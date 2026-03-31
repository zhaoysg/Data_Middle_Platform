package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.DqcExecution;
import org.dromara.metadata.domain.vo.DqcExecutionDetailVo;
import org.dromara.metadata.domain.vo.DqcExecutionVo;

import java.util.List;

/**
 * 数据质量执行服务接口
 */
public interface IDqcExecutionService {

    /**
     * 分页查询执行记录列表
     */
    TableDataInfo<DqcExecutionVo> queryPageList(DqcExecutionVo vo, PageQuery pageQuery);

    /**
     * 查询执行记录详情
     */
    DqcExecutionVo queryById(Long id);

    /**
     * 执行方案
     */
    DqcExecution executePlan(Long planId, String triggerType, Long triggerUser);

    /**
     * 查询执行明细列表
     */
    List<DqcExecutionDetailVo> listDetailsByExecutionId(Long executionId);

    /**
     * 根据方案ID查询执行记录
     */
    List<DqcExecutionVo> listByPlanId(Long planId);
}
