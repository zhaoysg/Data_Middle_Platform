package com.bagdatahouse.governance.lineage.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum LineageSourceEnum implements IEnum<Integer> {
    MANUAL(1, "MANUAL", "手动录入"),
    AUTO_PARSER(2, "AUTO_PARSER", "自动解析");

    private final Integer code;
    private final String value;
    private final String label;

    LineageSourceEnum(Integer code, String value, String label) {
        this.code = code;
        this.value = value;
        this.label = label;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }
}
