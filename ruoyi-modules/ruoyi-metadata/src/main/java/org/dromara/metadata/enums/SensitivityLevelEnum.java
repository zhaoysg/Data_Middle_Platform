package org.dromara.metadata.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 敏感等级枚举
 */
@Getter
public enum SensitivityLevelEnum {

    NORMAL("NORMAL", "普通"),
    INNER("INNER", "内部"),
    SENSITIVE("SENSITIVE", "敏感"),
    HIGHLY_SENSITIVE("HIGHLY_SENSITIVE", "高度敏感");

    @EnumValue
    private final String code;
    private final String desc;

    SensitivityLevelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static SensitivityLevelEnum fromCode(String code) {
        if (code == null) return null;
        for (SensitivityLevelEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
