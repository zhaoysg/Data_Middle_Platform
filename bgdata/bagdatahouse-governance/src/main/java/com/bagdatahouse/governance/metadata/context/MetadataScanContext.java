package com.bagdatahouse.governance.metadata.context;

import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.entity.GovMetadata;
import com.bagdatahouse.core.entity.GovMetadataColumn;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 元数据扫描上下文
 * 承载一次扫描任务的全量状态，供各阶段共享使用
 */
@Data
@Builder
public class MetadataScanContext {

    private String taskId;
    private DqDatasource datasource;
    private DataSourceAdapter adapter;
    private String scanScope;
    private List<String> tableNames;
    private String tablePattern;
    private String dataLayer;
    private String dataDomain;
    private String bizDomain;
    private boolean collectStats;
    private boolean collectColumnStats;
    private boolean overwriteExisting;
    private Long createUser;

    @Builder.Default
    private final long startTime = System.currentTimeMillis();

    @Builder.Default
    private final List<GovMetadata> metadataList = new ArrayList<>();

    @Builder.Default
    private final List<GovMetadataColumn> columnList = new ArrayList<>();

    @Builder.Default
    private final List<TableError> errors = new ArrayList<>();

    @Builder.Default
    private final AtomicInteger successCount = new AtomicInteger(0);

    @Builder.Default
    private final AtomicInteger failedCount = new AtomicInteger(0);

    @Builder.Default
    private final AtomicInteger insertedCount = new AtomicInteger(0);

    @Builder.Default
    private final AtomicInteger updatedCount = new AtomicInteger(0);

    @Builder.Default
    private final AtomicInteger skippedCount = new AtomicInteger(0);

    @Builder.Default
    private final AtomicInteger totalColumns = new AtomicInteger(0);

    @Builder.Default
    private volatile String status = "RUNNING";

    /**
     * 本次任务计划扫描的表数量（用于异步进度展示）
     */
    @Builder.Default
    private volatile int totalPlannedTables = 0;

    public long getElapsedMs() {
        return System.currentTimeMillis() - startTime;
    }

    public void addError(String tableName, String message, Throwable ex) {
        errors.add(new TableError(tableName, message, ex != null ? ex.getClass().getSimpleName() : null));
        failedCount.incrementAndGet();
    }

    public void addSuccess(String tableName, boolean inserted) {
        successCount.incrementAndGet();
        if (inserted) {
            insertedCount.incrementAndGet();
        } else {
            updatedCount.incrementAndGet();
        }
    }

    public void addSkipped() {
        skippedCount.incrementAndGet();
    }

    public void addColumns(int count) {
        totalColumns.addAndGet(count);
    }

    public record TableError(
            String tableName,
            String message,
            String exceptionType
    ) {}
}
