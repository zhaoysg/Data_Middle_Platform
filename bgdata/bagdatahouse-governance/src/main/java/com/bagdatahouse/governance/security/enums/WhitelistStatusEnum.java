package com.bagdatahouse.governance.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 脱敏白名单状态枚举
 */
@Getter
@AllArgsConstructor
public enum WhitelistStatusEnum {

    ACTIVE("ACTIVE", "生效"),
    EXPIRED("EXPIRED", "已过期"),
    REVOKED("REVOKED", "已撤销");

    private final String code;
    private final String label;

    public static String getLabel(String code) {
        for (WhitelistStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return code;
    }
}
