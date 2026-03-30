package com.bagdatahouse.governance.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 脱敏白名单实体类型枚举
 */
@Getter
@AllArgsConstructor
public enum WhitelistEntityTypeEnum {

    USER("USER", "用户"),
    ROLE("ROLE", "角色");

    private final String code;
    private final String label;

    public static String getLabel(String code) {
        for (WhitelistEntityTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return code;
    }
}
