package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.metadata.domain.MetadataScanSchedule;
import org.dromara.metadata.domain.bo.MetadataScanBo;
import org.dromara.metadata.domain.bo.MetadataScanScheduleBo;
import org.dromara.metadata.domain.vo.MetadataScanScheduleVo;
import org.dromara.metadata.domain.vo.MetadataScanResultVo;
import org.dromara.metadata.mapper.MetadataScanScheduleMapper;
import org.dromara.metadata.service.IMetadataScanScheduleService;
import org.dromara.metadata.service.IMetadataScanService;
import org.dromara.metadata.support.DatasourceHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 元数据扫描调度服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class MetadataScanScheduleServiceImpl implements IMetadataScanScheduleService {

    private final MetadataScanScheduleMapper baseMapper;
    private final IMetadataScanService scanService;
    private final DatasourceHelper datasourceHelper;

    @Override
    public TableDataInfo<MetadataScanScheduleVo> queryPageList(MetadataScanScheduleBo bo, PageQuery pageQuery) {
        Wrapper<MetadataScanSchedule> wrapper = buildQueryWrapper(bo);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public MetadataScanScheduleVo queryById(Long id) {
        return MapstructUtils.convert(requireAccessibleSchedule(id), MetadataScanScheduleVo.class);
    }

    @Override
    public Long insertByBo(MetadataScanScheduleBo bo) {
        datasourceHelper.getSysDatasource(bo.getDsId());
        MetadataScanSchedule entity = MapstructUtils.convert(bo, MetadataScanSchedule.class);
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int updateByBo(MetadataScanScheduleBo bo) {
        requireAccessibleSchedule(bo.getId());
        datasourceHelper.getSysDatasource(bo.getDsId());
        MetadataScanSchedule entity = MapstructUtils.convert(bo, MetadataScanSchedule.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int updateEnabled(MetadataScanScheduleBo bo) {
        requireAccessibleSchedule(bo.getId());
        MetadataScanSchedule entity = MapstructUtils.convert(bo, MetadataScanSchedule.class);
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            requireAccessibleSchedule(id);
        }
        return baseMapper.deleteBatchIds(ids);
    }

    @Override
    public List<MetadataScanSchedule> queryEnabledSchedules() {
        return baseMapper.selectList(
            Wrappers.<MetadataScanSchedule>lambdaQuery()
                .eq(MetadataScanSchedule::getEnabled, "1")
                .orderByDesc(MetadataScanSchedule::getCreateTime)
        );
    }

    @Override
    public MetadataScanResultVo executeScan(Long scheduleId) {
        MetadataScanSchedule schedule = requireAccessibleSchedule(scheduleId);
        if (schedule.getDsId() == null) {
            throw new IllegalArgumentException("扫描调度未配置数据源");
        }

        try {
            // 触发扫描
            MetadataScanBo scanBo = new MetadataScanBo();
            scanBo.setDsId(schedule.getDsId());
            scanBo.setSyncColumn("1".equals(schedule.getSyncColumn()));
            MetadataScanResultVo result = scanService.scanByDatasource(scanBo, null);

            // 更新调度记录
            schedule.setLastScanTime(LocalDateTime.now());
            // 计算下次扫描时间（如果有Cron表达式）
            if (StringUtils.isNotBlank(schedule.getCronExpr())) {
                // 简单处理：设置1小时后
                schedule.setNextScanTime(LocalDateTime.now().plusHours(1));
            }
            baseMapper.updateById(schedule);

            return result;
        } catch (Exception e) {
            log.error("执行扫描调度失败: {}", e.getMessage());
            throw new RuntimeException("扫描执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MetadataScanScheduleVo> listSchedule(MetadataScanScheduleBo bo) {
        Wrapper<MetadataScanSchedule> wrapper = buildQueryWrapper(bo);
        return baseMapper.selectVoList(wrapper);
    }

    @Override
    public TableDataInfo<MetadataScanScheduleVo> pageScheduleList(MetadataScanScheduleBo bo, PageQuery pageQuery) {
        return queryPageList(bo, pageQuery);
    }

    @Override
    public MetadataScanScheduleVo getScheduleById(Long id) {
        return queryById(id);
    }

    @Override
    public Long insertSchedule(MetadataScanScheduleBo bo) {
        return insertByBo(bo);
    }

    @Override
    public int updateSchedule(MetadataScanScheduleBo bo) {
        return updateByBo(bo);
    }

    @Override
    public int deleteSchedule(Long[] ids) {
        return deleteByIds(List.of(ids));
    }

    private Wrapper<MetadataScanSchedule> buildQueryWrapper(MetadataScanScheduleBo bo) {
        LambdaQueryWrapper<MetadataScanSchedule> wrapper = Wrappers.lambdaQuery();
        applyAccessibleDatasourceFilter(wrapper, bo.getDsId());
        wrapper.like(StringUtils.isNotBlank(bo.getDsName()), MetadataScanSchedule::getDsName, bo.getDsName())
            .eq(StringUtils.isNotBlank(bo.getScheduleType()), MetadataScanSchedule::getScheduleType, bo.getScheduleType())
            .eq(StringUtils.isNotBlank(bo.getEnabled()), MetadataScanSchedule::getEnabled, bo.getEnabled())
            .orderByDesc(MetadataScanSchedule::getCreateTime);
        return wrapper;
    }

    private void applyAccessibleDatasourceFilter(LambdaQueryWrapper<MetadataScanSchedule> wrapper, Long dsId) {
        List<Long> accessibleDsIds = datasourceHelper.resolveAccessibleDatasourceIds(dsId);
        if (accessibleDsIds.isEmpty()) {
            wrapper.eq(MetadataScanSchedule::getId, -1L);
            return;
        }
        wrapper.in(MetadataScanSchedule::getDsId, accessibleDsIds);
    }

    private MetadataScanSchedule requireAccessibleSchedule(Long id) {
        if (id == null) {
            throw new ServiceException("扫描调度ID不能为空");
        }
        MetadataScanSchedule schedule = baseMapper.selectById(id);
        if (schedule == null) {
            throw new ServiceException("扫描调度不存在: " + id);
        }
        datasourceHelper.getSysDatasource(schedule.getDsId());
        return schedule;
    }
}
