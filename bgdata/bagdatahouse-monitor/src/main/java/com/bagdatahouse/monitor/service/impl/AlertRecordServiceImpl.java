package com.bagdatahouse.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.enums.ResponseCode;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.dto.SensityAlertDTO;
import com.bagdatahouse.core.entity.MonitorAlertRecord;
import com.bagdatahouse.core.mapper.MonitorAlertRecordMapper;
import com.bagdatahouse.monitor.enums.AlertLevelEnum;
import com.bagdatahouse.monitor.enums.AlertStatusEnum;
import com.bagdatahouse.monitor.service.AlertRecordService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 告警记录服务实现
 */
@Service
public class AlertRecordServiceImpl extends ServiceImpl<MonitorAlertRecordMapper, MonitorAlertRecord>
        implements AlertRecordService {

    private static final Logger log = LoggerFactory.getLogger(AlertRecordServiceImpl.class);
    private static final DateTimeFormatter ALERT_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final AtomicLong alertSeq = new AtomicLong(System.currentTimeMillis() % 10000);

    @Override
    public Result<Page<MonitorAlertRecord>> page(
            Integer pageNum,
            Integer pageSize,
            String ruleName,
            String alertLevel,
            String status,
            String targetType,
            String startDate,
            String endDate
    ) {
        return page(pageNum, pageSize, ruleName, alertLevel, status, targetType, startDate, endDate, null, null);
    }

    @Override
    public Result<Page<MonitorAlertRecord>> page(
            Integer pageNum,
            Integer pageSize,
            String ruleName,
            String alertLevel,
            String status,
            String targetType,
            String startDate,
            String endDate,
            String ruleType,
            String sensitivityLevel
    ) {
        LambdaQueryWrapper<MonitorAlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(ruleName), MonitorAlertRecord::getRuleName, ruleName)
               .eq(StringUtils.isNotBlank(alertLevel), MonitorAlertRecord::getAlertLevel, alertLevel)
               .eq(StringUtils.isNotBlank(status), MonitorAlertRecord::getStatus, status)
               .eq(StringUtils.isNotBlank(targetType), MonitorAlertRecord::getTargetType, targetType)
               .ge(StringUtils.isNotBlank(startDate), MonitorAlertRecord::getCreateTime,
                       parseDate(startDate, true))
               .le(StringUtils.isNotBlank(endDate), MonitorAlertRecord::getCreateTime,
                       parseDate(endDate, false))
               // ruleType 映射到 alertTitle（SENSITIVE 告警的 ruleName 字段存的是告警标题）
               .like(isSensitiveAlertType(ruleType) && StringUtils.isNotBlank(getSensitiveAlertTitleLike(ruleType)),
                       MonitorAlertRecord::getRuleName,
                       getSensitiveAlertTitleLike(ruleType))
               .eq(StringUtils.isNotBlank(sensitivityLevel), MonitorAlertRecord::getSensitivityLevel, sensitivityLevel)
               .orderByDesc(MonitorAlertRecord::getCreateTime);

        Page<MonitorAlertRecord> page = new Page<>(pageNum, pageSize);
        Page<MonitorAlertRecord> result = baseMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    private boolean isSensitiveAlertType(String ruleType) {
        return "SENSITIVE_FIELD_SPIKE".equals(ruleType)
                || "SENSITIVE_LEVEL_CHANGE".equals(ruleType)
                || "SENSITIVE_ACCESS_ANOMALY".equals(ruleType)
                || "SENSITIVE_UNREVIEWED_LONG".equals(ruleType)
                || "SENSITIVE".equals(ruleType);
    }

    private String getSensitiveAlertTitleLike(String ruleType) {
        return switch (ruleType) {
            case "SENSITIVE_FIELD_SPIKE" -> "敏感字段数突增";
            case "SENSITIVE_LEVEL_CHANGE" -> "敏感等级变更";
            case "SENSITIVE_ACCESS_ANOMALY" -> "敏感字段访问异常";
            case "SENSITIVE_UNREVIEWED_LONG" -> "待审核超期";
            case "SENSITIVE" -> "敏感";
            default -> null;
        };
    }

    @Override
    public Result<MonitorAlertRecord> getById(Long id) {
        MonitorAlertRecord record = baseMapper.selectById(id);
        if (record == null) {
            return Result.fail(ResponseCode.NOT_FOUND, "告警记录不存在");
        }
        return Result.success(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> markAsRead(Long id, Long readUser) {
        MonitorAlertRecord record = baseMapper.selectById(id);
        if (record == null) {
            return Result.fail(ResponseCode.NOT_FOUND, "告警记录不存在");
        }
        String currentStatus = record.getStatus();
        if (AlertStatusEnum.RESOLVED.getCode().equals(currentStatus)) {
            return Result.fail(ResponseCode.BAD_REQUEST, "已解决的告警无法标记为已读");
        }
        if (AlertStatusEnum.READ.getCode().equals(currentStatus)) {
            return Result.success();
        }
        record.setStatus(AlertStatusEnum.READ.getCode());
        record.setReadTime(LocalDateTime.now());
        record.setReadUser(readUser);
        baseMapper.updateById(record);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchMarkAsRead(Long[] ids, Long readUser) {
        for (Long id : ids) {
            Result<Void> r = markAsRead(id, readUser);
            if (!r.isSuccess()) {
                return r;
            }
        }
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> resolve(Long id, Long resolveUser, String resolveComment) {
        MonitorAlertRecord record = baseMapper.selectById(id);
        if (record == null) {
            return Result.fail(ResponseCode.NOT_FOUND, "告警记录不存在");
        }
        if (AlertStatusEnum.RESOLVED.getCode().equals(record.getStatus())) {
            return Result.fail(ResponseCode.BAD_REQUEST, "该告警已经解决，请勿重复操作");
        }
        record.setStatus(AlertStatusEnum.RESOLVED.getCode());
        record.setResolvedTime(LocalDateTime.now());
        record.setResolveUser(resolveUser);
        record.setResolveComment(resolveComment);
        baseMapper.updateById(record);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchResolve(Long[] ids, Long resolveUser, String resolveComment) {
        for (Long id : ids) {
            Result<Void> r = resolve(id, resolveUser, resolveComment);
            if (!r.isSuccess()) {
                return r;
            }
        }
        return Result.success();
    }

    @Override
    public Result<Map<String, Object>> getAlertOverview() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1);

        long pendingCount = baseMapper.selectCount(
                new LambdaQueryWrapper<MonitorAlertRecord>()
                        .in(MonitorAlertRecord::getStatus,
                                AlertStatusEnum.PENDING.getCode(), AlertStatusEnum.SENT.getCode())
                        .ge(MonitorAlertRecord::getCreateTime, today));

        long todayTotal = baseMapper.selectCount(
                new LambdaQueryWrapper<MonitorAlertRecord>()
                        .ge(MonitorAlertRecord::getCreateTime, today));

        long todayResolved = baseMapper.selectCount(
                new LambdaQueryWrapper<MonitorAlertRecord>()
                        .eq(MonitorAlertRecord::getStatus, AlertStatusEnum.RESOLVED.getCode())
                        .ge(MonitorAlertRecord::getResolvedTime, today));

        long criticalCount = baseMapper.selectCount(
                new LambdaQueryWrapper<MonitorAlertRecord>()
                        .eq(MonitorAlertRecord::getAlertLevel, AlertLevelEnum.CRITICAL.getCode())
                        .in(MonitorAlertRecord::getStatus,
                                AlertStatusEnum.PENDING.getCode(), AlertStatusEnum.SENT.getCode()));

        Map<String, Object> overview = new HashMap<>();
        overview.put("pendingCount", pendingCount);
        overview.put("todayTotal", todayTotal);
        overview.put("todayResolved", todayResolved);
        overview.put("criticalCount", criticalCount);
        return Result.success(overview);
    }

    @Override
    public Result<Map<String, Object>> getAlertLevelDistribution(String startDate, String endDate) {
        LambdaQueryWrapper<MonitorAlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(StringUtils.isNotBlank(startDate), MonitorAlertRecord::getCreateTime,
                parseDate(startDate, true));
        wrapper.le(StringUtils.isNotBlank(endDate), MonitorAlertRecord::getCreateTime,
                parseDate(endDate, false));

        List<MonitorAlertRecord> records = baseMapper.selectList(wrapper);

        Map<String, Long> distribution = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getAlertLevel() != null ? r.getAlertLevel() : "UNKNOWN",
                        Collectors.counting()));

        Map<String, Object> result = new HashMap<>();
        for (AlertLevelEnum level : AlertLevelEnum.values()) {
            result.put(level.getCode(), distribution.getOrDefault(level.getCode(), 0L));
        }
        return Result.success(result);
    }

    @Override
    public Result<Map<String, Object>> getAlertTrend(String days) {
        int dayCount = 7;
        try {
            dayCount = Integer.parseInt(days);
        } catch (NumberFormatException ignored) {}

        LocalDateTime startTime = LocalDateTime.now().minusDays(dayCount).toLocalDate().atStartOfDay();

        LambdaQueryWrapper<MonitorAlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(MonitorAlertRecord::getCreateTime, startTime);
        List<MonitorAlertRecord> records = baseMapper.selectList(wrapper);

        Map<String, Map<String, Long>> dailyStats = new HashMap<>();
        for (MonitorAlertRecord record : records) {
            if (record.getCreateTime() == null) continue;
            String dateKey = record.getCreateTime().toLocalDate().toString();
            dailyStats.computeIfAbsent(dateKey, k -> {
                Map<String, Long> m = new HashMap<>();
                m.put("total", 0L);
                m.put("INFO", 0L);
                m.put("WARN", 0L);
                m.put("ERROR", 0L);
                m.put("CRITICAL", 0L);
                return m;
            });
            dailyStats.get(dateKey).merge("total", 1L, Long::sum);
            String level = record.getAlertLevel() != null ? record.getAlertLevel() : "UNKNOWN";
            dailyStats.get(dateKey).merge(level, 1L, Long::sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dailyStats", dailyStats);
        return Result.success(result);
    }

    private LocalDateTime parseDate(String dateStr, boolean startOfDay) {
        if (StringUtils.isBlank(dateStr)) return null;
        try {
            if (startOfDay) {
                return LocalDateTime.parse(dateStr + " 00:00:00",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                return LocalDateTime.parse(dateStr + " 23:59:59",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== SENSITIVE 类型告警记录实现 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createSensityAlert(SensityAlertDTO dto) {
        if (dto == null) {
            return Result.fail(ResponseCode.BAD_REQUEST, "告警信息不能为空");
        }

        String alertNo = generateAlertNo();
        MonitorAlertRecord record = MonitorAlertRecord.builder()
                .alertNo(alertNo)
                .ruleName(dto.getAlertTitle())
                .alertLevel(dto.getAlertLevel() != null ? dto.getAlertLevel() : "WARN")
                .alertTitle(dto.getAlertTitle())
                .alertContent(dto.getAlertContent())
                .sensitivityLevel(dto.getSensitivityLevel())
                .sensitivityTable(dto.getSensitivityTable())
                .sensitivityColumn(dto.getSensitivityColumn())
                .sensitivityDsId(dto.getSensitivityDsId())
                .scanBatchNo(dto.getScanBatchNo())
                .triggerValue(dto.getTriggerValue())
                .thresholdValue(dto.getThresholdValue())
                .status(AlertStatusEnum.PENDING.getCode())
                .sentTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();

        baseMapper.insert(record);
        log.info("创建 SENSITIVE 告警记录: alertNo={}, type={}, level={}, table={}.{}",
                alertNo, dto.getAlertType(), dto.getAlertLevel(),
                dto.getSensitivityTable(), dto.getSensitivityColumn());
        return Result.success(record.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Integer> createSensityAlertBatch(List<SensityAlertDTO> alerts) {
        if (alerts == null || alerts.isEmpty()) {
            return Result.success(0);
        }
        int successCount = 0;
        for (SensityAlertDTO dto : alerts) {
            try {
                Result<Long> r = createSensityAlert(dto);
                if (r.isSuccess()) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("批量创建 SENSITIVE 告警失败: {}, error={}",
                        dto.getAlertTitle(), e.getMessage());
            }
        }
        log.info("批量创建 SENSITIVE 告警: 请求{}条，成功{}条", alerts.size(), successCount);
        return Result.success(successCount);
    }

    private String generateAlertNo() {
        String timestamp = LocalDateTime.now().format(ALERT_NO_FORMATTER);
        long seq = alertSeq.incrementAndGet() % 10000;
        return "ALERT_" + timestamp + "_" + String.format("%04d", seq);
    }
}
