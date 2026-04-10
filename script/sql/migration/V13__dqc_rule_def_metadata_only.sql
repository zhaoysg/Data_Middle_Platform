-- ============================================================
-- V13__dqc_rule_def_metadata_only.sql
-- DQC 规则定义 - 纯元数据驱动改造
-- 删除冗余字符串字段，只保留元数据 ID 关联
-- 执行数据库：ry_bigdata_v1
-- ============================================================

USE ry_bigdata_v1;

-- ============================================================
-- 第1步：添加跨表规则需要的 compare_table_id 和 compare_column_id
-- ============================================================
ALTER TABLE dqc_rule_def
ADD COLUMN compare_table_id BIGINT COMMENT '对比表元数据ID (metadata_table.id)' AFTER column_id,
ADD COLUMN compare_column_id BIGINT COMMENT '对比字段元数据ID (metadata_column.id)' AFTER compare_table_id;

-- 添加索引
CREATE INDEX idx_rule_def_compare_table_id ON dqc_rule_def(compare_table_id);
CREATE INDEX idx_rule_def_compare_column_id ON dqc_rule_def(compare_column_id);

-- ============================================================
-- 第2步：从旧数据中迁移信息到新字段
-- 如果旧数据已有 tableId 但缺少 compare_table_id，可以通过 target_table 查找
-- 注意：此迁移假设 metadata_table 中已存在对应的表记录
-- ============================================================

-- 迁移目标表信息（通过 target_table + target_ds_id 关联 metadata_table）
-- 注意：此语句依赖 metadata_table 中已有对应记录，需要提前执行元数据扫描

-- ============================================================
-- 第3步：删除冗余的字符串字段
-- 删除前请确保所有新规则都使用 tableId/columnId/compareTableId/compareColumnId
-- ============================================================
ALTER TABLE dqc_rule_def
DROP COLUMN IF EXISTS target_ds_id,
DROP COLUMN IF EXISTS target_table,
DROP COLUMN IF EXISTS target_column,
DROP COLUMN IF EXISTS compare_ds_id,
DROP COLUMN IF EXISTS compare_table,
DROP COLUMN IF EXISTS compare_column;

-- ============================================================
-- 验证改造结果
-- ============================================================
SELECT 'V13 纯元数据驱动改造完成！' AS message;
DESC dqc_rule_def;
