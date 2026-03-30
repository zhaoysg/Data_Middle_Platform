package com.bagdatahouse.dqc.enums;

/**
 * 规则类型枚举
 */
public enum RuleTypeEnum {

    // 空值检查类
    NULL_CHECK("NULL_CHECK", "空值检查", "检测字段的空值/非空状态"),
    ROW_COUNT_NOT_ZERO("ROW_COUNT_NOT_ZERO", "行数非0检查", "检测表是否有数据"),
    ROW_COUNT_FLUCTUATION("ROW_COUNT_FLUCTUATION", "行数波动检查", "检测表行数较历史波动"),
    TABLE_UPDATE_TIMELINESS("TABLE_UPDATE_TIMELINESS", "表更新时效", "根据 MAX(时间列) 等结果判断数据是否在阈值内更新"),

    // 唯一性检查类
    UNIQUE_CHECK("UNIQUE_CHECK", "唯一性检查", "检测字段值是否唯一"),
    DUPLICATE_CHECK("DUPLICATE_CHECK", "重复值检查", "检测字段重复值数量"),
    CARDINALITY("CARDINALITY", "基数/离散度分析", "检测字段唯一值比例"),

    // 阈值检查类
    THRESHOLD_MIN("THRESHOLD_MIN", "最小值检查", "检测字段最小值是否满足阈值"),
    THRESHOLD_MAX("THRESHOLD_MAX", "最大值检查", "检测字段最大值是否满足阈值"),
    THRESHOLD_RANGE("THRESHOLD_RANGE", "值域范围检查", "检测字段值是否在指定范围内"),
    COUNT_THRESHOLD("COUNT_THRESHOLD", "计数阈值检查", "检测COUNT结果是否满足阈值"),

    // 格式校验类
    REGEX_PHONE("REGEX_PHONE", "手机号格式检查", "检测是否符合手机号格式"),
    REGEX_EMAIL("REGEX_EMAIL", "邮箱格式检查", "检测是否符合邮箱格式"),
    REGEX_IDCARD("REGEX_IDCARD", "身份证格式检查", "检测是否符合身份证格式"),
    REGEX("REGEX", "自定义正则检查", "使用自定义正则表达式校验"),
    LENGTH_CHECK("LENGTH_CHECK", "字符串长度检查", "检测字段长度是否满足要求"),

    // 跨字段/跨表检查类
    CROSS_FIELD_COMPARE("CROSS_FIELD_COMPARE", "跨字段比较", "比较两个字段的关系是否成立"),
    CROSS_FIELD_SUM("CROSS_FIELD_SUM", "跨字段求和验证", "验证字段A+字段B与字段C的关系"),
    CROSS_FIELD_NULL_CHECK("CROSS_FIELD_NULL_CHECK", "跨字段空值一致性", "检测字段间空值一致性"),
    CROSS_TABLE_COUNT("CROSS_TABLE_COUNT", "跨表行数一致性", "比较两张表的行数是否一致"),
    CROSS_TABLE_PRIMARY_KEY("CROSS_TABLE_PRIMARY_KEY", "跨表主键一致性", "比较两张表的主键覆盖度"),

    // 自定义类
    CUSTOM_SQL("CUSTOM_SQL", "自定义SQL检查", "执行用户编写的SQL进行质量检查"),
    CUSTOM_FUNC("CUSTOM_FUNC", "自定义函数检查", "执行用户注册的自定义Java函数");

    private final String code;
    private final String name;
    private final String description;

    RuleTypeEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public static RuleTypeEnum fromCode(String code) {
        for (RuleTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        RuleTypeEnum type = fromCode(code);
        return type != null ? type.name : code;
    }
}
