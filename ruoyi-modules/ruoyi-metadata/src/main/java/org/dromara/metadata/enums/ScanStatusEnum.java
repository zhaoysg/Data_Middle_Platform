package org.dromara.metadata.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 扫描状态枚举
 */
@Getter
public enum ScanStatusEnum {

    RUNNING("RUNNING", "扫描中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    PARTIAL("PARTIAL", "部分成功"),
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    private final String code;
    private final String desc;

    ScanStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ScanStatusEnum fromCode(String code) {
        if (code == null) return null;
        for (ScanStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
