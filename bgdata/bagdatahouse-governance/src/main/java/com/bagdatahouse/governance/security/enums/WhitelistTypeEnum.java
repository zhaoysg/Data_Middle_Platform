package com.bagdatahouse.governance.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 脱敏白名单豁免类型枚举
 */
@Getter
@AllArgsConstructor
public enum WhitelistTypeEnum {

    FULL_EXEMPT("FULL_EXEMPT", "完全豁免（不脱敏）"),
    PARTIAL_EXEMPT("PARTIAL_EXEMPT", "部分豁免（仅显示）");

    private final String code;
    private final String label;

    public static String getLabel(String code) {
        for (WhitelistTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return code;
    }
}
