package com.bagdatahouse.dqc.enums;

/**
 * 规则适用级别枚举
 */
public enum ApplyLevelEnum {

    TABLE("TABLE", "表级规则", "适用于整张表的规则"),
    COLUMN("COLUMN", "字段级规则", "适用于单个字段的规则"),
    CROSS_FIELD("CROSS_FIELD", "跨字段规则", "适用于多个字段间关系的规则"),
    CROSS_TABLE("CROSS_TABLE", "跨表规则", "适用于跨表关系的规则"),
    DATABASE("DATABASE", "数据库级规则", "适用于整个数据库的规则");

    private final String code;
    private final String name;
    private final String description;

    ApplyLevelEnum(String code, String name, String description) {
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

    public static ApplyLevelEnum fromCode(String code) {
        for (ApplyLevelEnum level : values()) {
            if (level.code.equalsIgnoreCase(code)) {
                return level;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        ApplyLevelEnum level = fromCode(code);
        return level != null ? level.name : code;
    }
}
