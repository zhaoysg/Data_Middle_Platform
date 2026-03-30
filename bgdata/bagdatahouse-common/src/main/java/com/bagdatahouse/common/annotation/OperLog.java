package com.bagdatahouse.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {

    /**
     * 操作模块
     */
    String value() default "";

    /**
     * 操作类型
     */
    OperType type() default OperType.OTHER;

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 操作类型枚举
     */
    enum OperType {
        OTHER,
        INSERT,
        UPDATE,
        DELETE,
        SELECT,
        GRANT,
        EXPORT,
        IMPORT,
        CLEAN
    }
}
