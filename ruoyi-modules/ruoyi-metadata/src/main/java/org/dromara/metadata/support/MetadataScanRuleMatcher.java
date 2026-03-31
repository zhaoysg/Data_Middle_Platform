package org.dromara.metadata.support;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.metadata.domain.bo.MetadataScanBo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 元数据扫描规则匹配器
 */
public final class MetadataScanRuleMatcher {

    public static final String MODE_ALL = "ALL";
    public static final String MODE_TABLES = "TABLES";
    public static final String MODE_RULE = "RULE";

    public static final String RULE_CONTAINS = "CONTAINS";
    public static final String RULE_PREFIX = "PREFIX";
    public static final String RULE_SUFFIX = "SUFFIX";
    public static final String RULE_EXACT = "EXACT";
    public static final String RULE_REGEX = "REGEX";

    private MetadataScanRuleMatcher() {
    }

    public static String resolveScanMode(MetadataScanBo bo) {
        if (bo == null) {
            return MODE_ALL;
        }
        if (bo.getTableNames() != null && !bo.getTableNames().isEmpty()
            && StringUtils.equalsAnyIgnoreCase(StringUtils.trim(bo.getScanMode()), MODE_ALL, "")) {
            return MODE_TABLES;
        }
        if (StringUtils.isBlank(bo.getScanMode())) {
            return MODE_ALL;
        }
        return bo.getScanMode().trim().toUpperCase(Locale.ROOT);
    }

    public static List<String> resolveTables(List<String> allTables, MetadataScanBo bo) {
        List<String> sourceTables = allTables == null ? List.of() : allTables;
        String scanMode = resolveScanMode(bo);
        return switch (scanMode) {
            case MODE_TABLES -> resolveSelectedTables(sourceTables, bo == null ? null : bo.getTableNames());
            case MODE_RULE -> resolveRuleTables(sourceTables, bo);
            default -> new ArrayList<>(sourceTables);
        };
    }

    private static List<String> resolveSelectedTables(List<String> allTables, List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return List.of();
        }
        Set<String> selected = new LinkedHashSet<>();
        for (String tableName : tableNames) {
            if (StringUtils.isNotBlank(tableName)) {
                selected.add(tableName.trim());
            }
        }
        if (selected.isEmpty()) {
            return List.of();
        }
        return allTables.stream()
            .filter(selected::contains)
            .toList();
    }

    private static List<String> resolveRuleTables(List<String> allTables, MetadataScanBo bo) {
        if (bo == null) {
            return List.of();
        }
        List<String> includes = splitPatterns(bo.getIncludePattern());
        if (includes.isEmpty()) {
            return List.of();
        }
        List<String> excludes = splitPatterns(bo.getExcludePattern());
        String ruleType = normalizeRuleType(bo.getRuleType());
        boolean ignoreCase = !Boolean.FALSE.equals(bo.getIgnoreCase());
        return allTables.stream()
            .filter(tableName -> matchesAny(tableName, includes, ruleType, ignoreCase))
            .filter(tableName -> !matchesAny(tableName, excludes, ruleType, ignoreCase))
            .toList();
    }

    private static boolean matchesAny(String tableName, List<String> patterns, String ruleType, boolean ignoreCase) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (matches(tableName, pattern, ruleType, ignoreCase)) {
                return true;
            }
        }
        return false;
    }

    static boolean matches(String tableName, String pattern, String ruleType, boolean ignoreCase) {
        if (StringUtils.isBlank(tableName) || StringUtils.isBlank(pattern)) {
            return false;
        }
        String source = ignoreCase ? tableName.toLowerCase(Locale.ROOT) : tableName;
        String rule = ignoreCase ? pattern.toLowerCase(Locale.ROOT) : pattern;
        return switch (normalizeRuleType(ruleType)) {
            case RULE_PREFIX -> source.startsWith(rule);
            case RULE_SUFFIX -> source.endsWith(rule);
            case RULE_EXACT -> source.equals(rule);
            case RULE_REGEX -> buildRegex(pattern, ignoreCase).matcher(tableName).find();
            default -> source.contains(rule);
        };
    }

    private static Pattern buildRegex(String pattern, boolean ignoreCase) {
        return ignoreCase
            ? Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
            : Pattern.compile(pattern);
    }

    private static List<String> splitPatterns(String rawPattern) {
        if (StringUtils.isBlank(rawPattern)) {
            return List.of();
        }
        String[] items = rawPattern.split("[,\\r\\n]+");
        List<String> patterns = new ArrayList<>(items.length);
        for (String item : items) {
            if (StringUtils.isNotBlank(item)) {
                patterns.add(item.trim());
            }
        }
        return patterns;
    }

    private static String normalizeRuleType(String ruleType) {
        if (StringUtils.isBlank(ruleType)) {
            return RULE_CONTAINS;
        }
        return ruleType.trim().toUpperCase(Locale.ROOT);
    }
}
