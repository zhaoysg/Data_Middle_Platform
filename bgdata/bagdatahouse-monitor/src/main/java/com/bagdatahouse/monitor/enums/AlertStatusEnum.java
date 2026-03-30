package com.bagdatahouse.monitor.enums;

import java.util.Arrays;

/**
 * 告警状态枚举
 */
public enum AlertStatusEnum {

    PENDING("PENDING", "待发送"),
    SENT("SENT", "已发送"),
    READ("READ", "已读"),
    RESOLVED("RESOLVED", "已解决");

    private final String code;
    private final String name;

    AlertStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }

    public static AlertStatusEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }
}
