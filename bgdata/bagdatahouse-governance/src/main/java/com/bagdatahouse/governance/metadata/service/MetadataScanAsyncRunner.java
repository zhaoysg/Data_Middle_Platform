package com.bagdatahouse.governance.metadata.service;

import com.bagdatahouse.governance.metadata.context.MetadataScanContext;
import com.bagdatahouse.governance.metadata.scanner.MetadataScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 元数据异步扫描执行器（独立 Bean，确保 {@link Async} 经 Spring 代理生效）
 */
@Slf4j
@Component
public class MetadataScanAsyncRunner {

    @Async("taskExecutor")
    public void runScan(MetadataScanContext ctx, MetadataScanner scanner) {
        try {
            if (scanner == null) {
                ctx.setStatus("FAILED");
                return;
            }
            scanner.scan(ctx);
        } catch (Exception e) {
            ctx.setStatus("FAILED");
            log.error("异步元数据扫描失败: dsId={}, {}", ctx.getDatasource().getId(), e.getMessage(), e);
        }
    }
}
