package com.bagdatahouse.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期工具类
 */
public class DateTimeUtil {

    private DateTimeUtil() {}

    public static final String PATTERN_DATE = "yyyy-MM-dd";
    public static final String PATTERN_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_COMPACT = "yyyyMMddHHmmss";
    public static final String PATTERN_DATE_COMPACT = "yyyyMMdd";

    public static String format(Date date) {
        return date != null ? cn.hutool.core.date.DateUtil.format(date, PATTERN_TIME) : null;
    }

    public static String format(Date date, String pattern) {
        return date != null ? cn.hutool.core.date.DateUtil.format(date, pattern) : null;
    }

    public static Date parse(String dateStr) {
        return cn.hutool.core.date.DateUtil.parse(dateStr);
    }

    public static Date parse(String dateStr, String pattern) {
        return cn.hutool.core.date.DateUtil.parse(dateStr, pattern);
    }

    public static Date now() {
        return new Date();
    }

    public static String today() {
        return format(new Date(), PATTERN_DATE);
    }

    public static String nowStr() {
        return format(new Date(), PATTERN_TIME);
    }

    public static Date addDays(Date date, int days) {
        return cn.hutool.core.date.DateUtil.offsetDay(date, days);
    }

    public static Date beginOfDay(Date date) {
        return cn.hutool.core.date.DateUtil.beginOfDay(date);
    }

    public static Date endOfDay(Date date) {
        return cn.hutool.core.date.DateUtil.endOfDay(date);
    }

    public static long diffDays(Date startDate, Date endDate) {
        return cn.hutool.core.date.DateUtil.betweenDay(startDate, endDate, true);
    }

    public static long diffMillis(Date startDate, Date endDate) {
        return endDate.getTime() - startDate.getTime();
    }

    public static boolean isToday(Date date) {
        return cn.hutool.core.date.DateUtil.isSameDay(date, new Date());
    }

    public static Date yesterday() {
        return addDays(new Date(), -1);
    }

    public static Date tomorrow() {
        return addDays(new Date(), 1);
    }
}
