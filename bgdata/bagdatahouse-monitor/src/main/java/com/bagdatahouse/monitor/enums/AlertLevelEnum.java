package com.bagdatahouse.monitor.enums;

import java.util.Arrays;

/**
 * 告警级别枚举
 */
public enum AlertLevelEnum {

    INFO("INFO", "通知"),
    WARN("WARN", "警告"),
    ERROR("ERROR", "错误"),
    CRITICAL("CRITICAL", "严重");

    private final String code;
    private final String name;

    AlertLevelEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }

    public static AlertLevelEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }
}
