-- ============================================================
-- dqc_plan — 补齐 alert_on_success 字段
-- 兼容 MySQL 5.7/8.x，用子查询判断列是否存在
-- 可在任何客户端中重复执行，已存在的列会跳过，不会报错
-- ============================================================

SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dqc_plan'
          AND COLUMN_NAME = 'alert_on_success');
SET @s = IF(@c = 0,
  'ALTER TABLE `dqc_plan` ADD COLUMN `alert_on_success` char(1) NOT NULL DEFAULT ''0'' COMMENT ''成功是否通知'' AFTER `alert_on_failure`',
  'SELECT ''skeptic skip'' AS result');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
