package org.dromara.metadata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.domain.vo.SysDatasourceVo;
import org.dromara.datasource.service.ISysDatasourceService;
import org.dromara.metadata.domain.DprofileColumnReport;
import org.dromara.metadata.domain.DprofileReport;
import org.dromara.metadata.domain.DprofileTask;
import org.dromara.metadata.domain.vo.DprofileColumnReportVo;
import org.dromara.metadata.domain.vo.DprofileReportVo;
import org.dromara.metadata.mapper.DprofileColumnReportMapper;
import org.dromara.metadata.mapper.DprofileReportMapper;
import org.dromara.metadata.mapper.DprofileTaskMapper;
import org.dromara.metadata.service.IDprofileReportService;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据探查报告服务实现
 */
@Service
@RequiredArgsConstructor
public class DprofileReportServiceImpl implements IDprofileReportService {

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final DprofileReportMapper reportMapper;
    private final DprofileColumnReportMapper columnReportMapper;
    private final DprofileTaskMapper taskMapper;
    private final ISysDatasourceService datasourceService;

    @Override
    public TableDataInfo<DprofileReportVo> queryPageList(DprofileReportVo vo, PageQuery pageQuery) {
        LambdaQueryWrapper<DprofileReport> wrapper = buildQueryWrapper(vo);
        var page = reportMapper.selectVoPage(pageQuery.build(), wrapper);
        page.getRecords().forEach(this::fillExtraFields);
        return TableDataInfo.build(page);
    }

    @Override
    public DprofileReportVo queryById(Long id) {
        DprofileReportVo vo = reportMapper.selectVoById(id);
        if (vo != null) {
            fillExtraFields(vo);
        }
        return vo;
    }

    @Override
    public List<DprofileColumnReportVo> queryColumnsByReportId(Long reportId) {
        List<DprofileColumnReportVo> list = columnReportMapper.selectVoList(
            Wrappers.<DprofileColumnReport>lambdaQuery()
                .eq(DprofileColumnReport::getReportId, reportId)
                .orderByAsc(DprofileColumnReport::getId)
        );
        list.forEach(this::fillColumnExtraFields);
        return list;
    }

    private LambdaQueryWrapper<DprofileReport> buildQueryWrapper(DprofileReportVo vo) {
        LambdaQueryWrapper<DprofileReport> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(vo.getId() != null, DprofileReport::getId, vo.getId())
            .eq(vo.getTaskId() != null, DprofileReport::getTaskId, vo.getTaskId())
            .eq(vo.getDsId() != null, DprofileReport::getDsId, vo.getDsId())
            .like(StringUtils.isNotBlank(vo.getTableName()), DprofileReport::getTableName, vo.getTableName())
            .orderByDesc(DprofileReport::getCreateTime);

        if (StringUtils.isNotBlank(vo.getDsName())) {
            Set<Long> dsIds = datasourceService.listEnabledDatasource().stream()
                .filter(item -> StringUtils.isNotBlank(item.getDsName()))
                .filter(item -> StringUtils.containsIgnoreCase(item.getDsName(), vo.getDsName()))
                .map(SysDatasourceVo::getDsId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (dsIds.isEmpty()) {
                wrapper.eq(DprofileReport::getId, -1L);
            } else {
                wrapper.in(DprofileReport::getDsId, dsIds);
            }
        }

        if (StringUtils.isNotBlank(vo.getTaskName())) {
            Set<Long> taskIds = taskMapper.selectList(
                Wrappers.<DprofileTask>lambdaQuery()
                    .select(DprofileTask::getId)
                    .like(DprofileTask::getTaskName, vo.getTaskName())
            ).stream().map(DprofileTask::getId).collect(Collectors.toSet());
            if (taskIds.isEmpty()) {
                wrapper.eq(DprofileReport::getId, -1L);
            } else {
                wrapper.in(DprofileReport::getTaskId, taskIds);
            }
        }

        return wrapper;
    }

    private void fillExtraFields(DprofileReportVo vo) {
        if (vo.getTaskId() != null) {
            DprofileTask task = taskMapper.selectById(vo.getTaskId());
            if (task != null) {
                vo.setTaskName(task.getTaskName());
            }
        }
        if (vo.getDsId() != null) {
            SysDatasourceVo datasource = datasourceService.getDatasourceById(vo.getDsId());
            if (datasource != null) {
                vo.setDsName(datasource.getDsName());
                vo.setDsCode(datasource.getDsCode());
            }
        }
        if (vo.getCreateTime() != null) {
            vo.setProfileTimeText(new SimpleDateFormat(TIME_PATTERN).format(vo.getCreateTime()));
        }
    }

    private void fillColumnExtraFields(DprofileColumnReportVo vo) {
        vo.setFullColumnName(String.format("%s.%s", safe(vo.getTableName()), safe(vo.getColumnName())));
        if (vo.getNullRate() != null) {
            vo.setNullRateText(vo.getNullRate().stripTrailingZeros().toPlainString());
        }
        if (vo.getUniqueRate() != null) {
            vo.setUniqueRateText(vo.getUniqueRate().stripTrailingZeros().toPlainString());
        }
    }

    private String safe(String value) {
        return StringUtils.isBlank(value) ? "-" : value;
    }
}
