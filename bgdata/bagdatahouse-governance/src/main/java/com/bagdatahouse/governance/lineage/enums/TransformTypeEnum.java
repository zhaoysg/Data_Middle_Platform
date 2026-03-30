package com.bagdatahouse.governance.lineage.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum TransformTypeEnum implements IEnum<Integer> {
    DIRECT(1, "DIRECT", "直接传递"),
    SUM(2, "SUM", "求和聚合"),
    AVG(3, "AVG", "求平均聚合"),
    COUNT(4, "COUNT", "计数聚合"),
    MAX(5, "MAX", "取最大值"),
    MIN(6, "MIN", "取最小值"),
    CONCAT(7, "CONCAT", "字符串拼接"),
    CASE_WHEN(8, "CASE_WHEN", "条件转换"),
    CUSTOM_EXPR(9, "CUSTOM_EXPR", "自定义表达式");

    private final Integer code;
    private final String value;
    private final String label;

    TransformTypeEnum(Integer code, String value, String label) {
        this.code = code;
        this.value = value;
        this.label = label;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }
}
