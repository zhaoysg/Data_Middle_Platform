-- ============================================================
-- V12__dqc_rule_def_metadata_link.sql
-- DQC 规则定义 - 元数据驱动重构
-- 添加 table_id 和 column_id 字段，关联元数据表
-- 执行数据库：ry_bigdata_v1
-- ============================================================

USE ry_bigdata_v1;

-- 1. 添加元数据关联字段
-- table_id: 关联 metadata_table.id，用于从元数据中选择目标表
ALTER TABLE dqc_rule_def
ADD COLUMN table_id BIGINT COMMENT '关联元数据表ID (metadata_table.id)'
AFTER template_id;

-- column_id: 关联 metadata_column.id，用于从元数据中选择目标字段
ALTER TABLE dqc_rule_def
ADD COLUMN column_id BIGINT COMMENT '关联元数据字段ID (metadata_column.id)'
AFTER table_id;

-- 2. 添加索引以优化查询性能
CREATE INDEX idx_rule_def_table_id ON dqc_rule_def(table_id);
CREATE INDEX idx_rule_def_column_id ON dqc_rule_def(column_id);

-- 3. 验证修复结果
SELECT '元数据关联字段添加完成！' AS message;
DESC dqc_rule_def;
