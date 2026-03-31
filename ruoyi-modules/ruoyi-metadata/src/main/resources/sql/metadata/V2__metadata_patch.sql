SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'metadata_column' AND COLUMN_NAME = 'create_dept'
);
SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE metadata_column ADD COLUMN create_dept BIGINT(20) DEFAULT NULL COMMENT ''创建部门'' AFTER del_flag',
  'SELECT ''metadata_column.create_dept exists'' AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_layer' AND COLUMN_NAME = 'tenant_id'
);
SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE data_layer ADD COLUMN tenant_id VARCHAR(20) DEFAULT ''000000'' COMMENT ''租户编号'' AFTER id',
  'SELECT ''data_layer.tenant_id exists'' AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_layer' AND COLUMN_NAME = 'create_dept'
);
SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE data_layer ADD COLUMN create_dept BIGINT(20) DEFAULT NULL COMMENT ''创建部门'' AFTER remark',
  'SELECT ''data_layer.create_dept exists'' AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'metadata_scan_log' AND COLUMN_NAME = 'tenant_id'
);
SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE metadata_scan_log ADD COLUMN tenant_id VARCHAR(20) DEFAULT ''000000'' COMMENT ''租户编号'' AFTER id',
  'SELECT ''metadata_scan_log.tenant_id exists'' AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'metadata_scan_log' AND COLUMN_NAME = 'create_dept'
);
SET @sql = IF(
  @column_exists = 0,
  'ALTER TABLE metadata_scan_log ADD COLUMN create_dept BIGINT(20) DEFAULT NULL COMMENT ''创建部门'' AFTER remark',
  'SELECT ''metadata_scan_log.create_dept exists'' AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_columns = (
  SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'metadata_table' AND INDEX_NAME = 'uk_ds_table'
  GROUP BY INDEX_NAME
);
SET @sql = IF(
  @index_columns IS NULL,
  'ALTER TABLE metadata_table ADD UNIQUE KEY uk_ds_table (tenant_id, ds_id, table_name)',
  IF(
    @index_columns = 'tenant_id,ds_id,table_name',
    'SELECT ''metadata_table.uk_ds_table ok'' AS result',
    'ALTER TABLE metadata_table DROP INDEX uk_ds_table, ADD UNIQUE KEY uk_ds_table (tenant_id, ds_id, table_name)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_columns = (
  SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'metadata_column' AND INDEX_NAME = 'uk_ds_table_column'
  GROUP BY INDEX_NAME
);
SET @sql = IF(
  @index_columns IS NULL,
  'ALTER TABLE metadata_column ADD UNIQUE KEY uk_ds_table_column (tenant_id, ds_id, table_name, column_name)',
  IF(
    @index_columns = 'tenant_id,ds_id,table_name,column_name',
    'SELECT ''metadata_column.uk_ds_table_column ok'' AS result',
    'ALTER TABLE metadata_column DROP INDEX uk_ds_table_column, ADD UNIQUE KEY uk_ds_table_column (tenant_id, ds_id, table_name, column_name)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_layer' AND INDEX_NAME = 'uk_tenant_layer_code'
);
SET @legacy_index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_layer' AND INDEX_NAME = 'uk_layer_code'
);
SET @sql = IF(
  @index_exists > 0,
  'SELECT ''data_layer.uk_tenant_layer_code ok'' AS result',
  IF(
    @legacy_index_exists > 0,
    'ALTER TABLE data_layer DROP INDEX uk_layer_code, ADD UNIQUE KEY uk_tenant_layer_code (tenant_id, layer_code)',
    'ALTER TABLE data_layer ADD UNIQUE KEY uk_tenant_layer_code (tenant_id, layer_code)'
  )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'data_layer' AND INDEX_NAME = 'idx_tenant_id'
);
SET @sql = IF(
  @index_exists = 0,
  'ALTER TABLE data_layer ADD KEY idx_tenant_id (tenant_id)',
  'SELECT ''data_layer.idx_tenant_id ok'' AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'metadata_scan_log' AND INDEX_NAME = 'idx_tenant_id'
);
SET @sql = IF(
  @index_exists = 0,
  'ALTER TABLE metadata_scan_log ADD KEY idx_tenant_id (tenant_id)',
  'SELECT ''metadata_scan_log.idx_tenant_id ok'' AS result'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE data_layer
SET create_by = '-1'
WHERE create_by IS NOT NULL
  AND create_by <> ''
  AND create_by NOT REGEXP '^-?[0-9]+$';

UPDATE data_layer
SET update_by = '-1'
WHERE update_by IS NOT NULL
  AND update_by <> ''
  AND update_by NOT REGEXP '^-?[0-9]+$';

UPDATE metadata_catalog
SET tenant_id = '000000'
WHERE tenant_id IS NULL OR tenant_id = '' OR tenant_id = '0';

UPDATE data_domain
SET tenant_id = '000000'
WHERE tenant_id IS NULL OR tenant_id = '' OR tenant_id = '0';

UPDATE data_layer
SET tenant_id = '000000'
WHERE tenant_id IS NULL OR tenant_id = '' OR tenant_id = '0';

UPDATE metadata_scan_log
SET tenant_id = '000000'
WHERE tenant_id IS NULL OR tenant_id = '' OR tenant_id = '0';

UPDATE metadata_table t
SET tenant_id = '000000'
WHERE (t.tenant_id IS NULL OR t.tenant_id = '' OR t.tenant_id = '0')
  AND NOT EXISTS (
    SELECT 1
    FROM (
      SELECT id, tenant_id, ds_id, table_name
      FROM metadata_table
    ) x
    WHERE x.id <> t.id
      AND x.tenant_id = '000000'
      AND x.ds_id <=> t.ds_id
      AND x.table_name = t.table_name
  );

UPDATE metadata_column c
SET tenant_id = '000000'
WHERE (c.tenant_id IS NULL OR c.tenant_id = '' OR c.tenant_id = '0')
  AND NOT EXISTS (
    SELECT 1
    FROM (
      SELECT id, tenant_id, ds_id, table_name, column_name
      FROM metadata_column
    ) x
    WHERE x.id <> c.id
      AND x.tenant_id = '000000'
      AND x.ds_id <=> c.ds_id
      AND x.table_name = c.table_name
      AND x.column_name = c.column_name
  );
