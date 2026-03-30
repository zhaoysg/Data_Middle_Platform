package com.bagdatahouse.governance.lineage.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum LineageTypeEnum implements IEnum<Integer> {
    TABLE(1, "TABLE", "表级血缘"),
    COLUMN(2, "COLUMN", "字段级血缘");

    private final Integer code;
    private final String value;
    private final String label;

    LineageTypeEnum(Integer code, String value, String label) {
        this.code = code;
        this.value = value;
        this.label = label;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }
}
