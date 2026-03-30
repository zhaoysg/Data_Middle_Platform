package com.bagdatahouse.governance.security.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 脱敏任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum MaskTaskStatusEnum {

    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String label;

    public static String getLabel(String code) {
        for (MaskTaskStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return code;
    }
}
