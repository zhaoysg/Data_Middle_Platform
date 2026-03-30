package com.bagdatahouse.dqc.enums;

/**
 * 质检方案状态枚举
 */
public enum PlanStatusEnum {

    DRAFT("DRAFT", "草稿", "草稿状态，可编辑但不能执行"),
    PUBLISHED("PUBLISHED", "已发布", "已发布状态，可以执行"),
    DISABLED("DISABLED", "已停用", "已停用状态，不能执行");

    private final String code;
    private final String name;
    private final String description;

    PlanStatusEnum(String code, String name, String description) {
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

    public static PlanStatusEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PlanStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getNameByCode(String code) {
        PlanStatusEnum status = fromCode(code);
        return status != null ? status.name : code;
    }

    public static boolean isExecutable(String code) {
        return PUBLISHED.code.equals(code);
    }

    public static boolean isDraft(String code) {
        return code == null || DRAFT.code.equals(code);
    }
}
