package com.bagdatahouse.governance.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 脱敏任务触发方式枚举
 */
@Getter
@AllArgsConstructor
public enum MaskTriggerTypeEnum {

    MANUAL("MANUAL", "手动触发"),
    SCHEDULED("SCHEDULED", "定时触发"),
    EVENT("EVENT", "事件触发");

    private final String code;
    private final String label;

    public static String getLabel(String code) {
        for (MaskTriggerTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return code;
    }
}
