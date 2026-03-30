package com.bagdatahouse.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置（性能优化）
 * 用于后台任务、报表生成、数据导出等场景
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncTaskConfig {

    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE = 10;
    
    /**
     * 最大线程数
     */
    private static final int MAX_POOL_SIZE = 50;
    
    /**
     * 队列容量
     */
    private static final int QUEUE_CAPACITY = 200;
    
    /**
     * 线程存活时间（秒）
     */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 通用异步任务执行器
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(CORE_POOL_SIZE);
        // 最大线程数
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        // 队列容量
        executor.setQueueCapacity(QUEUE_CAPACITY);
        // 线程名前缀
        executor.setThreadNamePrefix("bagdatahouse-async-");
        // 线程存活时间
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        
        // 拒绝策略：CallerRunsPolicy - 由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        
        // 初始化
        executor.initialize();
        
        log.info("通用异步任务执行器初始化完成: core={}, max={}, queue={}", 
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        
        return executor;
    }

    /**
     * 数据质量检查任务执行器（专用）
     */
    @Bean("qualityCheckExecutor")
    public Executor qualityCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("quality-check-");
        executor.setKeepAliveSeconds(120);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        
        executor.initialize();
        
        log.info("数据质量检查任务执行器初始化完成");
        
        return executor;
    }

    /**
     * 数据导出任务执行器（专用）
     */
    @Bean("exportExecutor")
    public Executor exportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("data-export-");
        executor.setKeepAliveSeconds(180);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300);
        
        executor.initialize();
        
        log.info("数据导出任务执行器初始化完成");
        
        return executor;
    }
}
