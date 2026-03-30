package com.bagdatahouse.server.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Druid 连接池性能优化配置
 * 针对平台自身数据库的高性能配置
 */
@Slf4j
@Configuration
public class DruidConfig {

    /**
     * 平台数据库连接池配置（高性能版）
     * 接收 application.yml 中的 spring.datasource 配置
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DataSource platformDataSource() {
        DruidDataSourceWrapper dataSource = new DruidDataSourceWrapper();
        
        // 从 spring.datasource 配置中读取基本连接属性
        dataSource.setUrl("jdbc:mysql://49.232.153.150:3366/bagdatahouse?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&cachePrepStmts=true");
        dataSource.setUsername("root");
        dataSource.setPassword("sdfwer@re3f");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setDbType("mysql");
        
        return dataSource;
    }

    public static class DruidDataSourceWrapper extends DruidDataSource {

        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(DruidDataSourceWrapper.class);

        @Override
        public void init() {
            try {
                // PSCache优化
                if (!isPoolPreparedStatements()) {
                    setPoolPreparedStatements(true);
                }
                if (getMaxPoolPreparedStatementPerConnectionSize() < 50) {
                    setMaxPoolPreparedStatementPerConnectionSize(50);
                }
                
                // 连接泄漏检测
                if (!isRemoveAbandoned()) {
                    setRemoveAbandoned(true);
                }
                if (getRemoveAbandonedTimeout() < 1800) {
                    setRemoveAbandonedTimeout(1800);
                }
                
                super.init();
                
                log.info("Druid 连接池配置完成（高性能模式）: maxActive={}", getMaxActive());
            } catch (Exception e) {
                log.error("Druid 连接池初始化失败", e);
                throw new RuntimeException("Druid 连接池初始化失败", e);
            }
        }
    }
}
