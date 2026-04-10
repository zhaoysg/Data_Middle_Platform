package org.dromara.metadata.engine.executor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata context for rule execution.
 * Used to pass table name, column name, datasource info during rule execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetadataContext {

    /** Target table name */
    private String tableName;

    /** Target column name */
    private String columnName;

    /** Compare table name */
    private String compareTableName;

    /** Compare column name */
    private String compareColumnName;

    /** Target datasource ID */
    private Long dsId;

    /** Target datasource code */
    private String dsCode;

    /**
     * Create metadata context for target table
     */
    public static MetadataContext of(String tableName, String columnName, Long dsId, String dsCode) {
        return new MetadataContext(tableName, columnName, null, null, dsId, dsCode);
    }

    /**
     * Create metadata context for cross-field rules
     */
    public static MetadataContext crossField(String tableName, String columnName,
                                            String compareTableName, String compareColumnName,
                                            Long dsId, String dsCode) {
        return new MetadataContext(tableName, columnName, compareTableName, compareColumnName, dsId, dsCode);
    }
}
