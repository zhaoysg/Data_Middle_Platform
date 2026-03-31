package org.dromara.metadata.config;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 元数据核心表自修复初始化器
 */
@Slf4j
@Component
public class MetadataSchemaInitializer implements ApplicationRunner {

    private static final String BIGDATA_DS = "bigdata";
    private static final String SCRIPT_LOCATION = "classpath*:sql/metadata/*.sql";

    private final DataSource dataSource;
    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    @Value("${metadata.schema.auto-init:true}")
    private boolean autoInit;

    public MetadataSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!autoInit) {
            log.info("metadata.schema.auto-init=false，跳过元数据表初始化");
            return;
        }
        Resource[] scripts = resourceResolver.getResources(SCRIPT_LOCATION);
        if (scripts.length == 0) {
            log.warn("未找到元数据初始化脚本: {}", SCRIPT_LOCATION);
            return;
        }
        Arrays.sort(scripts, Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareTo)));
        DynamicDataSourceContextHolder.push(BIGDATA_DS);
        try (Connection connection = dataSource.getConnection()) {
            for (Resource script : scripts) {
                log.info("执行元数据初始化脚本: {}", script.getFilename());
                ScriptUtils.executeSqlScript(connection, new EncodedResource(script, StandardCharsets.UTF_8));
            }
            log.info("元数据核心表初始化完成");
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }
}
