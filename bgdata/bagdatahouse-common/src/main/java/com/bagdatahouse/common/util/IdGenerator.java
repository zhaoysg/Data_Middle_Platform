package com.bagdatahouse.common.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * ID生成工具类
 */
public class IdGenerator {

    private IdGenerator() {}

    /**
     * 获取雪花ID
     */
    public static long snowflakeId() {
        return cn.hutool.core.util.IdUtil.getSnowflakeNextId();
    }

    /**
     * 获取简单UUID（无中划线）
     */
    public static String simpleUUID() {
        return cn.hutool.core.util.IdUtil.simpleUUID();
    }

    /**
     * 获取带连字符的UUID
     */
    public static String uuid() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * 生成指定位数的随机数字
     */
    public static String randomNumeric(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成执行编号
     */
    public static String generateExecutionNo() {
        return "EXEC" + cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMddHHmmss") 
               + randomNumeric(6);
    }

    /**
     * 生成告警编号
     */
    public static String generateAlertNo() {
        return "ALT" + cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMddHHmmss") 
               + randomNumeric(6);
    }

    /**
     * 生成规则编码
     */
    public static String generateRuleCode() {
        return "RULE" + cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMdd") 
               + randomNumeric(4);
    }

    /**
     * 生成方案编码
     */
    public static String generatePlanCode() {
        return "PLAN" + cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMdd") 
               + randomNumeric(4);
    }

    /**
     * 生成数据源编码
     */
    public static String generateDsCode() {
        return "DS" + cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyyMMdd") 
               + randomNumeric(4);
    }
}
