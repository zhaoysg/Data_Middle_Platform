package com.bagdatahouse.governance.security.enums;

/**
 * 脱敏方式枚举（对齐阿里 DataWorks 数据保护伞 + 华为 DataArts Studio）
 * <p>
 * 支持 10 种脱敏类型：
 * MASK/HIDE/ENCRYPT/HASH/RANDOM_REPLACE/RANGE/WATERMARK/FORMAT_KEEP/DELETE/NONE
 */
public enum MaskTypeEnum {

    NONE("NONE", "不脱敏"),
    MASK("MASK", "遮蔽"),
    HIDE("HIDE", "隐藏"),
    ENCRYPT("ENCRYPT", "加密"),
    DELETE("DELETE", "删除"),
    HASH("HASH", "哈希脱敏"),
    RANDOM_REPLACE("RANDOM_REPLACE", "随机替换"),
    RANGE("RANGE", "区间化"),
    WATERMARK("WATERMARK", "水印"),
    FORMAT_KEEP("FORMAT_KEEP", "保留格式");

    private final String code;
    private final String label;

    MaskTypeEnum(String code, String label) {
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
        for (MaskTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e.getLabel();
            }
        }
        return code;
    }
}
