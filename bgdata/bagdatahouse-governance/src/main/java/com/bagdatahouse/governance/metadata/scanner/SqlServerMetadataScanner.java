package com.bagdatahouse.governance.metadata.scanner;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.governance.metadata.context.MetadataScanContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SQL Server 元数据扫描器
 */
@Slf4j
@Component
public class SqlServerMetadataScanner extends AbstractMetadataScanner {

    @Override
    public String getDataSourceType() {
        return "SQLSERVER";
    }

    @Override
    protected List<String> getAllTables(DataSourceAdapter adapter) {
        try {
            return adapter.getTables();
        } catch (Exception e) {
            log.error("获取SQL Server表列表失败: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    protected List<String> filterTablesByPattern(List<String> allTables, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return allTables;
        }
        String regex = pattern
                .replace("%", ".*")
                .replace("_", ".");
        return allTables.stream()
                .filter(t -> t.matches(regex))
                .collect(Collectors.toList());
    }
}
