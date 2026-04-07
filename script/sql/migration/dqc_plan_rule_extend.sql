-- ============================================================
-- DQC Plan Rule 扩展字段迁移
-- 新增字段：target_table, target_column, enabled, skip_on_failure
-- 支持按表-字段细粒度绑定质检规则
--
-- 使用方式: 在已有 dqc_plan_rule 表基础上执行
-- ============================================================

-- 新增目标表名字段
ALTER TABLE `dqc_plan_rule`
  ADD COLUMN `target_table` varchar(128) DEFAULT NULL COMMENT '目标表名' AFTER `rule_id`;

-- 新增目标字段名字段
ALTER TABLE `dqc_plan_rule`
  ADD COLUMN `target_column` varchar(128) DEFAULT NULL COMMENT '目标字段名' AFTER `target_table`;

-- 新增是否启用字段
ALTER TABLE `dqc_plan_rule`
  ADD COLUMN `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用' AFTER `sort_order`;

-- 新增失败时是否跳过字段
ALTER TABLE `dqc_plan_rule`
  ADD COLUMN `skip_on_failure` bit(1) NOT NULL DEFAULT b'0' COMMENT '失败时是否跳过' AFTER `enabled`;

-- 移除旧的唯一约束（原 uk_plan_rule 只基于 plan_id + rule_id）
ALTER TABLE `dqc_plan_rule`
  DROP INDEX `uk_plan_rule`;

-- 新增新的唯一约束（允许同一规则绑定到不同表/字段）
ALTER TABLE `dqc_plan_rule`
  ADD UNIQUE KEY `uk_plan_rule_col` (`plan_id`, `rule_id`, `target_table`, `target_column`);
