package com.bagdatahouse.governance.glossary.enums;

/**
 * 映射审批状态枚举
 */
public enum MappingStatusEnum {

    PENDING("PENDING", "待审批"),
    APPROVED("APPROVED", "已审批"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String label;

    MappingStatusEnum(String code, String label) {
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
        for (MappingStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getLabel();
            }
        }
        return code;
    }
}
