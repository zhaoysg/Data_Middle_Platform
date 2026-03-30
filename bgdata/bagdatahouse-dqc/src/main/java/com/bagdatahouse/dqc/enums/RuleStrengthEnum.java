package com.bagdatahouse.dqc.enums;

/**
 * 规则强度枚举（强规则 vs 弱规则）
 */
public enum RuleStrengthEnum {

    STRONG("STRONG", "强规则", "失败时阻塞下游任务执行并立即告警"),
    WEAK("WEAK", "弱规则", "失败时仅记录日志并告警，不阻塞下游任务");

    private final String code;
    private final String name;
    private final String description;

    RuleStrengthEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static RuleStrengthEnum fromCode(String code) {
        for (RuleStrengthEnum strength : values()) {
            if (strength.code.equalsIgnoreCase(code)) {
                return strength;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        RuleStrengthEnum strength = fromCode(code);
        return strength != null ? strength.name : code;
    }

    public boolean isStrong() {
        return this == STRONG;
    }
}
