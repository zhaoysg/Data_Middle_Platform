package org.dromara.metadata.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 资产目录类型枚举
 */
@Getter
public enum CatalogTypeEnum {

    BUSINESS_DOMAIN("BUSINESS_DOMAIN", "业务域"),
    DATA_DOMAIN("DATA_DOMAIN", "数据域"),
    ALBUM("ALBUM", "专辑");

    @EnumValue
    private final String code;
    private final String desc;

    CatalogTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CatalogTypeEnum fromCode(String code) {
        if (code == null) return null;
        for (CatalogTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
