package org.dromara.metadata.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.metadata.domain.SecMaskStrategy;
import org.dromara.metadata.domain.SecMaskStrategyDetail;
import org.dromara.metadata.mapper.SecMaskStrategyDetailMapper;
import org.dromara.metadata.mapper.SecMaskStrategyMapper;
import org.dromara.metadata.service.IMaskQueryService;
import org.dromara.metadata.service.ISecAccessLogService;
import org.dromara.metadata.service.ISecMaskTemplateService;
import org.dromara.metadata.support.DatasourceHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 脱敏查询服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MaskQueryServiceImpl implements IMaskQueryService {

    private final DatasourceHelper datasourceHelper;
    private final SecMaskStrategyMapper strategyMapper;
    private final SecMaskStrategyDetailMapper detailMapper;
    private final ISecAccessLogService accessLogService;
    private final ISecMaskTemplateService maskTemplateService;

    private static final int DEFAULT_LIMIT = 1000;

    private static final List<String> FORBIDDEN_KEYWORDS = List.of(
        "INFORMATION_SCHEMA",
        "MYSQL",
        "PERFORMANCE_SCHEMA",
        "SLEEP(",
        "BENCHMARK(",
        "EXTRACTVALUE",
        "UPDATEXML",
        "PG_READ_FILE",
        "PG_LO_READ",
        "COPY ",
        "PG_SLEEP",
        "INTO OUTFILE",
        "INTO DUMPFILE",
        "LOAD_FILE"
    );

    @Override
    @Transactional(readOnly = true)
    public MaskQueryResult query(Long dsId, String sql, Long userId, String userName, String ipAddress) {
        ValidationResult validation = validateSql(sql);
        if (!validation.valid()) {
            throw new ServiceException(validation.message());
        }

        List<SecMaskStrategy> strategies = strategyMapper.selectList(
            Wrappers.<SecMaskStrategy>lambdaQuery()
                .eq(SecMaskStrategy::getDsId, dsId)
                .eq(SecMaskStrategy::getEnabled, "1")
        );

        Map<String, String> columnMaskMap = new HashMap<>();
        for (SecMaskStrategy s : strategies) {
            List<SecMaskStrategyDetail> details = detailMapper.selectList(
                Wrappers.<SecMaskStrategyDetail>lambdaQuery()
                    .eq(SecMaskStrategyDetail::getStrategyId, s.getId())
            );
            for (SecMaskStrategyDetail d : details) {
                String key = dsId + "." + d.getTableName() + "." + d.getColumnName();
                columnMaskMap.put(key, d.getTemplateCode());
            }
        }

        Map<String, String> templateExprMap = maskTemplateService.getTemplateExprMap();

        String maskedSql = rewriteSql(sql, columnMaskMap, templateExprMap);
        maskedSql = enforceLimit(maskedSql, DEFAULT_LIMIT);

        long start = System.currentTimeMillis();
        JdbcTemplate jdbc = datasourceHelper.getJdbcTemplate(dsId);
        List<Map<String, Object>> rows;
        String status = "SUCCESS";
        String errorMsg = null;

        try {
            rows = jdbc.queryForList(maskedSql);
        } catch (Exception e) {
            status = "FAILED";
            errorMsg = e.getMessage();
            rows = List.of();
            log.error("脱敏查询执行失败: {}", e.getMessage());
        }

        boolean truncated = rows.size() >= DEFAULT_LIMIT;
        long elapsedMs = System.currentTimeMillis() - start;

        accessLogService.logAsync(dsId, sql, maskedSql, rows.size(),
            List.copyOf(columnMaskMap.values()), elapsedMs, status, errorMsg,
            userId, userName, ipAddress);

        return new MaskQueryResult(rows, rows.size(), truncated,
            List.copyOf(columnMaskMap.values()), elapsedMs, sql);
    }

    @Override
    public ValidationResult validateSql(String sql) {
        if (sql == null || sql.isBlank()) {
            return new ValidationResult(false, "SQL不能为空", false, false, false, 0);
        }

        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);

        // 必须是 SELECT 开头（支持 WITH ... AS SELECT 形式）
        if (!upper.startsWith("SELECT") || upper.matches("\\s*SELECT\\s*.*\\b(INTO|DELETE|UPDATE|INSERT|DROP|ALTER|CREATE|TRUNCATE|EXEC|EXECUTE)\\b.*")) {
            return new ValidationResult(false, "仅支持SELECT查询，其他SQL语句已被禁止", false, false, false, 0);
        }

        // 危险关键词黑名单（深度防御）
        for (String kw : FORBIDDEN_KEYWORDS) {
            if (upper.contains(kw)) {
                return new ValidationResult(false, "禁止使用危险SQL语句: " + kw, false, true, false, 0);
            }
        }

        // 检查子查询中是否有危险操作
        if (upper.contains("WHERE ") && upper.matches(".*WHERE\\s+.*\\b(SELECT|INSERT|UPDATE|DELETE)\\b.*")) {
            return new ValidationResult(false, "禁止在WHERE子句中使用子查询修改数据", false, true, false, 0);
        }

        boolean hasLimit = upper.contains("LIMIT");
        int estimatedRows = hasLimit ? extractLimit(upper) : -1;

        return new ValidationResult(true, "SQL验证通过", true, true, hasLimit, estimatedRows);
    }

    private String rewriteSql(String sql, Map<String, String> colMap, Map<String, String> templateMap) {
        String result = sql;
        for (Map.Entry<String, String> entry : colMap.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("\\.");
            if (parts.length < 3) {
                continue;
            }
            String columnName = parts[2];
            String templateCode = entry.getValue();
            String expr = templateMap.get(templateCode);

            if (expr != null) {
                String replacement = applyMaskExpression(expr, columnName);
                String regex = "(?<![a-zA-Z0-9_`])" + Pattern.quote(columnName) + "(?![a-zA-Z0-9_`])";
                result = result.replaceAll(regex, replacement);
            }
        }
        return result;
    }

    private String applyMaskExpression(String maskExpr, String colName) {
        return maskExpr.replace("{col}", "`" + colName + "`");
    }

    private String enforceLimit(String sql, int limit) {
        String upper = sql.trim().toUpperCase(Locale.ROOT);
        if (upper.contains("LIMIT")) {
            Pattern pattern = Pattern.compile("(?i)\\s+LIMIT\\s+\\d+(\\s*,\\s*\\d+)?", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(sql);
            return matcher.replaceAll(" LIMIT " + limit);
        }
        return sql + " LIMIT " + limit;
    }

    private int extractLimit(String upper) {
        Matcher m = Pattern.compile("LIMIT\\s+(\\d+)").matcher(upper);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
