package com.bagdatahouse.governance.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.common.util.DataMaskingEngine;
import com.bagdatahouse.common.util.WaterMarker;
import com.bagdatahouse.core.entity.*;
import com.bagdatahouse.core.mapper.*;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import com.bagdatahouse.datasource.manager.DynamicDataSourceManager;
import com.bagdatahouse.governance.security.dto.*;
import com.bagdatahouse.governance.security.enums.*;
import com.bagdatahouse.governance.security.service.MaskService;
import com.bagdatahouse.governance.security.vo.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据安全 — 脱敏任务/策略/白名单服务实现
 */
@Slf4j
@Service
public class MaskServiceImpl implements MaskService {

    private static final int DEFAULT_BATCH_SIZE = 1000;

    @Autowired
    private SecMaskTaskMapper maskTaskMapper;

    @Autowired
    private SecMaskExecutionLogMapper executionLogMapper;

    @Autowired
    private SecMaskStrategyMapper strategyMapper;

    @Autowired
    private SecMaskWhitelistMapper whitelistMapper;

    @Autowired
    private DqDatasourceMapper datasourceMapper;

    @Autowired(required = false)
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Autowired(required = false)
    private DataSourceAdapterRegistry adapterRegistry;

    @Autowired
    private DataMaskingEngine maskingEngine;

