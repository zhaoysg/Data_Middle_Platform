-- ============================================================
-- DQC Plan 扩展字段迁移
-- 新增字段：alert_on_success, sensitivity_level
-- 支持成功通知开关和敏感等级绑定
--
-- 使用方式: 在已有 dqc_plan 表基础上执行
-- ============================================================

-- 新增成功通知字段
ALTER TABLE `dqc_plan`
  ADD COLUMN `alert_on_success` char(1) NOT NULL DEFAULT '0' COMMENT '成功是否通知';

-- 新增敏感等级字段
ALTER TABLE `dqc_plan`
  ADD COLUMN `sensitivity_level` varchar(20) DEFAULT NULL COMMENT '敏感等级 L1/L2/L3/L4';
