package com.bagdatahouse.common.annotation;

import java.lang.annotation.*;

/**
 * 响应数据脱敏注解
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sensitive {

    /**
     * 脱敏类型
     */
    SensitiveType type() default SensitiveType.DEFAULT;

    /**
     * 前缀保留长度
     */
    int prefixKeep() default 3;

    /**
     * 后缀保留长度
     */
    int suffixKeep() default 4;

    /**
     * 脱敏字符
     */
    String maskChar() default "*";

    enum SensitiveType {
        DEFAULT,
        PHONE,
        EMAIL,
        ID_CARD,
        BANK_CARD,
        NAME,
        ADDRESS,
        CUSTOM
    }
}
