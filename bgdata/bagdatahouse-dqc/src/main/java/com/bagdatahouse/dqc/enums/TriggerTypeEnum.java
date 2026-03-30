package com.bagdatahouse.dqc.enums;

/**
 * 质检方案触发类型枚举
 */
public enum TriggerTypeEnum {

    MANUAL("MANUAL", "手动触发", "用户手动执行"),
    SCHEDULE("SCHEDULE", "定时触发", "按 Cron 表达式定时执行"),
    API("API", "API触发", "通过接口调用触发"),
    EVENT("EVENT", "事件触发", "由特定业务事件触发");

    private final String code;
    private final String name;
    private final String description;

    TriggerTypeEnum(String code, String name, String description) {
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

    public static TriggerTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TriggerTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        TriggerTypeEnum type = fromCode(code);
        return type != null ? type.name : code;
    }
}
