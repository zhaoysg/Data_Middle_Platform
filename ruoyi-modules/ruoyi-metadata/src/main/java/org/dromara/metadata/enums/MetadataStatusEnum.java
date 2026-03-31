package org.dromara.metadata.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 元数据状态枚举
 */
@Getter
public enum MetadataStatusEnum {

    ACTIVE("ACTIVE", "活跃"),
    ARCHIVED("ARCHIVED", "归档"),
    DEPRECATED("DEPRECATED", "废弃");

    @EnumValue
    private final String code;
    private final String desc;

    MetadataStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MetadataStatusEnum fromCode(String code) {
        if (code == null) return null;
        for (MetadataStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
