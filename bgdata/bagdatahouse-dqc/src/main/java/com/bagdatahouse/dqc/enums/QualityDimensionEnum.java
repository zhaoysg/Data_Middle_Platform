package com.bagdatahouse.dqc.enums;

/**
 * 质量维度枚举（对齐华为六维模型）
 */
public enum QualityDimensionEnum {

    COMPLETENESS("COMPLETENESS", "完整性", "数据完整程度，包括空值率、行数等"),
    UNIQUENESS("UNIQUENESS", "唯一性", "数据唯一程度，包括主键重复、唯一约束等"),
    ACCURACY("ACCURACY", "准确性", "数据准确程度，包括数值范围、格式校验等"),
    CONSISTENCY("CONSISTENCY", "一致性", "数据一致程度，包括跨表一致性、时间戳一致性等"),
    TIMELINESS("TIMELINESS", "及时性", "数据更新及时程度，包括增量时间、数据延迟等"),
    VALIDITY("VALIDITY", "有效性", "数据有效程度，包括格式校验、正则匹配等");

    private final String code;
    private final String name;
    private final String description;

    QualityDimensionEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static QualityDimensionEnum fromCode(String code) {
        for (QualityDimensionEnum dim : values()) {
            if (dim.code.equalsIgnoreCase(code)) {
                return dim;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        QualityDimensionEnum dim = fromCode(code);
        return dim != null ? dim.name : code;
    }
}
