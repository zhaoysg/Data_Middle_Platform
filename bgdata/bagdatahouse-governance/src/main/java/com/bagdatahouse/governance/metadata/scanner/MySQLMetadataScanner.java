package com.bagdatahouse.governance.metadata.scanner;

import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.governance.metadata.context.MetadataScanContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL / TiDB 元数据扫描器
 */
@Slf4j
@Component
public class MySQLMetadataScanner extends AbstractMetadataScanner {

    @Override
    public String getDataSourceType() {
        return "MYSQL";
    }

    @Override
    protected List<String> getAllTables(DataSourceAdapter adapter) {
        try {
            return adapter.getTables();
        } catch (Exception e) {
            log.error("获取MySQL表列表失败: {}", e.getMessage(), e);
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

    @Override
    public void scan(MetadataScanContext ctx) {
        // TiDB extends MySQL, also handled here
        super.scan(ctx);
    }
}
