package org.dromara.metadata.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 表类型枚举
 */
@Getter
public enum TableTypeEnum {

    TABLE("TABLE", "表"),
    VIEW("VIEW", "视图");

    @EnumValue
    private final String code;
    private final String desc;

    TableTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TableTypeEnum fromCode(String code) {
        if (code == null) return null;
        for (TableTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
