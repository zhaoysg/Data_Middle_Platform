package com.bagdatahouse.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 数据权限类型
     * ALL - 全部数据权限
     * DEPT - 本部门数据权限
     * DEPT_AND_CHILD - 本部门及以下数据权限
     * SELF - 仅本人数据权限
     * CUSTOM - 自定义数据权限
     */
    DataScopeType type() default DataScopeType.ALL;

    /**
     * 自定义部门SQL
     */
    String deptSql() default "dept_id";

    enum DataScopeType {
        ALL,
        DEPT,
        DEPT_AND_CHILD,
        SELF,
        CUSTOM
    }
}
