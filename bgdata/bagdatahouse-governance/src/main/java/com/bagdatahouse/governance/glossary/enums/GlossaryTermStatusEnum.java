package com.bagdatahouse.governance.glossary.enums;

/**
 * 术语状态枚举
 */
public enum GlossaryTermStatusEnum {

    DRAFT("DRAFT", "草稿"),
    PUBLISHED("PUBLISHED", "已发布"),
    DEPRECATED("DEPRECATED", "已废弃");

    private final String code;
    private final String label;

    GlossaryTermStatusEnum(String code, String label) {
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
        for (GlossaryTermStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getLabel();
            }
        }
        return code;
    }
}
