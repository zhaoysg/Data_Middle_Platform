package com.bagdatahouse.server.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 性能优化配置
 * 针对 SQL 执行效率和分页查询的优化
 */
@Slf4j
@Configuration
public class MybatisPlusPerformanceConfig {

    /**
     * MyBatis-Plus 插件配置
     * 包含分页、乐观锁、防止全表更新与删除
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件 - 性能优化核心
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 最大单页限制数量，默认 500 条，-1 不受限制
        paginationInterceptor.setMaxLimit(500L);
        // 设置最大返回数据条数，防止一次查询过多数据
        paginationInterceptor.setOverflow(false);
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        // 乐观锁插件 - 并发更新控制
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        // 防止全表更新与删除 - 安全防护
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        
        log.info("MyBatis-Plus 性能优化配置加载完成");
        return interceptor;
    }
}
