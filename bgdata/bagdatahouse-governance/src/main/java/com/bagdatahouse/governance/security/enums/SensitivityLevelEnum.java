package com.bagdatahouse.governance.security.enums;

/**
 * 数据分级枚举（参考《个人信息保护法》四级分级）
 */
public enum SensitivityLevelEnum {

    L1("L1", "公开", 1, "#52c41a"),
    L2("L2", "内部", 2, "#1890ff"),
    L3("L3", "敏感", 3, "#FAAD14"),
    L4("L4", "机密", 4, "#FF4D4F");

    private final String code;
    private final String label;
    private final Integer levelValue;
    private final String color;

    SensitivityLevelEnum(String code, String label, Integer levelValue, String color) {
        this.code = code;
        this.label = label;
        this.levelValue = levelValue;
        this.color = color;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public Integer getLevelValue() {
        return levelValue;
    }

    public String getColor() {
        return color;
    }

    public Integer getValue() {
        return levelValue;
    }

    public static String getLabelByCode(String code) {
        for (SensitivityLevelEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getLabel();
            }
        }
        return code;
    }

    public static String getColorByCode(String code) {
        for (SensitivityLevelEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getColor();
            }
        }
        return "#8C8C8C";
    }
}
