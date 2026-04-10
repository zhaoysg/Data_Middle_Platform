-- ============================================================
-- 清理脚本：截断所有基础数据表
-- 执行顺序：按依赖关系（先删子表，再删主表）
-- ============================================================

USE ry_bigdata_v1;

-- 先关闭外键检查，避免外键约束导致删除失败
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 敏感识别规则
TRUNCATE TABLE sec_sensitivity_rule;

-- 2. 脱敏模板
TRUNCATE TABLE sec_mask_template;

-- 3. 数据分类
TRUNCATE TABLE sec_classification;

-- 4. 敏感等级
TRUNCATE TABLE sec_level;

-- 5. DQC规则模板
TRUNCATE TABLE dqc_rule_template;

-- 6. 数仓分层（DIM/TMP 补充层）
DELETE FROM data_layer WHERE layer_code IN ('DIM', 'TMP');

-- 7. 数据域
TRUNCATE TABLE data_domain;

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 验证清理结果
SELECT '清理完成，当前数据统计：' AS message;
SELECT 'sec_sensitivity_rule' AS tbl, COUNT(*) AS cnt FROM sec_sensitivity_rule
UNION ALL SELECT 'sec_mask_template', COUNT(*) FROM sec_mask_template
UNION ALL SELECT 'sec_classification', COUNT(*) FROM sec_classification
UNION ALL SELECT 'sec_level', COUNT(*) FROM sec_level
UNION ALL SELECT 'dqc_rule_template', COUNT(*) FROM dqc_rule_template
UNION ALL SELECT 'data_layer', COUNT(*) FROM data_layer
UNION ALL SELECT 'data_domain', COUNT(*) FROM data_domain;
