package com.bagdatahouse.governance.security.enums;

/**
 * 字段匹配类型枚举
 */
public enum MatchTypeEnum {

    COLUMN_NAME("COLUMN_NAME", "列名匹配"),
    COLUMN_COMMENT("COLUMN_COMMENT", "注释匹配"),
    DATA_TYPE("DATA_TYPE", "数据类型"),
    REGEX("REGEX", "正则表达式");

    private final String code;
    private final String label;

    MatchTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String getLabelByCode(String code) {
        for (MatchTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getLabel();
            }
        }
        return code;
    }
}
