package org.dromara.metadata.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.MetadataScanSchedule;
import org.dromara.metadata.domain.bo.MetadataScanScheduleBo;
import org.dromara.metadata.domain.vo.MetadataScanScheduleVo;
import org.dromara.metadata.domain.vo.MetadataScanResultVo;

import java.util.List;

/**
 * 元数据扫描调度服务接口
 */
public interface IMetadataScanScheduleService {

    /**
     * 分页查询扫描调度列表 - RuoYi标准方法名
     */
    TableDataInfo<MetadataScanScheduleVo> queryPageList(MetadataScanScheduleBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询扫描调度 - RuoYi标准方法名
     */
    MetadataScanScheduleVo queryById(Long id);

    /**
     * 新增扫描调度 - RuoYi标准方法名
     */
    Long insertByBo(MetadataScanScheduleBo bo);

    /**
     * 修改扫描调度 - RuoYi标准方法名
     */
    int updateByBo(MetadataScanScheduleBo bo);

    /**
     * 修改调度启用状态 - RuoYi标准方法名
     */
    int updateEnabled(MetadataScanScheduleBo bo);

    /**
     * 删除扫描调度 - RuoYi标准方法名
     */
    int deleteByIds(List<Long> ids);

    /**
     * 查询已启用的调度计划列表
     */
    List<MetadataScanSchedule> queryEnabledSchedules();

    /**
     * 分页查询扫描调度列表
     */
    TableDataInfo<MetadataScanScheduleVo> pageScheduleList(MetadataScanScheduleBo bo, PageQuery pageQuery);

    /**
     * 查询扫描调度详情
     */
    MetadataScanScheduleVo getScheduleById(Long id);

    /**
     * 新增扫描调度
     */
    Long insertSchedule(MetadataScanScheduleBo bo);

    /**
     * 修改扫描调度
     */
    int updateSchedule(MetadataScanScheduleBo bo);

    /**
     * 删除扫描调度
     */
    int deleteSchedule(Long[] ids);

    /**
     * 触发立即扫描
     */
    MetadataScanResultVo executeScan(Long scheduleId);

    /**
     * 查询扫描调度列表
     */
    List<MetadataScanScheduleVo> listSchedule(MetadataScanScheduleBo bo);
}
