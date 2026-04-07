-- ==============================================================
-- Migration: dprofile_task add target_columns column
-- Add column-level profiling control: comma-separated column names
-- ==============================================================

ALTER TABLE `dprofile_task`
  ADD COLUMN `target_columns` varchar(4000) DEFAULT NULL
  COMMENT '指定探查的列名（逗号分隔，留空表示全部列）'
  AFTER `table_pattern`;

-- ==============================================================
-- Migration: dqc_rule_def add rule_strength column
-- (kept from original file)
-- ==============================================================

-- ALTER TABLE `dqc_rule_def` ADD COLUMN `rule_strength` varchar(20) DEFAULT NULL COMMENT '规则强度 STRONG/WEAK';
