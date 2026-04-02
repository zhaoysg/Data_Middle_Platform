package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.domain.bo.SysDatasourceBo;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据探查报告服务实现
 */
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class DprofileReportServiceImpl implements IDprofileReportService {

    private final DprofileReportMapper reportMapper;
    private final DprofileColumnReportMapper columnReportMapper;
    private final DprofileTaskMapper taskMapper;
    private final ISysDatasourceService datasourceService;

    @Override
    public TableDataInfo<DprofileReportVo> queryPageList(DprofileReportVo vo, PageQuery pageQuery) {
        LambdaQueryWrapper<DprofileReport> wrapper = buildQueryWrapper(vo);
        var page = reportMapper.selectVoPage(pageQuery.build(), wrapper);
        enrichReportFields(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public DprofileReportVo queryById(Long id) {
        DprofileReportVo vo = reportMapper.selectVoById(id);
        if (vo != null) {
            enrichReportFields(List.of(vo));
        }
        return vo;
    }

    @Override
    public List<DprofileColumnReportVo> queryColumnsByReportId(Long reportId) {
        List<DprofileColumnReportVo> records = columnReportMapper.selectVoList(
            Wrappers.<DprofileColumnReport>lambdaQuery()
                .eq(DprofileColumnReport::getReportId, reportId)
                .orderByAsc(DprofileColumnReport::getColumnName)
        );
        records.forEach(this::enrichColumnFields);
        return records;
    }

    private LambdaQueryWrapper<DprofileReport> buildQueryWrapper(DprofileReportVo vo) {
        LambdaQueryWrapper<DprofileReport> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ObjectUtil.isNotNull(vo.getDsId()), DprofileReport::getDsId, vo.getDsId())
            .eq(ObjectUtil.isNotNull(vo.getTaskId()), DprofileReport::getTaskId, vo.getTaskId())
            .like(StringUtils.isNotBlank(vo.getTableName()), DprofileReport::getTableName, vo.getTableName())
            .orderByDesc(DprofileReport::getCreateTime);

        if (StringUtils.isNotBlank(vo.getDsName())) {
            List<Long> matchedDsIds = findAccessibleDatasourceIdsByName(vo.getDsName());
            if (matchedDsIds.isEmpty()) {
                wrapper.eq(DprofileReport::getId, -1L);
            } else {
                wrapper.in(DprofileReport::getDsId, matchedDsIds);
            }
        }

        return wrapper;
    }

    private void enrichReportFields(List<DprofileReportVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        Set<Long> dsIds = records.stream()
            .map(DprofileReportVo::getDsId)
            .filter(ObjectUtil::isNotNull)
            .collect(Collectors.toSet());
        Map<Long, SysDatasourceVo> datasourceMap = dsIds.isEmpty()
            ? Map.of()
            : datasourceService.listDatasourceByIds(List.copyOf(dsIds)).stream()
                .filter(item -> ObjectUtil.isNotNull(item.getDsId()))
                .collect(Collectors.toMap(SysDatasourceVo::getDsId, item -> item, (left, right) -> left));

        Set<Long> taskIds = records.stream()
            .map(DprofileReportVo::getTaskId)
            .filter(ObjectUtil::isNotNull)
            .collect(Collectors.toSet());
        Map<Long, DprofileTask> taskMap = taskIds.isEmpty()
            ? Map.of()
            : taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(DprofileTask::getId, item -> item));

        records.forEach(record -> {
            SysDatasourceVo datasource = datasourceMap.get(record.getDsId());
            if (datasource != null) {
                record.setDsName(datasource.getDsName());
                record.setDsCode(datasource.getDsCode());
            }

            DprofileTask task = taskMap.get(record.getTaskId());
            if (task != null) {
                record.setTaskName(task.getTaskName());
            }
        });
    }

    private List<Long> findAccessibleDatasourceIdsByName(String dsName) {
        SysDatasourceBo bo = new SysDatasourceBo();
        bo.setDsName(dsName);
        return datasourceService.listDatasource(bo).stream()
            .map(SysDatasourceVo::getDsId)
            .filter(ObjectUtil::isNotNull)
            .toList();
    }

    private void enrichColumnFields(DprofileColumnReportVo column) {
        if (column == null) {
            return;
        }
        column.setFullColumnName(column.getTableName() + "." + column.getColumnName());
        column.setNullRateText(formatRate(column.getNullRate()));
        column.setUniqueRateText(formatRate(column.getUniqueRate()));
    }

    private String formatRate(BigDecimal rate) {
        if (rate == null) {
            return "0%";
        }
        return rate.multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString() + "%";
    }
}
