package com.bagdatahouse.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SQL性能监控工具类
 * 用于监控SQL执行次数和耗时统计
 */
@Slf4j
public class SqlPerformanceMonitor {

    /**
     * SQL统计信息
     */
    private static final ConcurrentHashMap<String, SqlStat> SQL_STATS = new ConcurrentHashMap<>();

    /**
     * 慢SQL阈值（毫秒）
     */
    private static long SLOW_SQL_THRESHOLD = 3000;

    /**
     * 最大记录数
     */
    private static final int MAX_RECORDS = 1000;

    /**
     * SQL统计实体
     */
    private static class SqlStat {
        String sql;
        AtomicLong executeCount = new AtomicLong(0);
        AtomicLong totalTime = new AtomicLong(0);
        AtomicLong maxTime = new AtomicLong(0);
        AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong errorCount = new AtomicLong(0);
        long lastExecuteTime;

        SqlStat(String sql) {
            this.sql = sql;
        }

        void record(long time, boolean success) {
            executeCount.incrementAndGet();
            totalTime.addAndGet(time);
            lastExecuteTime = System.currentTimeMillis();

            // 更新最大最小值
            maxTime.updateAndGet(v -> Math.max(v, time));
            minTime.updateAndGet(v -> Math.min(v, time));

            if (!success) {
                errorCount.incrementAndGet();
            }
        }

        double getAvgTime() {
            long count = executeCount.get();
            return count > 0 ? (double) totalTime.get() / count : 0;
        }
    }

    /**
     * 记录SQL执行信息
     *
     * @param sql    SQL语句
     * @param time   执行时间（毫秒）
     * @param success 是否成功
     */
    public static void record(String sql, long time, boolean success) {
        // 简化SQL（去除参数）
        String simpleSql = simplifySql(sql);
        
        // 检查慢SQL
        if (time > SLOW_SQL_THRESHOLD) {
            log.warn("检测到慢SQL: {} ms, SQL: {}", time, simpleSql);
        }

        SQL_STATS.computeIfAbsent(simpleSql, SqlStat::new).record(time, success);
    }

    /**
     * 简化SQL语句
     */
    private static String simplifySql(String sql) {
        if (sql == null) {
            return "";
        }
        // 去除多余空白字符
        return sql.trim().replaceAll("\\s+", " ");
    }

    /**
     * 获取SQL统计信息
     */
    public static List<SqlStatInfo> getStats() {
        List<SqlStatInfo> result = new ArrayList<>();
        for (SqlStat stat : SQL_STATS.values()) {
            SqlStatInfo info = new SqlStatInfo();
            info.sql = stat.sql;
            info.executeCount = stat.executeCount.get();
            info.totalTime = stat.totalTime.get();
            info.maxTime = stat.maxTime.get();
            info.minTime = stat.minTime.get() == Long.MAX_VALUE ? 0 : stat.minTime.get();
            info.avgTime = stat.getAvgTime();
            info.errorCount = stat.errorCount.get();
            info.lastExecuteTime = stat.lastExecuteTime;
            result.add(info);
        }
        return result;
    }

    /**
     * 获取Top N慢SQL
     */
    public static List<SqlStatInfo> getTopSlowSql(int topN) {
        List<SqlStatInfo> allStats = getStats();
        allStats.sort((a, b) -> Long.compare(b.maxTime, a.maxTime));
        return allStats.subList(0, Math.min(topN, allStats.size()));
    }

    /**
     * 获取Top N高频SQL
     */
    public static List<SqlStatInfo> getTopFrequentSql(int topN) {
        List<SqlStatInfo> allStats = getStats();
        allStats.sort((a, b) -> Long.compare(b.executeCount, a.executeCount));
        return allStats.subList(0, Math.min(topN, allStats.size()));
    }

    /**
     * 清空统计数据
     */
    public static void clear() {
        SQL_STATS.clear();
    }

    /**
     * 设置慢SQL阈值
     */
    public static void setSlowSqlThreshold(long threshold) {
        SLOW_SQL_THRESHOLD = threshold;
    }

    /**
     * SQL统计信息
     */
    public static class SqlStatInfo {
        public String sql;
        public long executeCount;
        public long totalTime;
        public long maxTime;
        public long minTime;
        public double avgTime;
        public long errorCount;
        public long lastExecuteTime;

        @Override
        public String toString() {
            return String.format(
                "SQL: %s\n  执行次数: %d, 总耗时: %d ms, 最大: %d ms, 最小: %d ms, 平均: %.2f ms, 错误: %d",
                sql, executeCount, totalTime, maxTime, minTime, avgTime, errorCount
            );
        }
    }
}
