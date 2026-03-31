package org.dromara.metadata.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 扫描调度类型枚举
 *
 * @author shaozhengchao
 */
@Getter
public enum ScanScheduleTypeEnum {

    MANUAL("MANUAL", "手动触发"),
    CRON("CRON", "定时调度"),
    EVENT("EVENT", "事件触发");

    @EnumValue
    private final String code;

    private final String desc;

    ScanScheduleTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
