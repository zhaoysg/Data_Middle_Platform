package com.bagdatahouse.common.annotation;

import java.lang.annotation.*;

/**
 * 不记录操作日志的注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoOperLog {
}
