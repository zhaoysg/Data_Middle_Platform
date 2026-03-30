package com.bagdatahouse.common.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermissions {

    /**
     * 权限标识
     */
    String[] value() default {};

    /**
     * 权限逻辑：AND/OR
     */
    Logical logical() default Logical.AND;

    enum Logical {
        AND,
        OR
    }
}
