package com.bagdatahouse.dqc.enums;

/**
 * 规则错误级别枚举
 */
public enum ErrorLevelEnum {

    LOW("LOW", "低", "轻微问题，仅记录", "#52c41a"),
    MEDIUM("MEDIUM", "中", "一般问题，建议关注", "#faad14"),
    HIGH("HIGH", "高", "严重问题，需要处理", "#fa8c16"),
    CRITICAL("CRITICAL", "严重", "阻断性问题，必须立即处理", "#ff4d4f");

    private final String code;
    private final String name;
    private final String description;
    private final String color;

    ErrorLevelEnum(String code, String name, String description, String color) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.color = color;
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

    public String getColor() {
        return color;
    }

    public static ErrorLevelEnum fromCode(String code) {
        for (ErrorLevelEnum level : values()) {
            if (level.code.equalsIgnoreCase(code)) {
                return level;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        ErrorLevelEnum level = fromCode(code);
        return level != null ? level.name : code;
    }

    public static String getColorByCode(String code) {
        ErrorLevelEnum level = fromCode(code);
        return level != null ? level.color : "#8c8c8c";
    }
}
