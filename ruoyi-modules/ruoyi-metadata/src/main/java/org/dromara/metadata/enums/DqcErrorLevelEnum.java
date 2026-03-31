package org.dromara.metadata.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 数据质量错误级别枚举
 *
 * @author shaozhengchao
 */
@Getter
public enum DqcErrorLevelEnum {

    LOW("LOW", "低"),
    MEDIUM("MEDIUM", "中"),
    HIGH("HIGH", "高"),
    CRITICAL("CRITICAL", "严重");

    @EnumValue
    private final String code;

    private final String desc;

    DqcErrorLevelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
