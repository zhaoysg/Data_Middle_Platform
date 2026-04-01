package org.dromara.metadata.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.metadata.domain.DprofileReport;
import org.dromara.metadata.domain.DprofileSnapshot;
import org.dromara.metadata.domain.bo.DprofileSnapshotBo;
import org.dromara.metadata.domain.vo.*;
import org.dromara.metadata.mapper.DprofileReportMapper;
import org.dromara.metadata.mapper.DprofileSnapshotMapper;
import org.dromara.metadata.service.IDprofileSnapshotService;
import org.dromara.metadata.support.DatasourceHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据探查快照服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DprofileSnapshotServiceImpl implements IDprofileSnapshotService {

    private final DprofileSnapshotMapper snapshotMapper;
    private final DprofileReportMapper reportMapper;
    private final DatasourceHelper datasourceHelper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public TableDataInfo<DprofileSnapshotVo> queryPageList(DprofileSnapshotVo vo, PageQuery pageQuery) {
        Wrapper<DprofileSnapshot> wrapper = buildQueryWrapper(vo);
        var page = snapshotMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public DprofileSnapshotVo queryById(Long id) {
        return snapshotMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSnapshot(Long dsId, String name, String desc) {
        log.info("开始创建快照: dsId={}, name={}", dsId, name);

        DataSourceAdapter adapter = datasourceHelper.getAdapter(dsId);
        List<String> tables = adapter.getTables();

        Map<String, SnapshotCompareVo.TableSnapshotData> snapshotDataMap = new LinkedHashMap<>();
        int processedCount = 0;

        for (String tableName : tables) {
            try {
                TableStats stats = getTableStatsFromAdapter(adapter, tableName);
                Map<String, SnapshotCompareVo.ColumnSnapshotData> columnsMap = new LinkedHashMap<>();

                // Get column stats
                List<ColumnStats> columnStats = getColumnStatsFromAdapter(adapter, tableName);
                for (ColumnStats cs : columnStats) {
                    columnsMap.put(cs.columnName(), new SnapshotCompareVo.ColumnSnapshotData(
                        cs.columnName(),
                        cs.dataType(),
                        cs.nullCount(),
                        cs.nullRate(),
                        cs.uniqueCount(),
                        cs.uniqueRate()
                    ));
                }

                SnapshotCompareVo.TableSnapshotData tableData = new SnapshotCompareVo.TableSnapshotData(
                    tableName,
                    stats.rowCount(),
                    stats.columnCount(),
                    stats.lastModified(),
                    columnsMap
                );

                snapshotDataMap.put(tableName, tableData);
                processedCount++;
            } catch (Exception e) {
                log.warn("获取表 {} 的统计信息失败: {}", tableName, e.getMessage());
            }
        }

        // Create snapshot entity
        DprofileSnapshot snapshot = new DprofileSnapshot();
        snapshot.setSnapshotCode(generateSnapshotCode());
        snapshot.setSnapshotName(name);
        snapshot.setSnapshotDesc(desc);
        snapshot.setDsId(dsId);
        snapshot.setSnapshotData(JSON.toJSONString(snapshotDataMap));
        snapshot.setTableCount(snapshotDataMap.size());

        snapshotMapper.insert(snapshot);

        log.info("快照创建完成: id={}, code={}, name={}, tableCount={}",
            snapshot.getId(), snapshot.getSnapshotCode(), name, snapshotDataMap.size());

        return snapshot.getId();
    }

    @Override
    public SnapshotCompareVo compareSnapshot(Long snapshotId1, Long snapshotId2) {
        DprofileSnapshot snap1 = snapshotMapper.selectById(snapshotId1);
        DprofileSnapshot snap2 = snapshotMapper.selectById(snapshotId2);

        if (snap1 == null) {
            throw new ServiceException("快照1不存在: " + snapshotId1);
        }
        if (snap2 == null) {
            throw new ServiceException("快照2不存在: " + snapshotId2);
        }

        SnapshotCompareVo result = new SnapshotCompareVo();

        // Parse snapshot data
        Map<String, SnapshotCompareVo.TableSnapshotData> data1 = parseSnapshotData(snap1.getSnapshotData());
        Map<String, SnapshotCompareVo.TableSnapshotData> data2 = parseSnapshotData(snap2.getSnapshotData());

        Set<String> tables1 = data1.keySet();
        Set<String> tables2 = data2.keySet();

        // Find new tables (in snap2 but not in snap1)
        Set<String> newTables = new HashSet<>(tables2);
        newTables.removeAll(tables1);
        result.setNewTables(new ArrayList<>(newTables));

        // Find removed tables (in snap1 but not in snap2)
        Set<String> removedTables = new HashSet<>(tables1);
        removedTables.removeAll(tables2);
        result.setRemovedTables(new ArrayList<>(removedTables));

        // Find changed tables
        Set<String> commonTables = new HashSet<>(tables1);
        commonTables.retainAll(tables2);

        List<SnapshotCompareVo.TableChange> changes = new ArrayList<>();
        for (String tableName : commonTables) {
            SnapshotCompareVo.TableSnapshotData t1 = data1.get(tableName);
            SnapshotCompareVo.TableSnapshotData t2 = data2.get(tableName);

            SnapshotCompareVo.TableChange change = compareTableData(tableName, t1, t2);
            if (change != null) {
                changes.add(change);
            }
        }
        result.setChangedTables(changes);

        // Set snapshot info
        result.setSnapshot1(new SnapshotCompareVo.SnapshotInfo(
            snap1.getId(),
            snap1.getSnapshotCode(),
            snap1.getSnapshotName(),
            formatDateTime(snap1.getCreateTime()),
            data1.size(),
            data1
        ));
        result.setSnapshot2(new SnapshotCompareVo.SnapshotInfo(
            snap2.getId(),
            snap2.getSnapshotCode(),
            snap2.getSnapshotName(),
            formatDateTime(snap2.getCreateTime()),
            data2.size(),
            data2
        ));

        log.info("快照对比完成: snap1={}, snap2={}, newTables={}, removedTables={}, changedTables={}",
            snapshotId1, snapshotId2, result.getNewTables().size(),
            result.getRemovedTables().size(), result.getChangedTables().size());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        snapshotMapper.deleteById(id);
        log.info("删除快照: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("删除的ID列表不能为空");
        }
        snapshotMapper.deleteBatchIds(ids);
        log.info("批量删除快照: ids={}", ids);
    }

    private SnapshotCompareVo.TableChange compareTableData(
            String tableName,
            SnapshotCompareVo.TableSnapshotData t1,
            SnapshotCompareVo.TableSnapshotData t2) {

        List<SnapshotCompareVo.ColumnChange> columnChanges = new ArrayList<>();
        boolean hasChanges = false;

        // Compare row count
        Long rowCountBefore = t1.getRowCount();
        Long rowCountAfter = t2.getRowCount();
        Long rowCountChange = rowCountAfter - rowCountBefore;

        // Compare column count
        Integer colCountBefore = t1.getColumnCount();
        Integer colCountAfter = t2.getColumnCount();

        // Compare if there are meaningful changes
        if (!Objects.equals(rowCountBefore, rowCountAfter) ||
            !Objects.equals(colCountBefore, colCountAfter)) {
            hasChanges = true;
        }

        // Compare columns
        if (t1.getColumns() != null && t2.getColumns() != null) {
            Set<String> cols1 = t1.getColumns().keySet();
            Set<String> cols2 = t2.getColumns().keySet();

            Set<String> commonCols = new HashSet<>(cols1);
            commonCols.retainAll(cols2);

            for (String colName : commonCols) {
                SnapshotCompareVo.ColumnSnapshotData c1 = t1.getColumns().get(colName);
                SnapshotCompareVo.ColumnSnapshotData c2 = t2.getColumns().get(colName);

                if (!Objects.equals(c1.getNullRate(), c2.getNullRate()) ||
                    !Objects.equals(c1.getUniqueCount(), c2.getUniqueCount())) {

                    columnChanges.add(new SnapshotCompareVo.ColumnChange(
                        colName,
                        SnapshotCompareVo.ChangeType.MODIFIED,
                        c1.getNullRate(),
                        c2.getNullRate(),
                        c1.getUniqueCount(),
                        c2.getUniqueCount()
                    ));
                    hasChanges = true;
                }
            }
        }

        if (!hasChanges) {
            return null;
        }

        String description = buildChangeDescription(rowCountBefore, rowCountAfter,
            colCountBefore, colCountAfter, columnChanges.size());

        return new SnapshotCompareVo.TableChange(
            tableName,
            SnapshotCompareVo.ChangeType.MODIFIED,
            description,
            rowCountBefore,
            rowCountAfter,
            rowCountChange,
            colCountBefore,
            colCountAfter,
            columnChanges
        );
    }

    private String buildChangeDescription(Long rowBefore, Long rowAfter,
            Integer colBefore, Integer colAfter, int colChangeCount) {
        List<String> parts = new ArrayList<>();

        if (!Objects.equals(rowBefore, rowAfter)) {
            long diff = rowAfter - rowBefore;
            String sign = diff >= 0 ? "+" : "";
            parts.add("行数: " + rowBefore + " -> " + rowAfter + " (" + sign + diff + ")");
        }

        if (!Objects.equals(colBefore, colAfter)) {
            int diff = colAfter - colBefore;
            String sign = diff >= 0 ? "+" : "";
            parts.add("列数: " + colBefore + " -> " + colAfter + " (" + sign + diff + ")");
        }

        if (colChangeCount > 0) {
            parts.add("列统计变化: " + colChangeCount + " 列");
        }

        return String.join(", ", parts);
    }

    private Map<String, SnapshotCompareVo.TableSnapshotData> parseSnapshotData(String json) {
        if (StringUtils.isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return JSON.parseObject(json, LinkedHashMap.class);
        } catch (Exception e) {
            log.error("解析快照数据失败: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private TableStats getTableStatsFromAdapter(DataSourceAdapter adapter, String tableName) {
        long rowCount = adapter.getRowCount(tableName);
        var columns = adapter.getColumns(tableName);
        String comment = adapter.getTableComment(tableName);
        var lastModified = adapter.getTableLastUpdateTime(tableName);

        return new TableStats(
            tableName,
            rowCount,
            columns.size(),
            null,
            comment,
            lastModified.orElse(null)
        );
    }

    private List<ColumnStats> getColumnStatsFromAdapter(DataSourceAdapter adapter, String tableName) {
        var columns = adapter.getColumns(tableName);
        List<ColumnStats> result = new ArrayList<>();

        for (var col : columns) {
            result.add(new ColumnStats(
                col.columnName(),
                col.dataType(),
                col.columnComment(),
                col.nullable(),
                col.primaryKey(),
                0, 0, 0, 0,
                List.of(),
                Map.of()
            ));
        }

        return result;
    }

    private String generateSnapshotCode() {
        return "SNAP-" + System.currentTimeMillis();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(FORMATTER);
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return localDateTime.format(FORMATTER);
    }

    private Wrapper<DprofileSnapshot> buildQueryWrapper(DprofileSnapshotVo vo) {
        LambdaQueryWrapper<DprofileSnapshot> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ObjectUtil.isNotNull(vo.getDsId()), DprofileSnapshot::getDsId, vo.getDsId())
            .like(StringUtils.isNotBlank(vo.getSnapshotName()), DprofileSnapshot::getSnapshotName, vo.getSnapshotName())
            .orderByDesc(DprofileSnapshot::getCreateTime);
        return wrapper;
    }
}
