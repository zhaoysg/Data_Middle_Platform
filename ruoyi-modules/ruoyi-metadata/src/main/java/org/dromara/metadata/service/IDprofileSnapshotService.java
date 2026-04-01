package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.bo.DprofileSnapshotBo;
import org.dromara.metadata.domain.vo.DprofileSnapshotVo;
import org.dromara.metadata.domain.vo.SnapshotCompareVo;

import java.util.List;

/**
 * 数据探查快照服务接口
 */
public interface IDprofileSnapshotService {

    /**
     * 分页查询快照列表
     */
    TableDataInfo<DprofileSnapshotVo> queryPageList(DprofileSnapshotVo vo, PageQuery pageQuery);

    /**
     * 根据ID查询快照
     */
    DprofileSnapshotVo queryById(Long id);

    /**
     * 创建快照
     * <p>
     * 保存指定数据源下所有表的探查统计信息为JSON
     *
     * @param dsId 数据源ID
     * @param name 快照名称
     * @param desc 快照描述
     * @return 新增的快照ID
     */
    Long createSnapshot(Long dsId, String name, String desc);

    /**
     * 对比两个快照
     *
     * @param snapshotId1 快照1ID
     * @param snapshotId2 快照2ID
     * @return 对比结果
     */
    SnapshotCompareVo compareSnapshot(Long snapshotId1, Long snapshotId2);

    /**
     * 根据ID删除快照
     *
     * @param id 快照ID
     */
    void deleteById(Long id);

    /**
     * 批量删除快照
     *
     * @param ids 快照ID列表
     */
    void deleteByIds(List<Long> ids);
}
