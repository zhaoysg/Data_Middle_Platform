package com.bagdatahouse.common.util;

import cn.hutool.core.util.StrUtil;

/**
 * 字符串工具类
 */
public class StrUtils {

    private StrUtils() {}

    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 判断字符串是否为空白
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 空字符串返回默认值
     */
    public static String defaultIfEmpty(String str, String defaultStr) {
        return isEmpty(str) ? defaultStr : str;
    }

    /**
     * 空白字符串返回默认值
     */
    public static String defaultIfBlank(String str, String defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }

    /**
     * 字符串脱敏（手机号）
     * 13812345678 -> 138****5678
     */
    public static String maskPhone(String phone) {
        if (isEmpty(phone) || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 字符串脱敏（身份证号）
     * 110101199001011234 -> 110101**********1234
     */
    public static String maskIdCard(String idCard) {
        if (isEmpty(idCard) || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "**********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 字符串脱敏（邮箱）
     * test@example.com -> t***@example.com
     */
    public static String maskEmail(String email) {
        if (isEmpty(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 1) {
            return email;
        }
        return parts[0].charAt(0) + "***@" + parts[1];
    }

    /**
     * 字符串脱敏（银行卡）
     * 6222021234567890123 -> **** **** **** 0123
     */
    public static String maskBankCard(String card) {
        if (isEmpty(card) || card.length() < 8) {
            return card;
        }
        return "**** **** **** " + card.substring(card.length() - 4);
    }

    /**
     * 驼峰转下划线
     */
    public static String camelToUnderscore(String str) {
        return str.replaceAll("([A-Z])", "_$1").toLowerCase().replaceAll("^_", "");
    }

    /**
     * 下划线转驼峰
     */
    public static String underscoreToCamel(String str) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : str.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    /**
     * 首字母大写
     */
    public static String capitalize(String str) {
        return cn.hutool.core.util.StrUtil.upperFirst(str);
    }

    /**
     * 首字母小写
     */
    public static String uncapitalize(String str) {
        return cn.hutool.core.util.StrUtil.lowerFirst(str);
    }

    /**
     * 截取字符串（超过最大长度用...代替）
     */
    public static String truncate(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * 格式化文件大小
     */
    public static String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / 1024.0 / 1024.0);
        } else {
            return String.format("%.2f GB", size / 1024.0 / 1024.0 / 1024.0);
        }
    }

    /**
     * 格式化时长（毫秒转为可读格式）
     */
    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        } else if (millis < 60 * 1000) {
            return String.format("%.2f s", millis / 1000.0);
        } else if (millis < 60 * 60 * 1000) {
            return String.format("%.2f min", millis / 1000.0 / 60.0);
        } else {
            return String.format("%.2f h", millis / 1000.0 / 60.0 / 60.0);
        }
    }
}
