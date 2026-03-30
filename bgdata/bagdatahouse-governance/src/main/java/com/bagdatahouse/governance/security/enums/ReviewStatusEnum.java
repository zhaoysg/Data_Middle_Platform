package com.bagdatahouse.governance.security.enums;

/**
 * 审核状态枚举
 */
public enum ReviewStatusEnum {

    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已审核"),
    REJECTED("REJECTED", "已驳回");

    private final String code;
    private final String label;

    ReviewStatusEnum(String code, String label) {
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
        for (ReviewStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getLabel();
            }
        }
        return code;
    }
}
