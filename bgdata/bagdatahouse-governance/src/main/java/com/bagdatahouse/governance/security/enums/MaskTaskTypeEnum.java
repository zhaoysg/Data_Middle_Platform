package com.bagdatahouse.governance.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 脱敏任务类型枚举
 */
@Getter
@AllArgsConstructor
public enum MaskTaskTypeEnum {

    STATIC("STATIC", "静态脱敏"),
    DYNAMIC("DYNAMIC", "动态脱敏");

    private final String code;
    private final String label;

    public static String getLabel(String code) {
        for (MaskTaskTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return code;
    }
}
