package com.bagdatahouse.governance.asset.enums;

import lombok.Getter;

/**
 * 资产目录类型枚举
 */
@Getter
public enum CatalogTypeEnum {

    BUSINESS_DOMAIN("BUSINESS_DOMAIN", "业务域"),
    DATA_DOMAIN("DATA_DOMAIN", "数据域"),
    ALBUM("ALBUM", "数据专辑");

    private final String code;
    private final String label;

    CatalogTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String getLabel(String code) {
        for (CatalogTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e.label;
            }
        }
        return code;
    }
}
