package org.dromara.metadata.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.datasource.adapter.DataSourceAdapter.ColumnInfo;
import org.dromara.metadata.domain.SecColumnSensitivity;
import org.dromara.metadata.domain.SecLevel;
import org.dromara.metadata.domain.SecSensitivityRule;
import org.dromara.metadata.domain.dto.SecSensitivityScanDTO;
import org.dromara.metadata.domain.vo.SecColumnSensitivityVo;
import org.dromara.metadata.domain.vo.SecScanResultVO;
import org.dromara.metadata.mapper.SecColumnSensitivityMapper;
import org.dromara.metadata.mapper.SecLevelMapper;
import org.dromara.metadata.mapper.SecSensitivityRuleMapper;
import org.dromara.metadata.service.ISecColumnSensitivityService;
import org.dromara.metadata.service.ISecSensitivityRuleService;
import org.dromara.metadata.support.DatasourceHelper;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 字段敏感记录服务实现
 * 对标 governance/SecurityServiceImpl 的完整扫描能力
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SecColumnSensitivityServiceImpl implements ISecColumnSensitivityService {

    private final SecColumnSensitivityMapper baseMapper;
    private final SecLevelMapper levelMapper;
    private final SecSensitivityRuleMapper ruleMapper;
    private final DatasourceHelper datasourceHelper;

    /** Redis 分布式锁前缀 */
    private static final String LOCK_KEY_PREFIX = "secscan:lock:";

    @Override
    public TableDataInfo<SecColumnSensitivityVo> queryPageList(SecColumnSensitivityVo vo, PageQuery pageQuery) {
        List<Long> accessibleDsIds = datasourceHelper.resolveAccessibleDatasourceIds(vo.getDsId());
        var wrapper = Wrappers.<SecColumnSensitivity>lambdaQuery()
            .in(!accessibleDsIds.isEmpty(), SecColumnSensitivity::getDsId, accessibleDsIds)
            .eq(accessibleDsIds.isEmpty(), SecColumnSensitivity::getId, -1L)
            .like(StringUtils.isNotBlank(vo.getTableName()), SecColumnSensitivity::getTableName, vo.getTableName())
            .like(StringUtils.isNotBlank(vo.getColumnName()), SecColumnSensitivity::getColumnName, vo.getColumnName())
            .eq(StringUtils.isNotBlank(vo.getLevelCode()), SecColumnSensitivity::getLevelCode, vo.getLevelCode())
            .eq(StringUtils.isNotBlank(vo.getClassCode()), SecColumnSensitivity::getClassCode, vo.getClassCode())
            .eq(StringUtils.isNotBlank(vo.getConfirmed()), SecColumnSensitivity::getConfirmed, vo.getConfirmed())
            .orderByDesc(SecColumnSensitivity::getCreateTime);
        var page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SecColumnSensitivityVo queryById(Long id) {
        return MapstructUtils.convert(requireAccessibleRecord(id), SecColumnSensitivityVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SecScanResultVO scanSensitiveFields(SecSensitivityScanDTO dto) {
        if (dto.getDsId() == null) {
            throw new ServiceException("数据源ID不能为空");
        }

        // 验证数据源存在
        datasourceHelper.getSysDatasource(dto.getDsId());

        RLock lock = RedisUtils.getClient().getLock(LOCK_KEY_PREFIX + dto.getDsId());
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 3600, TimeUnit.SECONDS);
            if (!locked) {
                throw new ServiceException("该数据源的敏感字段扫描正在进行中，请稍后重试");
            }
            return executeScan(dto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("扫描被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行敏感字段扫描核心逻辑
     */
    private SecScanResultVO executeScan(SecSensitivityScanDTO dto) {
        long startTime = System.currentTimeMillis();
        String batchNo = "SCAN-" + System.currentTimeMillis();

        DataSourceAdapter adapter = datasourceHelper.getAdapter(dto.getDsId());
        String dsName = datasourceHelper.getSysDatasource(dto.getDsId()).getDsName();

        // 查询启用的规则并按优先级排序
        List<SecSensitivityRule> enabledRules = ruleMapper.selectList(
            Wrappers.<SecSensitivityRule>lambdaQuery()
                .eq(SecSensitivityRule::getEnabled, "1")
        );
        // 按优先级从高到低排序
        enabledRules.sort((a, b) -> {
            int pA = a.getPriority() != null ? a.getPriority() : 0;
            int pB = b.getPriority() != null ? b.getPriority() : 0;
            return Integer.compare(pB, pA);
        });

        if (CollUtil.isEmpty(enabledRules)) {
            log.warn("没有启用的敏感识别规则");
            return buildEmptyResult(dto, batchNo, dsName, startTime);
        }

        // 预加载敏感等级映射（code → entity）
        Map<String, SecLevel> levelMapByCode = levelMapper.selectList(null).stream()
            .collect(Collectors.toMap(SecLevel::getLevelCode, r -> r, (a, b) -> a));

        // 获取待扫描表列表
        List<String> tables = adapter.getTables();
        Set<String> scannedTables = new HashSet<>();
        List<SecColumnSensitivityVo> foundFields = new ArrayList<>();
        int totalColumnCount = 0;
        int newCount = 0;
        int updateCount = 0;

        for (String tableName : tables) {
            // 排除系统表
            if (Boolean.TRUE.equals(dto.getExcludeSystemTables())
                    && (tableName.startsWith("sys_") || tableName.startsWith("mysql_")
                    || tableName.startsWith("performance_") || tableName.startsWith("information_"))) {
                continue;
            }

            try {
                scannedTables.add(tableName);
                List<ColumnInfo> columns = adapter.getColumns(tableName);

                for (ColumnInfo col : columns) {
                    totalColumnCount++;
                    SecSensitivityRule bestMatch = matchBestRule(col, enabledRules, dto);

                    if (bestMatch != null) {
                        // ruoyi-metadata 使用 targetLevelCode / targetClassCode（String 编码）
                        SecLevel level = levelMapByCode.get(bestMatch.getTargetLevelCode());

                        // 构造保存 DTO
                        SecColumnSensitivity entity = new SecColumnSensitivity();
                        entity.setDsId(dto.getDsId());
                        entity.setTableName(tableName);
                        entity.setColumnName(col.columnName());
                        entity.setDataType(col.dataType());
                        entity.setLevelCode(level != null ? level.getLevelCode() : bestMatch.getTargetLevelCode());
                        entity.setClassCode(bestMatch.getTargetClassCode());
                        entity.setIdentifiedBy("AUTO");
                        entity.setConfirmed("0");
                        entity.setScanBatchNo(batchNo);

                        // 幂等：已存在的记录更新，新记录插入
                        SecColumnSensitivity existing = baseMapper.selectOne(Wrappers.<SecColumnSensitivity>lambdaQuery()
                            .eq(SecColumnSensitivity::getDsId, dto.getDsId())
                            .eq(SecColumnSensitivity::getTableName, tableName)
                            .eq(SecColumnSensitivity::getColumnName, col.columnName()));

                        if (existing != null) {
                            entity.setId(existing.getId());
                            entity.setUpdateTime(LocalDateTime.now());
                            baseMapper.updateById(entity);
                            updateCount++;
                        } else {
                            entity.setScanTime(LocalDateTime.now());
                            baseMapper.insert(entity);
                            newCount++;
                        }

                        // 构建结果 VO
                        SecColumnSensitivityVo vo = new SecColumnSensitivityVo();
                        vo.setId(entity.getId());
                        vo.setDsId(dto.getDsId());
                        vo.setDsName(dsName);
                        vo.setTableName(tableName);
                        vo.setColumnName(col.columnName());
                        vo.setDataType(col.dataType());
                        vo.setColumnComment(col.columnComment());
                        vo.setLevelCode(level != null ? level.getLevelCode() : bestMatch.getTargetLevelCode());
                        vo.setLevelName(level != null ? level.getLevelName() : null);
                        vo.setLevelColor(level != null ? level.getColor() : null);
                        vo.setMatchRuleId(bestMatch.getId());
                        vo.setMatchRuleName(bestMatch.getRuleName());
                        vo.setIdentifiedBy("AUTO");
                        vo.setConfirmed("0");
                        vo.setConfidence(BigDecimal.valueOf(85));
                        vo.setConfidenceLabel(getConfidenceLabel(BigDecimal.valueOf(85)));
                        vo.setScanBatchNo(batchNo);
                        vo.setScanTime(LocalDateTime.now());
                        foundFields.add(vo);
                    }
                }
            } catch (Exception e) {
                log.error("扫描表 {} 列信息失败: {}", tableName, e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        log.info("敏感字段扫描完成，批次号={}，共扫描 {} 张表 {} 个字段，发现 {} 个敏感字段（新增 {}，更新 {}）",
                batchNo, scannedTables.size(), totalColumnCount, foundFields.size(), newCount, updateCount);

        SecScanResultVO result = new SecScanResultVO();
        result.setBatchNo(batchNo);
        result.setDsId(dto.getDsId());
        result.setDsName(dsName);
        result.setStartTime(startTime);
        result.setEndTime(endTime);
        result.setCostMs(endTime - startTime);
        result.setTotalTableCount(scannedTables.size());
        result.setTotalColumnCount(totalColumnCount);
        result.setScannedTableCount(scannedTables.size());
        result.setScannedColumnCount(totalColumnCount);
        result.setFoundSensitiveCount(foundFields.size());
        result.setNewSensitiveCount(newCount);
        result.setUpdatedSensitiveCount(updateCount);
        result.setStatus(foundFields.isEmpty() ? "PARTIAL" : "SUCCESS");
        result.setSensitiveFields(foundFields);

        return result;
    }

    /**
     * 为最佳匹配的规则（按优先级第一个命中的）
     */
    private SecSensitivityRule matchBestRule(ColumnInfo col, List<SecSensitivityRule> rules, SecSensitivityScanDTO dto) {
        for (SecSensitivityRule rule : rules) {
            if (matchColumn(rule, col, dto)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * 规则匹配核心逻辑，对标 governance/matchColumn
     */
    private boolean matchColumn(SecSensitivityRule rule, ColumnInfo col, SecSensitivityScanDTO dto) {
        if (rule == null || col == null) {
            return false;
        }

        // 规则实体中的字段映射到 gov 风格
        // ruoyi: ruleType(COLUMN_NAME/REGEX/DATA_TYPE), ruleExpr(JSON)
        // gov:   matchType, matchExpr, matchExprType
        String matchType = rule.getRuleType();
        String matchExpr = rule.getRuleExpr();
        String matchExprType = getExprTypeFromRuleExpr(rule.getRuleExpr()); // 从 JSON 中提取

        if (!StringUtils.isNotBlank(matchExpr)) {
            return false;
        }

        // 扫描维度开关
        boolean scanColumnName = dto.getScanColumnName() == null || dto.getScanColumnName();
        boolean scanColumnComment = dto.getScanColumnComment() == null || dto.getScanColumnComment();
        boolean scanDataType = dto.getScanDataType() != null && dto.getScanDataType();
        boolean useRegex = dto.getUseRegex() == null || dto.getUseRegex();

        if ("COLUMN_NAME".equals(matchType)) {
            if (!scanColumnName) return false;
            String columnName = col.columnName() != null ? col.columnName().toLowerCase() : "";
            return matchByExpr(columnName, matchExpr, matchExprType, useRegex);

        } else if ("COLUMN_COMMENT".equals(matchType)) {
            if (!scanColumnComment) return false;
            String comment = col.columnComment() != null ? col.columnComment().toLowerCase() : "";
            return matchByExpr(comment, matchExpr, matchExprType, useRegex);

        } else if ("REGEX".equals(matchType)) {
            if (!useRegex) return false;
            String columnName = col.columnName() != null ? col.columnName() : "";
            String comment = col.columnComment() != null ? col.columnComment() : "";
            boolean matchedCol = scanColumnName && Pattern.compile(matchExpr, Pattern.CASE_INSENSITIVE).matcher(columnName).find();
            boolean matchedComment = scanColumnComment && Pattern.compile(matchExpr, Pattern.CASE_INSENSITIVE).matcher(comment).find();
            return matchedCol || matchedComment;

        } else if ("DATA_TYPE".equals(matchType)) {
            if (!scanDataType) return false;
            String dataType = col.dataType() != null ? col.dataType().toLowerCase() : "";
            // matchExpr 可能是逗号分隔的字符串
            String[] keywords = matchExpr.toLowerCase().split("[,\\s]+");
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (StringUtils.isNotBlank(keyword) && dataType.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    /**
     * 根据匹配表达式类型判断是否命中
     */
    private boolean matchByExpr(String text, String expr, String exprType, boolean useRegex) {
        if (!StringUtils.isNotBlank(expr)) return false;
        text = text.toLowerCase();
        expr = expr.toLowerCase();

        if ("REGEX".equals(exprType) && useRegex) {
            try {
                return Pattern.matches(expr, text);
            } catch (Exception e) {
                log.warn("无效正则表达式: {}", expr);
                return false;
            }
        }

        // 默认按 CONTAINS ��理（兼容旧规则）
        String[] keywords = expr.split("[,\\s]+");
        for (String keyword : keywords) {
            keyword = keyword.trim();
            if (StringUtils.isNotBlank(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从旧版 JSON 格式的 ruleExpr 中提取 exprType
     */
    private String getExprTypeFromRuleExpr(String ruleExpr) {
        if (!StringUtils.isNotBlank(ruleExpr)) {
            return "CONTAINS"; // 默认
        }
        try {
            var json = JSONUtil.parseObj(ruleExpr);
            return json.getStr("exprType", "CONTAINS");
        } catch (Exception e) {
            // 不是 JSON 格式，直接用原值作为 keywords
            return "CONTAINS";
        }
    }

    private SecScanResultVO buildEmptyResult(SecSensitivityScanDTO dto, String batchNo, String dsName, long startTime) {
        SecScanResultVO result = new SecScanResultVO();
        result.setBatchNo(batchNo);
        result.setDsId(dto.getDsId());
        result.setDsName(dsName);
        result.setStartTime(startTime);
        result.setEndTime(System.currentTimeMillis());
        result.setCostMs(System.currentTimeMillis() - startTime);
        result.setStatus("SUCCESS");
        result.setSensitiveFields(Collections.emptyList());
        return result;
    }

    private String getConfidenceLabel(BigDecimal confidence) {
        if (confidence == null) return "未知";
        double v = confidence.doubleValue();
        if (v >= 90) return "高";
        if (v >= 70) return "中";
        if (v >= 50) return "低";
        return "未知";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmColumns(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        for (Long id : ids) {
            requireAccessibleRecord(id);
        }
        SecColumnSensitivity update = new SecColumnSensitivity();
        update.setConfirmed("1");
        return baseMapper.update(update, Wrappers.<SecColumnSensitivity>lambdaUpdate()
            .in(SecColumnSensitivity::getId, ids));
    }

    @Override
    public Long insert(SecColumnSensitivityVo vo) {
        datasourceHelper.getSysDatasource(vo.getDsId());
        SecColumnSensitivity entity = new SecColumnSensitivity();
        entity.setDsId(vo.getDsId());
        entity.setTableName(vo.getTableName());
        entity.setColumnName(vo.getColumnName());
        entity.setDataType(vo.getDataType());
        entity.setLevelCode(vo.getLevelCode());
        entity.setClassCode(vo.getClassCode());
        entity.setIdentifiedBy(vo.getIdentifiedBy() != null ? vo.getIdentifiedBy() : "MANUAL");
        entity.setScanTaskId(vo.getScanTaskId());
        entity.setConfirmed(vo.getConfirmed() != null ? vo.getConfirmed() : "0");
        baseMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int update(SecColumnSensitivityVo vo) {
        if (vo.getId() == null) {
            throw new ServiceException("记录ID不能为空");
        }
        SecColumnSensitivity existing = requireAccessibleRecord(vo.getId());
        if (vo.getDsId() != null && !vo.getDsId().equals(existing.getDsId())) {
            datasourceHelper.getSysDatasource(vo.getDsId());
        }
        SecColumnSensitivity entity = new SecColumnSensitivity();
        entity.setId(vo.getId());
        entity.setLevelCode(vo.getLevelCode());
        entity.setClassCode(vo.getClassCode());
        entity.setConfirmed(vo.getConfirmed());
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(Long[] ids) {
        for (Long id : ids) {
            requireAccessibleRecord(id);
        }
        return baseMapper.deleteBatchIds(List.of(ids));
    }

    private SecColumnSensitivity requireAccessibleRecord(Long id) {
        SecColumnSensitivity entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("敏感字段记录不存在: " + id);
        }
        datasourceHelper.getSysDatasource(entity.getDsId());
        return entity;
    }
}
