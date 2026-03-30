package com.bagdatahouse.governance.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 目标写入模式枚举
 */
@Getter
@AllArgsConstructor
public enum TargetModeEnum {

    APPEND("APPEND", "追加"),
    TRUNCATE("TRUNCATE", "清空重写"),
    UPSERT("UPSERT", "Upsert");

    private final String code;
    private final String label;

    public static String getLabel(String code) {
        for (TargetModeEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return code;
    }
}
