package com.bagdatahouse.common.enums;

/**
 * 响应码枚举
 */
public enum ResponseCode {

    // ========== 成功 ==========
    SUCCESS(200, "操作成功"),

    // ========== 参数错误 4xx ==========
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请登录"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // ========== 服务器错误 5xx ==========
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),

    // ========== 业务错误 1xxx ==========
    // 系统错误
    SYSTEM_ERROR(1001, "系统错误"),
    SYSTEM_BUSY(1002, "系统繁忙，请稍后再试"),

    // 认证错误
    TOKEN_EXPIRED(1101, "Token已过期"),
    TOKEN_INVALID(1102, "Token无效"),
    TOKEN_NOT_FOUND(1103, "Token不存在"),
    PASSWORD_ERROR(1104, "密码错误"),
    USER_DISABLED(1105, "用户已禁用"),
    USER_LOCKED(1106, "用户已锁定"),

    // 数据源错误
    DATASOURCE_NOT_FOUND(2001, "数据源不存在"),
    DATASOURCE_CONNECTION_FAILED(2002, "数据源连接失败"),
    DATASOURCE_ALREADY_EXISTS(2003, "数据源已存在"),

    // 规则错误
    RULE_NOT_FOUND(3001, "规则不存在"),
    RULE_EXPRESSION_INVALID(3002, "规则表达式无效"),
    RULE_TEMPLATE_NOT_FOUND(3003, "规则模板不存在"),

    // 方案错误
    PLAN_NOT_FOUND(4001, "质检方案不存在"),
    PLAN_NO_RULES(4002, "方案未绑定任何规则"),
    PLAN_EXECUTION_FAILED(4003, "方案执行失败"),

    // 元数据错误
    METADATA_NOT_FOUND(5001, "元数据不存在"),
    METADATA_SCAN_FAILED(5002, "元数据扫描失败"),

    // 血缘错误
    LINEAGE_NOT_FOUND(6001, "血缘关系不存在"),
    LINEAGE_CIRCULAR_DEPENDENCY(6002, "血缘关系存在循环依赖"),

    // 探查错误
    PROFILE_TASK_NOT_FOUND(7001, "探查任务不存在"),
    PROFILE_EXECUTION_FAILED(7002, "探查执行失败"),

    // 告警错误
    ALERT_RULE_NOT_FOUND(8001, "告警规则不存在"),
    ALERT_SEND_FAILED(8002, "告警发送失败"),

    // 文件错误
    FILE_UPLOAD_FAILED(9001, "文件上传失败"),
    FILE_DOWNLOAD_FAILED(9002, "文件下载失败"),
    FILE_TYPE_NOT_SUPPORTED(9003, "文件类型不支持");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
