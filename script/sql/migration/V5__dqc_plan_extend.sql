-- ============================================================
-- DQC Plan 扩展字段迁移
-- 兼容不支持 `ADD COLUMN IF NOT EXISTS` 的 MySQL 版本
-- ============================================================

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'dqc_plan'
        AND COLUMN_NAME = 'alert_on_success'
    ),
    'SELECT 1',
    'ALTER TABLE `dqc_plan` ADD COLUMN `alert_on_success` char(1) NOT NULL DEFAULT ''0'' COMMENT ''成功是否通知'' AFTER `alert_on_failure`'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'dqc_plan'
        AND COLUMN_NAME = 'sensitivity_level'
    ),
    'SELECT 1',
    'ALTER TABLE `dqc_plan` ADD COLUMN `sensitivity_level` varchar(20) DEFAULT NULL COMMENT ''敏感等级 L1/L2/L3/L4'' AFTER `auto_block`'
  )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
