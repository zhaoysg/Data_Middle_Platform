package com.bagdatahouse.common.constant;

/**
 * 全局常量定义
 */
public class GlobalConstants {

    private GlobalConstants() {}

    // ========== 通用状态 ==========
    public static final String STATUS_NORMAL = "1";
    public static final String STATUS_DISABLE = "0";

    // ========== 是否标记 ==========
    public static final int YES = 1;
    public static final int NO = 0;

    // ========== 分页默认值 ==========
    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    // ========== 认证相关 ==========
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_ISSUER = "bagdatahouse";

    // ========== 缓存Key前缀 ==========
    public static final String CACHE_PREFIX_USER = "user:";
    public static final String CACHE_PREFIX_PERMISSION = "permission:";
    public static final String CACHE_PREFIX_CAPTCHA = "captcha:";
    public static final String CACHE_PREFIX_DQC_RESULT = "dqc:result:";
    public static final String CACHE_PREFIX_METADATA = "metadata:";

    // =========* 数据源类型 ==========
    public static final String DS_TYPE_MYSQL = "MYSQL";
    public static final String DS_TYPE_SQLSERVER = "SQLSERVER";
    public static final String DS_TYPE_ORACLE = "ORACLE";
    public static final String DS_TYPE_TIDB = "TIDB";
    public static final String DS_TYPE_POSTGRESQL = "POSTGRESQL";

    // ========== 数仓分层 ==========
    public static final String LAYER_ODS = "ODS";
    public static final String LAYER_DWD = "DWD";
    public static final String LAYER_DWS = "DWS";
    public static final String LAYER_ADS = "ADS";

    // ========== 规则类型 ==========
    public static final String RULE_TYPE_NULL_CHECK = "NULL_CHECK";
    public static final String RULE_TYPE_UNIQUE = "UNIQUE";
    public static final String RULE_TYPE_REGEX = "REGEX";
    public static final String RULE_TYPE_THRESHOLD = "THRESHOLD";
    public static final String RULE_TYPE_SQL = "SQL";
    public static final String RULE_TYPE_FLUCTUATION = "FLUCTUATION";
    public static final String RULE_TYPE_CUSTOM_FUNC = "CUSTOM_FUNC";

    // ========== 规则强度 ==========
    public static final String RULE_STRENGTH_STRONG = "STRONG";
    public static final String RULE_STRENGTH_WEAK = "WEAK";

    // ========== 错误级别 ==========
    public static final String ERROR_LEVEL_LOW = "LOW";
    public static final String ERROR_LEVEL_MEDIUM = "MEDIUM";
    public static final String ERROR_LEVEL_HIGH = "HIGH";
    public static final String ERROR_LEVEL_CRITICAL = "CRITICAL";

    // ========== 质量维度 ==========
    public static final String DIMENSION_COMPLETENESS = "COMPLETENESS";
    public static final String DIMENSION_UNIQUENESS = "UNIQUENESS";
    public static final String DIMENSION_ACCURACY = "ACCURACY";
    public static final String DIMENSION_CONSISTENCY = "CONSISTENCY";
    public static final String DIMENSION_TIMELINESS = "TIMELINESS";
    public static final String DIMENSION_VALIDITY = "VALIDITY";

    // ========== 告警级别 ==========
    public static final String ALERT_LEVEL_INFO = "INFO";
    public static final String ALERT_LEVEL_WARN = "WARN";
    public static final String ALERT_LEVEL_ERROR = "ERROR";
    public static final String ALERT_LEVEL_CRITICAL = "CRITICAL";

    // ========== 敏感等级 ==========
    public static final String SENSITIVITY_NORMAL = "NORMAL";
    public static final String SENSITIVITY_INNER = "INNER";
    public static final String SENSITIVITY_SENSITIVE = "SENSITIVE";
    public static final String SENSITIVITY_HIGHLY_SENSITIVE = "HIGHLY_SENSITIVE";

    // ========== 血缘类型 ==========
    public static final String LINEAGE_TYPE_TABLE = "TABLE";
    public static final String LINEAGE_TYPE_COLUMN = "COLUMN";

    // ========== 血缘来源 ==========
    public static final String LINEAGE_SOURCE_MANUAL = "MANUAL";
    public static final String LINEAGE_SOURCE_AUTO_PARSER = "AUTO_PARSER";

    // ========== 脱敏类型 ==========
    public static final String MASK_TYPE_NONE = "NONE";
    public static final String MASK_TYPE_MASK = "MASK";
    public static final String MASK_TYPE_HIDE = "HIDE";
    public static final String MASK_TYPE_ENCRYPT = "ENCRYPT";
    public static final String MASK_TYPE_DELETE = "DELETE";

    // ========== 系统角色代码 ==========
    /** 数据安全管理员（强制审批人，用于 L4 机密字段） */
    public static final String ROLE_CODE_DAYU_ADMIN = "DAYU_ADMIN";
    /** 超级管理员 */
    public static final String ROLE_CODE_SUPER_ADMIN = "SUPER_ADMIN";
}
