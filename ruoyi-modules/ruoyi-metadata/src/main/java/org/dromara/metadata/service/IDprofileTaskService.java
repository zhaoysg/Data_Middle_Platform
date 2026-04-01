package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.DprofileTaskBo;
import org.dromara.metadata.domain.vo.DprofileTaskVo;

import java.util.List;

/**
 * 数据探查任务服务接口
 */
public interface IDprofileTaskService {

    /**
     * 分页查询任务列表
     */
    TableDataInfo<DprofileTaskVo> queryPageList(DprofileTaskVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询任务
     */
    DprofileTaskVo queryById(Long id);

    /**
     * 创建任务
     *
     * @param bo 任务业务对象
     * @return 新增的任务ID
     */
    Long insertByBo(DprofileTaskBo bo);

    /**
     * 更新任务
     *
     * @param bo 任务业务对象
     */
    void updateByBo(DprofileTaskBo bo);

    /**
     * 批量删除任务
     *
     * @param ids 任务ID列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 启动任务
     * <p>
     * 设置状态为 RUNNING，触发探查执行
     *
     * @param taskId 任务ID
     */
    void startTask(Long taskId);

    /**
     * 停止任务
     * <p>
     * 设置状态为 STOPPED
     *
     * @param taskId 任务ID
     */
    void stopTask(Long taskId);

    /**
     * 同步执行任务（用于测试/手动触发）
     * <p>
     * 根据 tablePattern 获取匹配的表，逐表执行探查分析
     *
     * @param taskId 任务ID
     */
    void runTaskSync(Long taskId);
}
