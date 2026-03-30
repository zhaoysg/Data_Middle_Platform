package com.bagdatahouse.governance.standard.enums;

/**
 * 合规状态枚举
 */
public enum ComplianceStatusEnum {

    PENDING("PENDING", "待检测"),
    COMPLIANT("COMPLIANT", "合规"),
    NON_COMPLIANT("NON_COMPLIANT", "不合规");

    private final String code;
    private final String label;

    ComplianceStatusEnum(String code, String label) {
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
        for (ComplianceStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getLabel();
            }
        }
        return code;
    }
}
