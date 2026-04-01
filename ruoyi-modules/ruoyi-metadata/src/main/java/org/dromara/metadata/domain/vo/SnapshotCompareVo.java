package org.dromara.metadata.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 快照对比结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnapshotCompareVo {

    /**
     * 快照1信息
     */
    private SnapshotInfo snapshot1;

    /**
     * 快照2信息
     */
    private SnapshotInfo snapshot2;

    /**
     * 新增的表
     */
    private List<String> newTables;

    /**
     * 删除的表
     */
    private List<String> removedTables;

    /**
     * 变化的表详情
     */
    private List<TableChange> changedTables;

    public SnapshotCompareVo(SnapshotInfo snapshot1, SnapshotInfo snapshot2) {
        this.snapshot1 = snapshot1;
        this.snapshot2 = snapshot2;
        this.newTables = new ArrayList<>();
        this.removedTables = new ArrayList<>();
        this.changedTables = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnapshotInfo {
        private Long id;
        private String snapshotCode;
        private String snapshotName;
        private String createTime;
        private int tableCount;
        private Map<String, TableSnapshotData> tables;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableSnapshotData {
        private String tableName;
        private Long rowCount;
        private Integer columnCount;
        private String lastModified;
        private Map<String, ColumnSnapshotData> columns;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnSnapshotData {
        private String columnName;
        private String dataType;
        private Long nullCount;
        private Double nullRate;
        private Long uniqueCount;
        private Double uniqueRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableChange {
        private String tableName;
        private ChangeType changeType;
        private String description;
        private Long rowCountBefore;
        private Long rowCountAfter;
        private Long rowCountChange;
        private Integer columnCountBefore;
        private Integer columnCountAfter;
        private List<ColumnChange> columnChanges;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnChange {
        private String columnName;
        private ChangeType changeType;
        private Double nullRateBefore;
        private Double nullRateAfter;
        private Long uniqueCountBefore;
        private Long uniqueCountAfter;
    }

    public enum ChangeType {
        ADDED,
        REMOVED,
        MODIFIED
    }
}
