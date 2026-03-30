package com.bagdatahouse.governance.glossary.enums;

/**
 * 映射类型枚举
 */
public enum MappingTypeEnum {

    DIRECT("DIRECT", "直接映射"),
    TRANSFORM("TRANSFORM", "转换映射"),
    AGGREGATE("AGGREGATE", "聚合映射");

    private final String code;
    private final String label;

    MappingTypeEnum(String code, String label) {
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
        if (code == null) return null;
        for (MappingTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getLabel();
            }
        }
        return code;
    }
}
