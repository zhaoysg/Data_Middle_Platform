-- ============================================================
-- 元数据平台 Phase3 DDL + Seed Data
-- 所有 Phase 3 新表和初始化数据集中在此文件
-- ============================================================

-- -------------------------------------------------------
-- Part 1: DqcQualityScore 表（Track A 共享评分表）
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `dqc_quality_score` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `check_date` date DEFAULT NULL COMMENT '质检日期',
  `target_ds_id` bigint DEFAULT NULL COMMENT '目标数据源ID',
  `target_table` varchar(200) DEFAULT NULL COMMENT '目标表名',
  `layer_code` varchar(20) DEFAULT NULL COMMENT '数据层编码 ODS/DWD/DWS/ADS',
  `execution_id` bigint DEFAULT NULL COMMENT '质检执行ID',
  `plan_id` bigint DEFAULT NULL COMMENT '质检方案ID',
  `plan_name` varchar(100) DEFAULT NULL COMMENT '质检方案名称',
  `completeness_score` decimal(5,2) DEFAULT NULL COMMENT '完整性得分 0-100',
  `uniqueness_score` decimal(5,2) DEFAULT NULL COMMENT '唯一性得分 0-100',
  `accuracy_score` decimal(5,2) DEFAULT NULL COMMENT '准确性得分 0-100',
  `consistency_score` decimal(5,2) DEFAULT NULL COMMENT '一致性得分 0-100',
  `timeliness_score` decimal(5,2) DEFAULT NULL COMMENT '及时性得分 0-100',
  `validity_score` decimal(5,2) DEFAULT NULL COMMENT '有效性得分 0-100',
  `overall_score` decimal(5,2) DEFAULT NULL COMMENT '综合得分 0-100',
  `rule_pass_rate` decimal(5,2) DEFAULT NULL COMMENT '规则通过率 0-100',
  `rule_total_count` int DEFAULT NULL COMMENT '规则总数',
  `rule_pass_count` int DEFAULT NULL COMMENT '规则通过数',
  `rule_fail_count` int DEFAULT NULL COMMENT '规则失败数',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 1删除',
  PRIMARY KEY (`id`),
  KEY `idx_check_date` (`check_date`),
  KEY `idx_ds_id` (`target_ds_id`),
  KEY `idx_ds_table` (`target_ds_id`, `target_table`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_layer_code` (`layer_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据质量评分表';

-- -------------------------------------------------------
-- Part 2: V7 Seeds — DQC 21个内置规则模板
-- -------------------------------------------------------

INSERT IGNORE INTO `dqc_rule_template` (
  `template_code`, `template_name`, `template_desc`,
  `rule_type`, `apply_level`, `dimension`, `default_expr`,
  `threshold_json`, `param_spec`, `builtin`, `enabled`,
  `create_dept`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`
) VALUES
-- 完整性维度（COMPLETENESS）
('TPL_NULL_CHECK', '空值检查', '检测指定字段的空值数量和空值率', 'NULL_CHECK', 'COLUMN', 'COMPLETENESS',
 'SELECT COUNT(*) FROM {table} WHERE {column} IS NULL', '{"maxNullRate": 0.05}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_COMPLETENESS_RATE', '完整率检查', '检测字段非空率是否达标', 'THRESHOLD', 'COLUMN', 'COMPLETENESS',
 'SELECT (COUNT(*) - COUNT({column})) * 100.0 / COUNT(*) FROM {table}',
 '{"minValue": 95.0, "maxValue": 100.0}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_ROW_COUNT', '记录数检查', '检测表的记录数是否在合理范围', 'THRESHOLD', 'TABLE', 'COMPLETENESS',
 'SELECT COUNT(*) FROM {table}',
 '{"minValue": 1, "maxValue": null}',
 '[{"name":"table","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

-- 唯一性维度（UNIQUENESS）
('TPL_UNIQUE_COUNT', '唯一值数量', '检测指定字段的唯一值数量', 'UNIQUE', 'COLUMN', 'UNIQUENESS',
 'SELECT COUNT(DISTINCT {column}) FROM {table}',
 '{"minValue": 1, "maxValue": null}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_DUPLICATE_COUNT', '重复值检查', '检测指定字段的重复记录数', 'UNIQUE', 'COLUMN', 'UNIQUENESS',
 'SELECT COUNT(*) - COUNT(DISTINCT {column}) FROM {table} WHERE {column} IS NOT NULL',
 '{"maxValue": 0}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

-- 准确性维度（ACCURACY）
('TPL_REGEX_MATCH', '格式校验', '检测字段值是否符合正则表达式', 'REGEX', 'COLUMN', 'ACCURACY',
 'SELECT COUNT(*) FROM {table} WHERE {column} IS NOT NULL AND {column} NOT REGEXP ''{pattern}''',
 '{"maxValue": 0}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true},{"name":"pattern","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_ACCURACY_ENUM', '枚举值校验', '检测字段值是否在允许的枚举值范围内', 'THRESHOLD', 'COLUMN', 'ACCURACY',
 'SELECT COUNT(*) FROM {table} WHERE {column} IS NOT NULL AND {column} NOT IN ({enumValues})',
 '{"maxValue": 0}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true},{"name":"enumValues","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_CROSS_FIELD_COMPARE', '跨字段一致性', '检测两个字段之间的数值一致性', 'CUSTOM_SQL', 'CROSS_FIELD', 'ACCURACY',
 'SELECT COUNT(*) FROM {table} WHERE {column1} <> {column2}',
 '{"maxValue": 0}',
 '[{"name":"column1","type":"STRING","required":true},{"name":"column2","type":"STRING","required":true},{"name":"table","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

-- 一致性维度（CONSISTENCY）
('TPL_DATA_TYPE_CONSISTENT', '数据类型一致性', '检测列的数据类型是否一致', 'REGEX', 'COLUMN', 'CONSISTENCY',
 'SELECT COUNT(*) FROM {table} WHERE {column} IS NOT NULL AND LENGTH({column}) <> LENGTH(CAST({column} AS CHAR))',
 '{"maxValue": 0}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_FK_REFERENCE', '外键引用一致性', '检测外键值是否都在主表存在', 'CUSTOM_SQL', 'CROSS_TABLE', 'CONSISTENCY',
 'SELECT COUNT(*) FROM {child_table} t1 LEFT JOIN {parent_table} t2 ON t1.{fk_column}=t2.{pk_column} WHERE t1.{fk_column} IS NOT NULL AND t2.{pk_column} IS NULL',
 '{"maxValue": 0}',
 '[{"name":"child_table","type":"STRING","required":true},{"name":"parent_table","type":"STRING","required":true},{"name":"fk_column","type":"STRING","required":true},{"name":"pk_column","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_CODE_MAPPING', '码值映射一致性', '检测码值是否在标准码表中存在', 'CUSTOM_SQL', 'COLUMN', 'CONSISTENCY',
 'SELECT COUNT(*) FROM {table} t1 LEFT JOIN {code_table} t2 ON t1.{code_column}=t2.{standard_column} WHERE t1.{code_column} IS NOT NULL AND t2.{standard_column} IS NULL',
 '{"maxValue": 0}',
 '[{"name":"table","type":"STRING","required":true},{"name":"code_column","type":"STRING","required":true},{"name":"code_table","type":"STRING","required":true},{"name":"standard_column","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

-- 及时性维度（TIMELINESS）
('TPL_UPDATE_TIMELINESS', '数据更新及时性', '检测数据是否在规定时间内更新', 'UPDATE_TIMELINESS', 'TABLE', 'TIMELINESS',
 'SELECT MAX({update_time_column}) FROM {table}',
 '{"maxAgeHours": 24}',
 '[{"name":"table","type":"STRING","required":true},{"name":"update_time_column","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_FRESHNESS', '数据新鲜度', '检测最新一条记录的时间戳', 'UPDATE_TIMELINESS', 'TABLE', 'TIMELINESS',
 'SELECT TIMESTAMPDIFF(HOUR, MAX({time_column}), NOW()) FROM {table}',
 '{"maxValue": 24}',
 '[{"name":"table","type":"STRING","required":true},{"name":"time_column","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

-- 有效性维度（VALIDITY）
('TPL_VALUE_RANGE', '数值范围检查', '检测字段值是否在指定范围内', 'THRESHOLD', 'COLUMN', 'VALIDITY',
 'SELECT COUNT(*) FROM {table} WHERE {column} < {minValue} OR {column} > {maxValue}',
 '{"maxValue": 0}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true},{"name":"minValue","type":"DECIMAL","required":false},{"name":"maxValue","type":"DECIMAL","required":false}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_LENGTH_CHECK', '字段长度检查', '检测字符串字段长度是否超限', 'THRESHOLD', 'COLUMN', 'VALIDITY',
 'SELECT COUNT(*) FROM {table} WHERE LENGTH({column}) > {maxLength}',
 '{"maxValue": 0}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true},{"name":"maxLength","type":"INTEGER","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_EMAIL_FORMAT', '邮箱格式校验', '检测邮箱字段格式是否有效', 'REGEX', 'COLUMN', 'VALIDITY',
 'SELECT COUNT(*) FROM {table} WHERE {column} IS NOT NULL AND {column} NOT REGEXP ''^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$''',
 '{"maxValue": 0}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

('TPL_PHONE_FORMAT', '手机号格式校验', '检测手机号字段格式是否有效', 'REGEX', 'COLUMN', 'VALIDITY',
 'SELECT COUNT(*) FROM {table} WHERE {column} IS NOT NULL AND LENGTH({column}) = 11 AND {column} NOT REGEXP ''^1[3-9][0-9]{9}$''',
 '{"maxValue": 0}',
 '[{"name":"column","type":"STRING","required":true},{"name":"table","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

-- 波动检测（FLUCTUATION）
('TPL_VALUE_FLUCTUATION', '数值波动检测', '检测当前值与历史基线的波动是否超出阈值', 'FLUCTUATION', 'COLUMN', 'CONSISTENCY',
 'SELECT ABS(AVG({column}) - (SELECT AVG({column}) FROM {table} WHERE {time_column} >= DATE_SUB(NOW(), INTERVAL {baselineDays} DAY) AND {time_column} < DATE_SUB(NOW(), INTERVAL {compareDays} DAY)))',
 '{"maxFluctuation": 0.2}',
 '[{"name":"column","type":"DECIMAL","required":true},{"name":"table","type":"STRING","required":true},{"name":"time_column","type":"STRING","required":true},{"name":"baselineDays","type":"INTEGER","required":false},{"name":"compareDays","type":"INTEGER","required":false}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

-- 自定义SQL
('TPL_CUSTOM_SQL', '自定义SQL检查', '用户编写自定义SQL，返回非0表示检查失败', 'CUSTOM_SQL', 'TABLE', 'CUSTOM',
 '{customSql}',
 '{}',
 '[{"name":"customSql","type":"TEXT","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0'),

-- 汇总行数检查
('TPL_SUM_CHECK', '汇总一致性检查', '检测子表汇总是否与主表一致', 'CUSTOM_SQL', 'CROSS_TABLE', 'ACCURACY',
 'SELECT ABS(COALESCE((SELECT SUM({child_column}) FROM {child_table}), 0) - COALESCE((SELECT SUM({parent_column}) FROM {parent_table}), 0))',
 '{"maxValue": 0}',
 '[{"name":"child_table","type":"STRING","required":true},{"name":"child_column","type":"STRING","required":true},{"name":"parent_table","type":"STRING","required":true},{"name":"parent_column","type":"STRING","required":true}]',
 '1', '1', NULL, 'admin', NOW(), 'admin', NOW(), '0');

-- -------------------------------------------------------
-- Part 3: Sec_* 简化版表（Track B，去掉审批流）
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `sec_classification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `class_code` varchar(50) NOT NULL COMMENT '分类编码',
  `class_name` varchar(100) NOT NULL COMMENT '分类名称',
  `class_desc` varchar(500) DEFAULT NULL COMMENT '分类描述',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用 0否 1是',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_code` (`class_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据分类表';

CREATE TABLE IF NOT EXISTS `sec_level` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `level_code` varchar(50) NOT NULL COMMENT '敏感等级编码',
  `level_name` varchar(100) NOT NULL COMMENT '敏感等级名称',
  `level_value` int NOT NULL COMMENT '等级值 1-4',
  `level_desc` varchar(500) DEFAULT NULL COMMENT '等级描述',
  `color` varchar(20) DEFAULT NULL COMMENT '前端展示颜色',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_code` (`level_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感等级表';

CREATE TABLE IF NOT EXISTS `sec_sensitivity_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_type` varchar(50) NOT NULL COMMENT '识别类型 REGEX/COLUMN_NAME/DATA_TYPE/CUSTOM',
  `rule_expr` varchar(500) DEFAULT NULL COMMENT '匹配表达式 JSON格式',
  `target_level_code` varchar(50) DEFAULT NULL COMMENT '关联的敏感等级',
  `target_class_code` varchar(50) DEFAULT NULL COMMENT '关联的数据分类',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用',
  `builtin` char(1) DEFAULT '0' COMMENT '是否内置规则 0否 1是',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感识别规则表';

CREATE TABLE IF NOT EXISTS `sec_mask_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_type` varchar(30) NOT NULL COMMENT '脱敏类型 ENCRYPT/MASK/HIDE/DELETE/SHUFFLE/CUSTOM',
  `mask_expr` varchar(500) NOT NULL COMMENT '脱敏表达式 如 MASK(name, ''*'', 3, 4)',
  `template_desc` varchar(500) DEFAULT NULL COMMENT '模板描述',
  `builtin` char(1) DEFAULT '0' COMMENT '是否内置',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脱敏模板表';

CREATE TABLE IF NOT EXISTS `sec_column_sensitivity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `ds_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `column_name` varchar(100) NOT NULL COMMENT '字段名',
  `data_type` varchar(50) DEFAULT NULL COMMENT '数据类型',
  `level_code` varchar(50) DEFAULT NULL COMMENT '敏感等级',
  `class_code` varchar(50) DEFAULT NULL COMMENT '数据分类',
  `identified_by` varchar(50) DEFAULT 'AUTO' COMMENT '识别方式 AUTO/MANUAL',
  `scan_task_id` bigint DEFAULT NULL COMMENT '扫描任务ID',
  `confirmed` char(1) DEFAULT '0' COMMENT '是否确认 0待确认 1已确认',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_table_column` (`ds_id`, `table_name`, `column_name`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_level_code` (`level_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字段敏感记录表';

CREATE TABLE IF NOT EXISTS `sec_mask_strategy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `strategy_code` varchar(50) NOT NULL COMMENT '策略编码',
  `strategy_name` varchar(100) NOT NULL COMMENT '策略名称',
  `strategy_desc` varchar(500) DEFAULT NULL COMMENT '策略描述',
  `ds_id` bigint NOT NULL COMMENT '关联数据源ID',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_code` (`strategy_code`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脱敏策略表';

CREATE TABLE IF NOT EXISTS `sec_mask_strategy_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` bigint NOT NULL COMMENT '策略ID',
  `ds_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `column_name` varchar(100) NOT NULL COMMENT '字段名',
  `template_code` varchar(50) NOT NULL COMMENT '脱敏模板编码',
  `output_alias` varchar(100) DEFAULT NULL COMMENT '输出别名',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脱敏策略明细表';

CREATE TABLE IF NOT EXISTS `sec_access_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '访问时间',
  `ds_id` bigint NOT NULL COMMENT '数据源ID',
  `user_id` bigint DEFAULT NULL COMMENT '访问用户ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '访问用户名',
  `query_sql` text COMMENT '查询SQL',
  `masked_sql` text COMMENT '脱敏后SQL',
  `result_rows` int DEFAULT 0 COMMENT '返回行数',
  `masked_columns` varchar(500) DEFAULT NULL COMMENT '脱敏字段列表 JSON',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '耗时',
  `status` varchar(20) DEFAULT 'SUCCESS' COMMENT '状态 SUCCESS/FAILED',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '客户端IP',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脱敏访问日志表';

-- -------------------------------------------------------
-- Part 4: V8 Seeds — 脱敏体系初始化数据
-- -------------------------------------------------------

INSERT IGNORE INTO `sec_classification` (`class_code`, `class_name`, `class_desc`, `sort_order`, `enabled`, `create_by`, `create_time`, `del_flag`) VALUES
('PERSONAL', '个人信息', '涉及个人身份识别或隐私的信息', 1, '1', 'admin', NOW(), '0'),
('FINANCIAL', '财务数据', '涉及金额、账户、交易等财务信息', 2, '1', 'admin', NOW(), '0'),
('BUSINESS', '商业机密', '涉及商业策略、客户、供应商等机密信息', 3, '1', 'admin', NOW(), '0'),
('TECHNICAL', '技术数据', '涉及系统架构、密码、技术参数等', 4, '1', 'admin', NOW(), '0');

INSERT IGNORE INTO `sec_level` (`level_code`, `level_name`, `level_value`, `level_desc`, `color`, `sort_order`, `enabled`, `create_by`, `create_time`, `del_flag`) VALUES
('LEVEL1', '公开', 1, '可对外公开的信息', '#52C41A', 1, '1', 'admin', NOW(), '0'),
('LEVEL2', '内部', 2, '仅内部人员可访问', '#1677FF', 2, '1', 'admin', NOW(), '0'),
('LEVEL3', '机密', 3, '需要特殊权限才可访问', '#FAAD14', 3, '1', 'admin', NOW(), '0'),
('LEVEL4', '绝密', 4, '最高安全级别，严格管控', '#FF4D4F', 4, '1', 'admin', NOW(), '0');

INSERT IGNORE INTO `sec_sensitivity_rule` (`rule_code`, `rule_name`, `rule_type`, `rule_expr`, `target_level_code`, `target_class_code`, `builtin`, `enabled`, `create_by`, `create_time`, `del_flag`) VALUES
('RULE_IDCARD', '身份证号识别', 'REGEX', '{"pattern":"(^[1-9]\\\\d{5}(18|19|20)\\\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\\\d|3[01])\\\\d{3}[\\\\dXx]$)"}', 'LEVEL3', 'PERSONAL', '1', '1', 'admin', NOW(), '0'),
('RULE_PHONE', '手机号识别', 'REGEX', '{"pattern":"(^1[3-9]\\\\d{9}$)"}', 'LEVEL2', 'PERSONAL', '1', '1', 'admin', NOW(), '0'),
('RULE_EMAIL', '邮箱地址识别', 'REGEX', '{"pattern":"(^[_\\\\w\\\\.-]+@[_\\\\w\\\\.-]+\\\\.\\\\w{2,}$)"}', 'LEVEL2', 'PERSONAL', '1', '1', 'admin', NOW(), '0'),
('RULE_BANK_CARD', '银行卡号识别', 'REGEX', '{"pattern":"(^\\\\d{16,19}$)"}', 'LEVEL4', 'FINANCIAL', '1', '1', 'admin', NOW(), '0'),
('RULE_PASSWORD', '密码字段识别', 'COLUMN_NAME', '{"columnNames":["password","pwd","passwd","secret","token","apikey","api_key","access_key"]}', 'LEVEL4', 'TECHNICAL', '1', '1', 'admin', NOW(), '0'),
('RULE_IP_ADDRESS', 'IP地址识别', 'REGEX', '{"pattern":"(^(\\\\d{1,3}\\\\.){3}\\\\d{1,3}$)"}', 'LEVEL2', 'TECHNICAL', '1', '1', 'admin', NOW(), '0'),
('RULE_ADDRESS', '详细地址识别', 'COLUMN_NAME', '{"columnNames":["address","addr","home_address","company_address"]}', 'LEVEL3', 'PERSONAL', '1', '1', 'admin', NOW(), '0'),
('RULE_REAL_NAME', '真实姓名识别', 'COLUMN_NAME', '{"columnNames":["real_name","realname","name","user_name","username","full_name","fullname"]}', 'LEVEL2', 'PERSONAL', '1', '1', 'admin', NOW(), '0'),
('RULE_SALARY', '薪资信息识别', 'COLUMN_NAME', '{"columnNames":["salary","wage","bonus","commission","income"]}', 'LEVEL4', 'FINANCIAL', '1', '1', 'admin', NOW(), '0'),
('RULE_ID_NUMBER', '证件号码识别', 'COLUMN_NAME', '{"columnNames":["id_card","idcard","passport","license_no"]}', 'LEVEL3', 'PERSONAL', '1', '1', 'admin', NOW(), '0'),
('RULE_COMPANY_NAME', '公司名称识别', 'COLUMN_NAME', '{"columnNames":["company","company_name","corp_name","organization"]}', 'LEVEL2', 'BUSINESS', '1', '1', 'admin', NOW(), '0'),
('RULE_CREDIT_CODE', '统一社会信用代码', 'REGEX', '{"pattern":"(^[0-9A-HJ-NPQRTUWXY]{2}\\\\d{6}[0-9A-HJ-NPQRTUWXY]{10}$)"}', 'LEVEL3', 'BUSINESS', '1', '1', 'admin', NOW(), '0'),
('RULE_BIRTH_DATE', '出生日期识别', 'COLUMN_NAME', '{"columnNames":["birthday","birth_date","dob","date_of_birth"]}', 'LEVEL2', 'PERSONAL', '1', '1', 'admin', NOW(), '0'),
('RULE_GENDER', '性别识别', 'COLUMN_NAME', '{"columnNames":["gender","sex"]}', 'LEVEL1', 'PERSONAL', '1', '1', 'admin', NOW(), '0'),
('RULE_SSN', '社保号码识别', 'REGEX', '{"pattern":"(^\\\\d{15}|\\\\d{18}$)"}', 'LEVEL4', 'PERSONAL', '1', '1', 'admin', NOW(), '0');

INSERT IGNORE INTO `sec_mask_template` (`template_code`, `template_name`, `template_type`, `mask_expr`, `template_desc`, `builtin`, `enabled`, `create_by`, `create_time`, `del_flag`) VALUES
('MASK_FULL', '完全脱敏', 'MASK', 'REPEAT(''*'', LENGTH({col}))', '所有字符替换为*', '1', '1', 'admin', NOW(), '0'),
('MASK_PARTIAL', '部分脱敏', 'MASK', 'CONCAT(LEFT({col}, 3), REPEAT(''*'', LENGTH({col})-6), RIGHT({col}, 3))', '保留前3后3，中间脱敏', '1', '1', 'admin', NOW(), '0'),
('MASK_EMAIL', '邮箱脱敏', 'MASK', 'CONCAT(LEFT({col}, 2), ''***'', SUBSTRING_INDEX({col}, ''@'', -1))', '邮箱脱敏，保留前缀2位', '1', '1', 'admin', NOW(), '0'),
('MASK_PHONE', '手机号脱敏', 'MASK', 'CONCAT(LEFT({col}, 3), ''****'', RIGHT({col}, 4))', '手机号中间4位脱敏', '1', '1', 'admin', NOW(), '0'),
('MASK_IDCARD', '身份证脱敏', 'MASK', 'CONCAT(LEFT({col}, 6), ''********'', RIGHT({col}, 4))', '身份证号脱敏，保留前6后4', '1', '1', 'admin', NOW(), '0'),
('MASK_BANK', '银行卡脱敏', 'MASK', 'CONCAT(LEFT({col}, 4), ''****'', RIGHT({col}, 4))', '银行卡脱敏，保留前4后4', '1', '1', 'admin', NOW(), '0'),
('DELETE_COL', '列删除', 'DELETE', 'NULL', '该列直接输出NULL', '1', '1', 'admin', NOW(), '0');

-- -------------------------------------------------------
-- Part 5: Dprofile_* 数据探查表（Track C）
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS `dprofile_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `task_code` varchar(64) NOT NULL COMMENT '任务编码',
  `task_name` varchar(100) NOT NULL COMMENT '任务名称',
  `task_desc` varchar(500) DEFAULT NULL COMMENT '任务描述',
  `ds_id` bigint NOT NULL COMMENT '关联数据源ID',
  `profile_level` varchar(20) NOT NULL DEFAULT 'DETAILED' COMMENT '探查级别 BASIC/DETAILED/FULL',
  `table_pattern` varchar(500) DEFAULT NULL COMMENT '表名匹配模式 支持通配符',
  `trigger_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式 MANUAL/SCHEDULE/API',
  `cron_expr` varchar(50) DEFAULT NULL COMMENT 'Cron表达式',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/RUNNING/SUCCESS/FAILED/STOPPED',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近运行时间',
  `last_run_status` varchar(20) DEFAULT NULL COMMENT '最近运行状态',
  `total_tables` int DEFAULT 0 COMMENT '涉及表数量',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_code` (`task_code`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据探查任务表';

CREATE TABLE IF NOT EXISTS `dprofile_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '探查时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `task_id` bigint NOT NULL COMMENT '探查任务ID',
  `ds_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `snapshot_id` bigint DEFAULT NULL COMMENT '快照ID',
  `row_count` bigint DEFAULT 0 COMMENT '行数',
  `column_count` int DEFAULT 0 COMMENT '列数',
  `data_size_bytes` bigint DEFAULT 0 COMMENT '数据大小字节',
  `storage_comment` varchar(500) DEFAULT NULL COMMENT '表注释',
  `last_modified` datetime DEFAULT NULL COMMENT '表最后更新时间',
  `profile_data` longtext COMMENT '探查结果 JSON',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_table_name` (`ds_id`, `table_name`),
  KEY `idx_snapshot_id` (`snapshot_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据探查报告表';

CREATE TABLE IF NOT EXISTS `dprofile_column_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `report_id` bigint NOT NULL COMMENT '报告ID',
  `ds_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `column_name` varchar(100) NOT NULL COMMENT '列名',
  `data_type` varchar(50) DEFAULT NULL COMMENT '数据类型',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '列注释',
  `nullable` char(1) DEFAULT 'Y' COMMENT '是否可空',
  `is_primary_key` char(1) DEFAULT 'N' COMMENT '是否主键',
  `total_count` bigint DEFAULT 0 COMMENT '总记录数',
  `null_count` bigint DEFAULT 0 COMMENT '空值数',
  `null_rate` decimal(5,2) DEFAULT 0 COMMENT '空值率',
  `unique_count` bigint DEFAULT 0 COMMENT '唯一值数量',
  `unique_rate` decimal(5,2) DEFAULT 0 COMMENT '唯一值比例',
  `sample_values` text COMMENT '样本值 JSON数组',
  `top_values` text COMMENT '高频值分布 JSON',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_ds_table` (`ds_id`, `table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='列级探查报告表';

CREATE TABLE IF NOT EXISTS `dprofile_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `snapshot_code` varchar(64) NOT NULL COMMENT '快照编码',
  `snapshot_name` varchar(100) NOT NULL COMMENT '快照名称',
  `snapshot_desc` varchar(500) DEFAULT NULL COMMENT '快照描述',
  `ds_id` bigint NOT NULL COMMENT '数据源ID',
  `snapshot_data` longtext COMMENT '快照元数据 JSON',
  `table_count` int DEFAULT 0 COMMENT '包含表数量',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_snapshot_code` (`snapshot_code`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据探查快照表';

-- -------------------------------------------------------
-- Part 6: 索引优化
-- -------------------------------------------------------

-- FluctuationCheck 查询优化：按 rule_id + status 查询
SET @table_exists = (
  SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'dqc_execution_detail'
);
SET @rule_id_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'dqc_execution_detail'
    AND COLUMN_NAME = 'rule_id'
);
SET @status_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'dqc_execution_detail'
    AND COLUMN_NAME = 'status'
);
SET @idx_exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'dqc_execution_detail'
    AND INDEX_NAME = 'idx_rule_status'
);
SET @sql = IF(@table_exists = 1 AND @rule_id_exists = 1 AND @status_exists = 1 AND @idx_exists = 0,
  'CREATE INDEX idx_rule_status ON dqc_execution_detail (rule_id, status)',
  'SELECT ''skip idx_rule_status'' AS result');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
