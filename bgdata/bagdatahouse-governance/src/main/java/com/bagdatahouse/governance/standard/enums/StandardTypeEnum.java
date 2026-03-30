package com.bagdatahouse.governance.standard.enums;

/**
 * 数据标准类型枚举
 */
public enum StandardTypeEnum {

    CODE_STANDARD("CODE_STANDARD", "编码标准"),
    NAMING_STANDARD("NAMING_STANDARD", "命名规范"),
    PRIMARY_DATA("PRIMARY_DATA", "主数据");

    private final String code;
    private final String label;

    StandardTypeEnum(String code, String label) {
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
        for (StandardTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getLabel();
            }
        }
        return code;
    }
}
