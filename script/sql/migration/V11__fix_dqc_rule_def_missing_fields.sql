-- ============================================================
-- V11__fix_dqc_rule_def_missing_fields.sql
-- 修复 dqc_rule_def 表缺失字段
-- 执行数据库：ry_bigdata_v1
-- ============================================================

USE ry_bigdata_v1;

-- 1. 添加 rule_strength 字段（规则强度）
ALTER TABLE dqc_rule_def
ADD COLUMN IF NOT EXISTS rule_strength VARCHAR(20) DEFAULT 'WEAK' COMMENT '规则强度：STRONG-强规则 / WEAK-弱规则'
AFTER error_level;

-- 2. 验证修复结果
SELECT '字段修复完成！' AS message;

-- 3. 查看当前表结构
DESC dqc_rule_def;
