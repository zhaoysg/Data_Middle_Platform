-- ============================================================
-- metadata_scan_log — 补齐下游处理统计字段
-- 兼容 MySQL 5.7/8.x，每个 ADD COLUMN 前用子查询判断列是否存在
-- 可在任何客户端中重复执行，已存在的列会跳过，不会报错
-- ============================================================

-- 列1：sensitive_matched
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'metadata_scan_log'
          AND COLUMN_NAME = 'sensitive_matched');
SET @s = IF(@c = 0,
  'ALTER TABLE `metadata_scan_log` ADD COLUMN `sensitive_matched` int DEFAULT NULL COMMENT ''下游处理：敏感识别匹配数'' AFTER `remark`',
  'SELECT ''skeptic skip'' AS result');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 列2：lineage_discovered
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'metadata_scan_log'
          AND COLUMN_NAME = 'lineage_discovered');
SET @s = IF(@c = 0,
  'ALTER TABLE `metadata_scan_log` ADD COLUMN `lineage_discovered` int DEFAULT NULL COMMENT ''下游处理：血缘关系发现数'' AFTER `sensitive_matched`',
  'SELECT ''skeptic skip'' AS result');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 列3：dqc_binded
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'metadata_scan_log'
          AND COLUMN_NAME = 'dqc_binded');
SET @s = IF(@c = 0,
  'ALTER TABLE `metadata_scan_log` ADD COLUMN `dqc_binded` int DEFAULT NULL COMMENT ''下游处理：DQC绑定数'' AFTER `lineage_discovered`',
  'SELECT ''skeptic skip'' AS result');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 列4：mask_applied
SET @c = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'metadata_scan_log'
          AND COLUMN_NAME = 'mask_applied');
SET @s = IF(@c = 0,
  'ALTER TABLE `metadata_scan_log` ADD COLUMN `mask_applied` int DEFAULT NULL COMMENT ''下游处理：脱敏应用数'' AFTER `dqc_binded`',
  'SELECT ''skeptic skip'' AS result');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
