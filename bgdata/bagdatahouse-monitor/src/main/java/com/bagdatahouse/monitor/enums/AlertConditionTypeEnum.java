package com.bagdatahouse.monitor.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 告警条件类型枚举
 */
public enum AlertConditionTypeEnum {

    GT("GT", "大于"),
    LT("LT", "小于"),
    EQ("EQ", "等于"),
    NE("NE", "不等于"),
    BETWEEN("BETWEEN", "区间"),
    FLUCTUATION("FLUCTUATION", "波动检测");

    private final String code;
    private final String name;

    AlertConditionTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }

    public static AlertConditionTypeEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }

    public static String getNameByCode(String code) {
        AlertConditionTypeEnum e = fromCode(code);
        return e != null ? e.name : code;
    }

    public static List<String> allCodes() {
        return Arrays.stream(values()).map(AlertConditionTypeEnum::getCode).toList();
    }
}
