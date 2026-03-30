package com.bagdatahouse.monitor.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 告警规则类型枚举
 */
public enum AlertRuleTypeEnum {

    QUALITY("QUALITY", "质量告警", "质检规则执行失败或质量分数低于阈值"),
    AVAILABILITY("AVAILABILITY", "可用性告警", "数据源连接失败或任务执行超时"),
    PERFORMANCE("PERFORMANCE", "性能告警", "任务执行时间超过阈值"),
    SYSTEM("SYSTEM", "系统告警", "平台自身资源使用率异常"),
    FLUCTUATION("FLUCTUATION", "波动告警", "关键指标较历史波动超过阈值"),
    // SENSITIVE 类型下包含 4 种子告警类型
    SENSITIVE_FIELD_SPIKE("SENSITIVE_FIELD_SPIKE", "敏感字段突增告警", "扫描发现敏感字段数较昨日增长超过阈值"),
    SENSITIVE_LEVEL_CHANGE("SENSITIVE_LEVEL_CHANGE", "敏感等级变更告警", "字段敏感等级被降级时触发"),
    SENSITIVE_ACCESS_ANOMALY("SENSITIVE_ACCESS_ANOMALY", "敏感字段访问异常告警", "敏感字段在非工作时间或异常频率被访问"),
    SENSITIVE_UNREVIEWED_LONG("SENSITIVE_UNREVIEWED_LONG", "敏感字段待审核超期告警", "敏感字段待审核超过 N 天时触发");

    private final String code;
    private final String name;
    private final String description;

    AlertRuleTypeEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }

    public static AlertRuleTypeEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }

    public static String getNameByCode(String code) {
        AlertRuleTypeEnum e = fromCode(code);
        return e != null ? e.name : code;
    }

    public static List<String> allCodes() {
        return Arrays.stream(values()).map(AlertRuleTypeEnum::getCode).toList();
    }
}
