package org.dromara.metadata.support;

import lombok.extern.slf4j.Slf4j;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数仓分层自动识别引擎
 * <p>
 * 根据表名前缀、schema名称、列名特征自动推断 ODS/DWD/DWS/ADS 层。
 * 识别规则可通过配置文件或数据库表管理（metadata_layer_rule）。
 * <p>
 * 默认规则：
 * - ODS: 前缀 ods_，schema public/dw_ods
 * - DWD: 前缀 dwd_，schema dw_dwd
 * - DWS: 前缀 dws_，schema dw_dws
 * - ADS: 前缀 ads_ 或 rpt_，schema dw_ads/report
 */
@Slf4j
@Component
public class MetadataLayerResolver {

    /** 层信息 record */
    public record LayerMatch(String layerCode, String layerName, double confidence) {}

    /** 单条识别规则 */
    public record LayerRule(
        String layerCode,
        Pattern tableNamePattern,
        Pattern schemaPattern,
        Pattern columnNamePattern,
        double weight
    ) {}

    private final Map<String, LayerRule> rules = new HashMap<>();

    public MetadataLayerResolver() {
        initDefaultRules();
    }

    /**
     * 初始化默认识别规则
     */
    private void initDefaultRules() {
        // ODS: Operational Data Store — 原始层，前缀 ods_ 或 staging_
        addRule(new LayerRule("ODS",
            Pattern.compile("^(ods|dwd|dws|ads|rpt)_", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(public|dw_ods|staging)", Pattern.CASE_INSENSITIVE),
            null,
            1.0
        ));

        // ODS: 原始层特征（无特定前缀但schema为ods）
        addRule(new LayerRule("ODS",
            null,
            Pattern.compile("^(ods_|staging|raw)", Pattern.CASE_INSENSITIVE),
            null,
            0.8
        ));

        // DWD: 明细宽表层，前缀 dwd_
        addRule(new LayerRule("DWD",
            Pattern.compile("^(dwd_)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(dw_dwd|ods)", Pattern.CASE_INSENSITIVE),
            null,
            1.0
        ));

        // DWS: 汇总宽表层，前缀 dws_
        addRule(new LayerRule("DWS",
            Pattern.compile("^(dws_)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(dw_dws|dwd)", Pattern.CASE_INSENSITIVE),
            null,
            1.0
        ));

        // ADS: 应用层，前缀 ads_ 或 rpt_
        addRule(new LayerRule("ADS",
            Pattern.compile("^(ads_|rpt_|app_)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(dw_ads|report|app)", Pattern.CASE_INSENSITIVE),
            null,
            1.0
        ));

        // DIM: 维度表，前缀 dim_
        addRule(new LayerRule("DIM",
            Pattern.compile("^(dim_)", Pattern.CASE_INSENSITIVE),
            null,
            null,
            1.0
        ));
    }

    /**
     * 注册自定义识别规则
     */
    public void addRule(LayerRule rule) {
        if (rule != null && rule.layerCode() != null) {
            rules.put(rule.layerCode(), rule);
        }
    }

    /**
     * 自动识别表所属数仓分层
     *
     * @param tableName 表名
     * @param schemaName schema名（PostgreSQL专用，可为null）
     * @param columnNames 列名列表（用于辅助判断）
     * @return 识别结果，找不到返回 null
     */
    public LayerMatch resolve(String tableName, String schemaName, List<String> columnNames) {
        if (tableName == null || tableName.isBlank()) {
            return null;
        }

        LayerMatch bestMatch = null;
        double bestConfidence = 0;

        for (LayerRule rule : rules.values()) {
            double confidence = calculateConfidence(rule, tableName, schemaName, columnNames);
            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                bestMatch = new LayerMatch(rule.layerCode(), resolveLayerName(rule.layerCode()), confidence);
            }
        }

        if (bestConfidence > 0) {
            log.debug("分层识别: table={}, schema={}, layer={}, confidence={}",
                tableName, schemaName, bestMatch.layerCode(), String.format("%.2f", bestConfidence));
        }
        return bestMatch;
    }

    /**
     * 计算规则匹配置信度
     */
    private double calculateConfidence(LayerRule rule, String tableName, String schemaName, List<String> columnNames) {
        double score = 0;

        // 表名匹配（权重最高）
        if (rule.tableNamePattern() != null && rule.tableNamePattern().matcher(tableName).find()) {
            score += 0.7 * rule.weight();
        }

        // schema匹配
        if (schemaName != null && rule.schemaPattern() != null && rule.schemaPattern().matcher(schemaName).find()) {
            score += 0.25 * rule.weight();
        }

        // 列名特征匹配（用于补充判断）
        if (columnNames != null && rule.columnNamePattern() != null) {
            for (String col : columnNames) {
                if (rule.columnNamePattern().matcher(col).find()) {
                    score += 0.05 * rule.weight();
                    break;
                }
            }
        }

        return Math.min(score, 1.0);
    }

    /**
     * 从DataSourceAdapter推断schema名称
     */
    public static String inferSchemaName(DataSourceAdapter adapter) {
        // PostgreSQL 适配器可以通过 adapter.getClass() 判断
        if (adapter.getClass().getSimpleName().contains("Postgres")) {
            // PostgreSQL 默认 schema 为 public
            return "public";
        }
        return null;
    }

    private String resolveLayerName(String layerCode) {
        return switch (layerCode) {
            case "ODS" -> "操作数据层";
            case "DWD" -> "明细宽表层";
            case "DWS" -> "汇总宽表层";
            case "ADS" -> "应用数据层";
            case "DIM" -> "维度层";
            default -> layerCode;
        };
    }

    /**
     * 根据层代码获取颜色
     */
    public static String getLayerColor(String layerCode) {
        return switch (layerCode) {
            case "ODS" -> "#E8E8E8";
            case "DWD" -> "#5BC2E7";
            case "DWS" -> "#4A90D9";
            case "ADS", "DIM" -> "#2D5F8B";
            default -> "#999999";
        };
    }
}