    @Autowired
    private WaterMarker waterMarker;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 脱敏任务管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecMaskTask> saveMaskTask(SecMaskTaskSaveDTO dto) {
        // 校验 taskCode 唯一性
        if (!StringUtils.hasText(dto.getTaskCode())) {
            throw new BusinessException(2001, "任务编码不能为空");
        }
        LambdaQueryWrapper<SecMaskTask> check = new LambdaQueryWrapper<>();
        check.eq(SecMaskTask::getTaskCode, dto.getTaskCode());
        if (maskTaskMapper.selectCount(check) > 0) {
            throw new BusinessException(2001, "任务编码已存在: " + dto.getTaskCode());
        }

        SecMaskTask entity = new SecMaskTask();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(MaskTaskStatusEnum.DRAFT.getCode());
        entity.setTotalRunCount(0);
        if (entity.getBatchSize() == null) {
            entity.setBatchSize(DEFAULT_BATCH_SIZE);
        }
        if (entity.getTriggerType() == null) {
            entity.setTriggerType(MaskTriggerTypeEnum.MANUAL.getCode());
        }
        if (entity.getTargetMode() == null) {
            entity.setTargetMode(TargetModeEnum.APPEND.getCode());
        }
        maskTaskMapper.insert(entity);
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateMaskTask(Long id, SecMaskTaskSaveDTO dto) {
        SecMaskTask existing = maskTaskMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "脱敏任务不存在: id=" + id);
        }
        // taskCode 不能改
        dto.setTaskCode(null);
        BeanUtils.copyProperties(dto, existing);
        maskTaskMapper.updateById(existing);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteMaskTask(Long id) {
        maskTaskMapper.deleteById(id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchDeleteMaskTask(List<Long> ids) {
        maskTaskMapper.deleteBatchIds(ids);
        return Result.success();
    }

    @Override
    public Result<SecMaskTask> getMaskTaskById(Long id) {
        return Result.success(maskTaskMapper.selectById(id));
    }

    @Override
    public Result<Page<SecMaskTaskVO>> pageMaskTask(Integer pageNum, Integer pageSize,
            Long dsId, String status, String taskType) {
        Page<SecMaskTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecMaskTask> wrapper = new LambdaQueryWrapper<>();
        if (dsId != null) {
            wrapper.eq(SecMaskTask::getSourceDsId, dsId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SecMaskTask::getStatus, status);
        }
        if (StringUtils.hasText(taskType)) {
            wrapper.eq(SecMaskTask::getTaskType, taskType);
        }
        wrapper.orderByDesc(SecMaskTask::getCreateTime);
        Page<SecMaskTask> result = maskTaskMapper.selectPage(page, wrapper);

        Page<SecMaskTaskVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SecMaskTaskVO> records = result.getRecords().stream()
                .map(this::convertToTaskVO)
                .collect(Collectors.toList());
        voPage.setRecords(records);
        return Result.success(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enableMaskTask(Long id) {
        SecMaskTask task = maskTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(2001, "脱敏任务不存在");
        }
        task.setStatus(MaskTaskStatusEnum.PUBLISHED.getCode());
        maskTaskMapper.updateById(task);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disableMaskTask(Long id) {
        SecMaskTask task = maskTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(2001, "脱敏任务不存在");
        }
        task.setStatus(MaskTaskStatusEnum.DRAFT.getCode());
        maskTaskMapper.updateById(task);
        return Result.success();
    }

    // ==================== 静态脱敏执行器 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecMaskExecutionLogVO> executeMaskTask(Long id) {
        SecMaskTask task = maskTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(2001, "脱敏任务不存在: id=" + id);
        }
        if (!MaskTaskTypeEnum.STATIC.getCode().equals(task.getTaskType())) {
            throw new BusinessException(2001, "仅支持静态脱敏任务执行");
        }
        if (task.getSourceDsId() == null) {
            throw new BusinessException(2001, "源端数据源未配置");
        }
        if (task.getTargetDsId() == null || !StringUtils.hasText(task.getTargetTable())) {
            throw new BusinessException(2001, "目标端数据源和目标表未配置");
        }

        // 更新任务状态为 RUNNING
        task.setStatus(MaskTaskStatusEnum.RUNNING.getCode());
        maskTaskMapper.updateById(task);

        // 创建执行日志
        SecMaskExecutionLog execLog = SecMaskExecutionLog.builder()
                .taskId(task.getId())
                .taskCode(task.getTaskCode())
                .runTime(LocalDateTime.now())
                .status(MaskTaskStatusEnum.RUNNING.getCode())
                .build();
        executionLogMapper.insert(execLog);

        long startMs = System.currentTimeMillis();
        try {
            // 获取源端和目标端适配器
            DataSourceAdapter sourceAdapter = getAdapter(task.getSourceDsId());
            DataSourceAdapter targetAdapter = getAdapter(task.getTargetDsId());

            if (sourceAdapter == null || targetAdapter == null) {
                throw new BusinessException(2001, "数据源连接失败");
            }

            // 解析脱敏规则
            List<Map<String, String>> maskRules = parseMaskRules(task.getMaskRules());

            // 解析 SQL（支持 ${table} 占位符）
            String sql = resolveSourceSql(task.getSourceSql(), task.getSourceSchema(), task.getSourceDsId());

            // 获取源端数据源配置（用于获取表名）
            DqDatasource sourceDs = datasourceMapper.selectById(task.getSourceDsId());

            // 分批查询并脱敏写入
            int batchSize = task.getBatchSize() != null ? task.getBatchSize() : DEFAULT_BATCH_SIZE;
            int totalRows = 0;
            int maskedRows = 0;
            int errorRows = 0;

            long offset = 0;
            boolean hasMore = true;
            StringBuilder sqlLog = new StringBuilder();

            while (hasMore) {
                String pagedSql = sourceAdapter.buildPaginationSql(sql, offset, batchSize);
                List<Map<String, Object>> rows = sourceAdapter.executeQuery(pagedSql);

                if (rows == null || rows.isEmpty()) {
                    hasMore = false;
                    break;
                }

                for (Map<String, Object> row : rows) {
                    totalRows++;
                    try {
                        Map<String, Object> maskedRow = applyMaskToRow(row, maskRules);
                        String insertSql = buildInsertSql(targetAdapter, task, maskedRow, row.keySet());
                        targetAdapter.executeUpdate(insertSql);
                        maskedRows++;
                        if (sqlLog.length() < 2000) {
                            sqlLog.append(insertSql, 0, Math.min(200, insertSql.length())).append("\n");
                        }
                    } catch (Exception e) {
                        errorRows++;
                        log.warn("脱敏行处理失败: {}", e.getMessage());
                    }
                }

                offset += batchSize;
                if (rows.size() < batchSize) {
                    hasMore = false;
                }
            }

            // 处理目标表写入模式
            handleTargetMode(targetAdapter, task);

            long durationMs = System.currentTimeMillis() - startMs;

            // 更新执行日志
            execLog.setStatus(MaskTaskStatusEnum.SUCCESS.getCode());
            execLog.setTotalRows(totalRows);
            execLog.setMaskedRows(maskedRows);
            execLog.setErrorRows(errorRows);
            execLog.setDurationMs(durationMs);
            execLog.setExecuteSqlLog(sqlLog.toString());
            executionLogMapper.updateById(execLog);

            // 更新任务状态
            task.setStatus(MaskTaskStatusEnum.SUCCESS.getCode());
            task.setLastRunTime(LocalDateTime.now());
            task.setLastRunStatus(MaskTaskStatusEnum.SUCCESS.getCode());
            task.setLastRunRows(totalRows);
            task.setLastRunError(null);
            task.setTotalRunCount(task.getTotalRunCount() != null ? task.getTotalRunCount() + 1 : 1);
            maskTaskMapper.updateById(task);

            return Result.success(convertToLogVO(execLog));

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            log.error("脱敏任务执行失败: id={}", id, e);

            execLog.setStatus(MaskTaskStatusEnum.FAILED.getCode());
            execLog.setErrorMessage(e.getMessage());
            execLog.setDurationMs(durationMs);
            executionLogMapper.updateById(execLog);

            task.setStatus(MaskTaskStatusEnum.FAILED.getCode());
            task.setLastRunTime(LocalDateTime.now());
            task.setLastRunStatus(MaskTaskStatusEnum.FAILED.getCode());
            task.setLastRunError(e.getMessage());
            task.setTotalRunCount(task.getTotalRunCount() != null ? task.getTotalRunCount() + 1 : 1);
            maskTaskMapper.updateById(task);

            throw new BusinessException(2001, "脱敏任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定数据源的适配器
     */
    private DataSourceAdapter getAdapter(Long dsId) {
        if (adapterRegistry == null) {
            return null;
        }
        return adapterRegistry.getAdapterById(dsId);
    }

    /**
     * 解析脱敏规则 JSON
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseMaskRules(String maskRulesJson) {
        if (!StringUtils.hasText(maskRulesJson)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(maskRulesJson, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("脱敏规则解析失败: {}", maskRulesJson);
            return new ArrayList<>();
        }
    }

    /**
     * 解析源端 SQL，支持 ${table} 占位符
     */
    private String resolveSourceSql(String sourceSql, String sourceSchema, Long dsId) {
        if (!StringUtils.hasText(sourceSql)) {
            return null;
        }
        String sql = sourceSql;
        if (sql.contains("${table}")) {
            // 从元数据或直接查询获取表名，这里直接返回原 SQL（用户应提供完整 SQL）
            sql = sql.replace("${table}", "");
        }
        return sql.trim();
    }

    /**
     * 对单行数据应用脱敏规则
     */
    private Map<String, Object> applyMaskToRow(Map<String, Object> row, List<Map<String, String>> maskRules) {
        Map<String, Object> masked = new LinkedHashMap<>(row);
        for (Map<String, String> rule : maskRules) {
            String columnName = rule.get("columnName");
            String maskType = rule.get("maskType");
            String maskPattern = rule.get("maskPattern");

            if (columnName != null && masked.containsKey(columnName)) {
                Object raw = masked.get(columnName);
                String rawStr = raw != null ? raw.toString() : null;
                String maskedStr = maskingEngine.applyMask(rawStr, maskType, maskPattern);
                masked.put(columnName, maskedStr);
            }
        }
        return masked;
    }

    /**
     * 构建目标端 INSERT SQL（使用目标端适配器的标识符引号）
     */
    private String buildInsertSql(DataSourceAdapter targetAdapter, SecMaskTask task,
            Map<String, Object> maskedRow, Set<String> columns) {
        List<String> colNames = columns.stream()
                .map(c -> targetAdapter.quoteIdentifier(c))
                .collect(Collectors.toList());

        List<String> placeholders = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (String col : columns) {
            placeholders.add("?");
            Object val = maskedRow.get(col);
            values.add(val != null ? val.toString() : null);
        }

        String targetTable = targetAdapter.quoteIdentifier(task.getTargetTable());
        String cols = String.join(", ", colNames);
        String vals = String.join(", ", placeholders);

        return "INSERT INTO " + targetTable + " (" + cols + ") VALUES (" + vals + ")";
    }

    /**
     * 处理目标表写入模式
     */
    private void handleTargetMode(DataSourceAdapter targetAdapter, SecMaskTask task) {
        if (!StringUtils.hasText(task.getTargetMode())) {
            return;
        }
        String targetTable = targetAdapter.quoteIdentifier(task.getTargetTable());
        switch (task.getTargetMode()) {
            case "TRUNCATE":
                targetAdapter.executeUpdate("DELETE FROM " + targetTable);
                break;
            case "UPSERT":
                // UPSERT 由 INSERT 触发，依赖目标端数据库支持（如 MySQL ON DUPLICATE KEY UPDATE）
                break;
            case "APPEND":
            default:
                // 追加模式，无需额外处理
                break;
        }
    }

    @Override
    public Result<Page<SecMaskExecutionLogVO>> pageMaskExecutionLog(Integer pageNum, Integer pageSize, Long taskId) {
        Page<SecMaskExecutionLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecMaskExecutionLog> wrapper = new LambdaQueryWrapper<>();
        if (taskId != null) {
            wrapper.eq(SecMaskExecutionLog::getTaskId, taskId);
        }
        wrapper.orderByDesc(SecMaskExecutionLog::getRunTime);
        Page<SecMaskExecutionLog> result = executionLogMapper.selectPage(page, wrapper);

        Page<SecMaskExecutionLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SecMaskExecutionLogVO> records = result.getRecords().stream()
                .map(this::convertToLogVO)
                .collect(Collectors.toList());
        voPage.setRecords(records);
        return Result.success(voPage);
    }

    // ==================== 脱敏策略管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecMaskStrategy> saveMaskStrategy(SecMaskStrategySaveDTO dto) {
        if (!StringUtils.hasText(dto.getStrategyCode())) {
            throw new BusinessException(2001, "策略编码不能为空");
        }
        LambdaQueryWrapper<SecMaskStrategy> check = new LambdaQueryWrapper<>();
        check.eq(SecMaskStrategy::getStrategyCode, dto.getStrategyCode());
        if (strategyMapper.selectCount(check) > 0) {
            throw new BusinessException(2001, "策略编码已存在: " + dto.getStrategyCode());
        }

        SecMaskStrategy entity = new SecMaskStrategy();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus("ENABLED");
        if (entity.getPriority() == null) {
            entity.setPriority(100);
        }
        if (entity.getConflictCheck() == null) {
            entity.setConflictCheck(1);
        }
        strategyMapper.insert(entity);
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateMaskStrategy(Long id, SecMaskStrategySaveDTO dto) {
        SecMaskStrategy existing = strategyMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "脱敏策略不存在");
        }
        dto.setStrategyCode(null);
        BeanUtils.copyProperties(dto, existing);
        strategyMapper.updateById(existing);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteMaskStrategy(Long id) {
        strategyMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<SecMaskStrategy> getMaskStrategyById(Long id) {
        return Result.success(strategyMapper.selectById(id));
    }

    @Override
    public Result<Page<SecMaskStrategyVO>> pageMaskStrategy(Integer pageNum, Integer pageSize,
            String sceneType, String status) {
        Page<SecMaskStrategy> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecMaskStrategy> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sceneType)) {
            wrapper.eq(SecMaskStrategy::getSceneType, sceneType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SecMaskStrategy::getStatus, status);
        }
        wrapper.orderByAsc(SecMaskStrategy::getPriority);
        Page<SecMaskStrategy> result = strategyMapper.selectPage(page, wrapper);

        Page<SecMaskStrategyVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SecMaskStrategyVO> records = result.getRecords().stream()
                .map(this::convertToStrategyVO)
                .collect(Collectors.toList());
        voPage.setRecords(records);
        return Result.success(voPage);
    }

    @Override
    public Result<List<SecMaskStrategyVO>> listMaskStrategyByScene(String sceneType) {
        LambdaQueryWrapper<SecMaskStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecMaskStrategy::getStatus, "ENABLED");
        if (StringUtils.hasText(sceneType)) {
            wrapper.eq(SecMaskStrategy::getSceneType, sceneType);
        }
        wrapper.orderByAsc(SecMaskStrategy::getPriority);
        List<SecMaskStrategy> list = strategyMapper.selectList(wrapper);
        List<SecMaskStrategyVO> vos = list.stream()
                .map(this::convertToStrategyVO)
                .collect(Collectors.toList());
        return Result.success(vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> enableMaskStrategy(Long id) {
        SecMaskStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new BusinessException(2001, "脱敏策略不存在");
        }
        strategy.setStatus("ENABLED");
        strategyMapper.updateById(strategy);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> disableMaskStrategy(Long id) {
        SecMaskStrategy strategy = strategyMapper.selectById(id);
        if (strategy == null) {
            throw new BusinessException(2001, "脱敏策略不存在");
        }
        strategy.setStatus("DISABLED");
        strategyMapper.updateById(strategy);
        return Result.success();
    }

    /**
     * 检测脱敏策略冲突：同一场景下同一敏感等级有多个 maskType
     */
    @Override
    public Result<List<String>> detectStrategyConflicts() {
        List<String> conflicts = new ArrayList<>();

        // 按场景分组查询所有启用的策略
        LambdaQueryWrapper<SecMaskStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecMaskStrategy::getStatus, "ENABLED");
        List<SecMaskStrategy> strategies = strategyMapper.selectList(wrapper);

        // 按 sceneType 分组
        Map<String, List<SecMaskStrategy>> byScene = strategies.stream()
                .collect(Collectors.groupingBy(SecMaskStrategy::getSceneType));

        for (Map.Entry<String, List<SecMaskStrategy>> entry : byScene.entrySet()) {
            String sceneType = entry.getKey();
            List<SecMaskStrategy> sceneStrategies = entry.getValue();

            // 解析每个策略的等级→maskType 映射，检查同一等级是否有不同 maskType
            Map<String, Set<String>> levelToMaskTypes = new HashMap<>();

            for (SecMaskStrategy strategy : sceneStrategies) {
                String mappingJson = strategy.getLevelMaskMapping();
                if (!StringUtils.hasText(mappingJson)) continue;

                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> mappings = objectMapper.readValue(mappingJson,
                            new TypeReference<List<Map<String, String>>>() {});
                    for (Map<String, String> mapping : mappings) {
                        String level = mapping.get("sensitivityLevel");
                        String maskType = mapping.get("maskType");
                        if (level != null && maskType != null) {
                            levelToMaskTypes.computeIfAbsent(level, k -> new HashSet<>()).add(maskType);
                        }
                    }
                } catch (Exception e) {
                    log.warn("策略映射解析失败: strategyId={}", strategy.getId(), e);
                }
            }

            // 检查冲突
            for (Map.Entry<String, Set<String>> levelEntry : levelToMaskTypes.entrySet()) {
                if (levelEntry.getValue().size() > 1) {
                    conflicts.add(String.format("场景[%s] 等级[%s] 存在多个脱敏规则: %s",
                            MaskSceneTypeEnum.getLabel(sceneType),
                            levelEntry.getKey(),
                            levelEntry.getValue()));
                }
            }
        }

        return Result.success(conflicts);
    }

    // ==================== 脱敏白名单管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<SecMaskWhitelist> saveMaskWhitelist(SecMaskWhitelistSaveDTO dto) {
        if (dto.getStrategyId() == null) {
            throw new BusinessException(2001, "关联策略ID不能为空");
        }
        if (!StringUtils.hasText(dto.getEntityType())) {
            throw new BusinessException(2001, "白名单类型不能为空");
        }
        if (dto.getEntityId() == null) {
            throw new BusinessException(2001, "用户ID/角色ID不能为空");
        }

        SecMaskWhitelist entity = new SecMaskWhitelist();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(WhitelistStatusEnum.ACTIVE.getCode());
        if (entity.getWhitelistType() == null) {
            entity.setWhitelistType(WhitelistTypeEnum.FULL_EXEMPT.getCode());
        }
        if (StringUtils.hasText(dto.getStartTime())) {
            entity.setStartTime(LocalDateTime.parse(dto.getStartTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(dto.getEndTime())) {
            entity.setEndTime(LocalDateTime.parse(dto.getEndTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        whitelistMapper.insert(entity);
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateMaskWhitelist(Long id, SecMaskWhitelistSaveDTO dto) {
        SecMaskWhitelist existing = whitelistMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(2001, "白名单记录不存在");
        }
        dto.setStrategyId(null);
        dto.setEntityType(null);
        dto.setEntityId(null);
        BeanUtils.copyProperties(dto, existing);
        if (StringUtils.hasText(dto.getStartTime())) {
            existing.setStartTime(LocalDateTime.parse(dto.getStartTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (StringUtils.hasText(dto.getEndTime())) {
            existing.setEndTime(LocalDateTime.parse(dto.getEndTime(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        whitelistMapper.updateById(existing);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteMaskWhitelist(Long id) {
        whitelistMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result<SecMaskWhitelist> getMaskWhitelistById(Long id) {
        return Result.success(whitelistMapper.selectById(id));
    }

    @Override
    public Result<Page<SecMaskWhitelistVO>> pageMaskWhitelist(Integer pageNum, Integer pageSize,
            Long strategyId, String entityType, String status) {
        Page<SecMaskWhitelist> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SecMaskWhitelist> wrapper = new LambdaQueryWrapper<>();
        if (strategyId != null) {
            wrapper.eq(SecMaskWhitelist::getStrategyId, strategyId);
        }
        if (StringUtils.hasText(entityType)) {
            wrapper.eq(SecMaskWhitelist::getEntityType, entityType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SecMaskWhitelist::getStatus, status);
        }
        wrapper.orderByDesc(SecMaskWhitelist::getCreateTime);
        Page<SecMaskWhitelist> result = whitelistMapper.selectPage(page, wrapper);

        Page<SecMaskWhitelistVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<SecMaskWhitelistVO> records = result.getRecords().stream()
                .map(this::convertToWhitelistVO)
                .collect(Collectors.toList());
        voPage.setRecords(records);
        return Result.success(voPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> approveMaskWhitelist(SecMaskWhitelistApproveDTO dto) {
        SecMaskWhitelist whitelist = whitelistMapper.selectById(dto.getWhitelistId());
        if (whitelist == null) {
            throw new BusinessException(2001, "白名单记录不存在");
        }

        if ("APPROVE".equals(dto.getAction())) {
            whitelist.setStatus(WhitelistStatusEnum.ACTIVE.getCode());
        } else if ("REJECT".equals(dto.getAction())) {
            whitelist.setStatus(WhitelistStatusEnum.REVOKED.getCode());
        } else {
            throw new BusinessException(2001, "无效的审批动作: " + dto.getAction());
        }
        whitelist.setApproverId(dto.getApproverId());
        whitelist.setApproverName(dto.getApproverName());
        whitelist.setApproveTime(LocalDateTime.now());
        whitelist.setApproveComment(dto.getComment());
        whitelistMapper.updateById(whitelist);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> revokeMaskWhitelist(Long id) {
        SecMaskWhitelist whitelist = whitelistMapper.selectById(id);
        if (whitelist == null) {
            throw new BusinessException(2001, "白名单记录不存在");
        }
        whitelist.setStatus(WhitelistStatusEnum.REVOKED.getCode());
        whitelistMapper.updateById(whitelist);
        return Result.success();
    }

    /**
     * 检查用户/角色是否在白名单中（在有效期内）
     */
    @Override
    public Result<Boolean> isInWhitelist(Long strategyId, String entityType, Long entityId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<SecMaskWhitelist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecMaskWhitelist::getStrategyId, strategyId);
        wrapper.eq(SecMaskWhitelist::getEntityType, entityType);
        wrapper.eq(SecMaskWhitelist::getEntityId, entityId);
        wrapper.eq(SecMaskWhitelist::getStatus, WhitelistStatusEnum.ACTIVE.getCode());
        // 有效期检查
        wrapper.and(w -> w
                .isNull(SecMaskWhitelist::getEndTime)
                .or()
                .ge(SecMaskWhitelist::getEndTime, now)
        );
        wrapper.and(w -> w
                .isNull(SecMaskWhitelist::getStartTime)
                .or()
                .le(SecMaskWhitelist::getStartTime, now)
        );

        long count = whitelistMapper.selectCount(wrapper);
        return Result.success(count > 0);
    }

    // ==================== 动态脱敏 ====================

    /**
     * 动态脱敏：根据策略对单条数据进行脱敏
     */
    @Override
    public Result<String> dynamicMask(String value, String sceneType, String level,
            String entityType, Long entityId) {
        if (value == null) {
            return Result.success(null);
        }
        if (!StringUtils.hasText(sceneType)) {
            return Result.success(value);
        }

        // 查询对应场景的策略
        LambdaQueryWrapper<SecMaskStrategy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecMaskStrategy::getSceneType, sceneType);
        wrapper.eq(SecMaskStrategy::getStatus, "ENABLED");
        wrapper.orderByAsc(SecMaskStrategy::getPriority);
        List<SecMaskStrategy> strategies = strategyMapper.selectList(wrapper);

        if (strategies.isEmpty()) {
            return Result.success(value);
        }

        // 取优先级最高的策略
        SecMaskStrategy strategy = strategies.get(0);

        // 检查白名单
        Boolean inWhitelist = isInWhitelist(strategy.getId(), entityType, entityId).getData();
        if (Boolean.TRUE.equals(inWhitelist)) {
            return Result.success(value);
        }

        // 解析等级→脱敏类型映射
        String mappingJson = strategy.getLevelMaskMapping();
        if (!StringUtils.hasText(mappingJson)) {
            return Result.success(value);
        }

        String maskType = resolveMaskTypeFromMapping(mappingJson, level);
        if (maskType == null || "NONE".equalsIgnoreCase(maskType)) {
            return Result.success(value);
        }

        String masked = maskingEngine.applyMask(value, maskType, null);
        return Result.success(masked);
    }

    /**
     * 从等级→脱敏类型映射 JSON 中获取指定等级的 maskType
     */
    private String resolveMaskTypeFromMapping(String mappingJson, String level) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> mappings = objectMapper.readValue(mappingJson,
                    new TypeReference<List<Map<String, String>>>() {});
            for (Map<String, String> mapping : mappings) {
                String mappedLevel = mapping.get("sensitivityLevel");
                if (level != null && level.equalsIgnoreCase(mappedLevel)) {
                    return mapping.get("maskType");
                }
            }
        } catch (Exception e) {
            log.warn("策略映射解析失败: {}", mappingJson, e);
        }
        return null;
    }

    // ==================== VO 转换工具 ====================

    private SecMaskTaskVO convertToTaskVO(SecMaskTask task) {
        SecMaskTaskVO vo = SecMaskTaskVO.builder()
                .id(task.getId())
                .taskName(task.getTaskName())
                .taskCode(task.getTaskCode())
                .taskType(task.getTaskType())
                .taskTypeLabel(MaskTaskTypeEnum.getLabel(task.getTaskType()))
                .sourceDsId(task.getSourceDsId())
                .sourceSchema(task.getSourceSchema())
                .targetDsId(task.getTargetDsId())
                .targetSchema(task.getTargetSchema())
                .targetTable(task.getTargetTable())
                .targetMode(task.getTargetMode())
                .triggerType(task.getTriggerType())
                .triggerTypeLabel(MaskTriggerTypeEnum.getLabel(task.getTriggerType()))
                .triggerCron(task.getTriggerCron())
                .status(task.getStatus())
                .statusLabel(MaskTaskStatusEnum.getLabel(task.getStatus()))
                .lastRunTime(task.getLastRunTime())
                .lastRunStatus(task.getLastRunStatus())
                .lastRunRows(task.getLastRunRows())
                .totalRunCount(task.getTotalRunCount())
                .remark(task.getRemark())
                .createUser(task.getCreateUser())
                .createTime(task.getCreateTime())
                .updateUser(task.getUpdateUser())
                .updateTime(task.getUpdateTime())
                .build();

        // 填充数据源名称
        if (task.getSourceDsId() != null) {
            DqDatasource ds = datasourceMapper.selectById(task.getSourceDsId());
            if (ds != null) {
                vo.setSourceDsName(ds.getDsName());
            }
        }
        if (task.getTargetDsId() != null) {
            DqDatasource ds = datasourceMapper.selectById(task.getTargetDsId());
            if (ds != null) {
                vo.setTargetDsName(ds.getDsName());
            }
        }
        return vo;
    }

    private SecMaskExecutionLogVO convertToLogVO(SecMaskExecutionLog log) {
        return SecMaskExecutionLogVO.builder()
                .id(log.getId())
                .taskId(log.getTaskId())
                .taskCode(log.getTaskCode())
                .runTime(log.getRunTime())
                .status(log.getStatus())
                .statusLabel(MaskTaskStatusEnum.getLabel(log.getStatus()))
                .totalRows(log.getTotalRows())
                .maskedRows(log.getMaskedRows())
                .errorRows(log.getErrorRows())
                .durationMs(log.getDurationMs())
                .errorMessage(log.getErrorMessage())
                .sourceRowCount(log.getSourceRowCount())
                .createTime(log.getCreateTime())
                .build();
    }

    private SecMaskStrategyVO convertToStrategyVO(SecMaskStrategy strategy) {
        // 统计白名单数量
        LambdaQueryWrapper<SecMaskWhitelist> wlWrapper = new LambdaQueryWrapper<>();
        wlWrapper.eq(SecMaskWhitelist::getStrategyId, strategy.getId());
        wlWrapper.eq(SecMaskWhitelist::getStatus, WhitelistStatusEnum.ACTIVE.getCode());
        long whitelistCount = whitelistMapper.selectCount(wlWrapper);

        return SecMaskStrategyVO.builder()
                .id(strategy.getId())
                .strategyName(strategy.getStrategyName())
                .strategyCode(strategy.getStrategyCode())
                .sceneType(strategy.getSceneType())
                .sceneTypeLabel(MaskSceneTypeEnum.getLabel(strategy.getSceneType()))
                .strategyDesc(strategy.getStrategyDesc())
                .levelMaskMapping(strategy.getLevelMaskMapping())
                .whitelistCount((int) whitelistCount)
                .whitelistExpiry(strategy.getWhitelistExpiry())
                .priority(strategy.getPriority())
                .conflictCheck(strategy.getConflictCheck())
                .status(strategy.getStatus())
                .statusLabel("ENABLED".equals(strategy.getStatus()) ? "启用" : "禁用")
                .createUser(strategy.getCreateUser())
                .createTime(strategy.getCreateTime())
                .updateUser(strategy.getUpdateUser())
                .updateTime(strategy.getUpdateTime())
                .build();
    }

    private SecMaskWhitelistVO convertToWhitelistVO(SecMaskWhitelist wl) {
        LocalDateTime now = LocalDateTime.now();
        boolean inEffect = (wl.getStartTime() == null || !wl.getStartTime().isAfter(now))
                && (wl.getEndTime() == null || !wl.getEndTime().isBefore(now));

        SecMaskWhitelistVO vo = SecMaskWhitelistVO.builder()
                .id(wl.getId())
                .strategyId(wl.getStrategyId())
                .entityType(wl.getEntityType())
                .entityTypeLabel(WhitelistEntityTypeEnum.getLabel(wl.getEntityType()))
                .entityId(wl.getEntityId())
                .entityName(wl.getEntityName())
                .whitelistType(wl.getWhitelistType())
                .whitelistTypeLabel(WhitelistTypeEnum.getLabel(wl.getWhitelistType()))
                .startTime(wl.getStartTime())
                .endTime(wl.getEndTime())
                .inEffect(inEffect)
                .reason(wl.getReason())
                .approverId(wl.getApproverId())
                .approverName(wl.getApproverName())
                .approveTime(wl.getApproveTime())
                .approveComment(wl.getApproveComment())
                .status(wl.getStatus())
                .statusLabel(WhitelistStatusEnum.getLabel(wl.getStatus()))
                .createUser(wl.getCreateUser())
                .createTime(wl.getCreateTime())
                .updateUser(wl.getUpdateUser())
                .updateTime(wl.getUpdateTime())
                .build();

        // 填充策略信息
        if (wl.getStrategyId() != null) {
            SecMaskStrategy strategy = strategyMapper.selectById(wl.getStrategyId());
            if (strategy != null) {
                vo.setStrategyName(strategy.getStrategyName());
                vo.setStrategyCode(strategy.getStrategyCode());
            }
        }
        return vo;
    }
}
