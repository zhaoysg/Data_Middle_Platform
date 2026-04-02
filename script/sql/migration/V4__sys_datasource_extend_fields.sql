SET @has_sys_datasource = (
    SELECT COUNT(1)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_datasource'
);

SET @has_data_source = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_datasource'
      AND column_name = 'data_source'
);

SET @has_ds_flag = (
    SELECT COUNT(1)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_datasource'
      AND column_name = 'ds_flag'
);

SET @ddl = IF(
    @has_sys_datasource = 1 AND @has_data_source = 0,
    'ALTER TABLE `sys_datasource` ADD COLUMN `data_source` varchar(32) DEFAULT NULL COMMENT ''数据来源：K3DC/K3HW/K1/K2/OTHER'' AFTER `data_layer`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_sys_datasource = 1 AND @has_ds_flag = 0,
    'ALTER TABLE `sys_datasource` ADD COLUMN `ds_flag` char(1) DEFAULT ''0'' COMMENT ''数据源标识（0内部 1外部）'' AFTER `data_source`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
