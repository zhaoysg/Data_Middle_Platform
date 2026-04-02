-- ============================================================
-- Metadata DQC / Data Profile core tables
-- 解决增量环境缺失 dqc_plan / dqc_rule_def / dprofile_task 等核心表的问题
-- ============================================================

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
  `layer_code` varchar(20) DEFAULT NULL COMMENT '数据层编码',
  `execution_id` bigint DEFAULT NULL COMMENT '质检执行ID',
  `plan_id` bigint DEFAULT NULL COMMENT '质检方案ID',
  `plan_name` varchar(100) DEFAULT NULL COMMENT '质检方案名称',
  `completeness_score` decimal(5,2) DEFAULT NULL COMMENT '完整性得分',
  `uniqueness_score` decimal(5,2) DEFAULT NULL COMMENT '唯一性得分',
  `accuracy_score` decimal(5,2) DEFAULT NULL COMMENT '准确性得分',
  `consistency_score` decimal(5,2) DEFAULT NULL COMMENT '一致性得分',
  `timeliness_score` decimal(5,2) DEFAULT NULL COMMENT '及时性得分',
  `validity_score` decimal(5,2) DEFAULT NULL COMMENT '有效性得分',
  `overall_score` decimal(5,2) DEFAULT NULL COMMENT '综合得分',
  `rule_pass_rate` decimal(5,2) DEFAULT NULL COMMENT '规则通过率',
  `rule_total_count` int DEFAULT NULL COMMENT '规则总数',
  `rule_pass_count` int DEFAULT NULL COMMENT '规则通过数',
  `rule_fail_count` int DEFAULT NULL COMMENT '规则失败数',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_check_date` (`check_date`),
  KEY `idx_ds_id` (`target_ds_id`),
  KEY `idx_ds_table` (`target_ds_id`, `target_table`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_layer_code` (`layer_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据质量评分表';

CREATE TABLE IF NOT EXISTS `dqc_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `execution_no` varchar(64) NOT NULL COMMENT '执行编号',
  `plan_id` bigint DEFAULT NULL COMMENT '方案ID',
  `plan_name` varchar(100) DEFAULT NULL COMMENT '方案名称',
  `layer_code` varchar(20) DEFAULT NULL COMMENT '质检数据层',
  `trigger_type` varchar(20) NOT NULL COMMENT '触发方式',
  `trigger_user` bigint DEFAULT NULL COMMENT '触发用户',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '耗时',
  `total_rules` int DEFAULT '0' COMMENT '总规则数',
  `passed_count` int DEFAULT '0' COMMENT '通过数',
  `failed_count` int DEFAULT '0' COMMENT '失败数',
  `blocked_count` int DEFAULT '0' COMMENT '阻塞数',
  `overall_score` decimal(5,2) DEFAULT NULL COMMENT '综合得分',
  `status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_execution_no` (`execution_no`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC执行记录';

CREATE TABLE IF NOT EXISTS `dqc_execution_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `execution_id` bigint NOT NULL COMMENT '执行ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `rule_name` varchar(100) DEFAULT NULL COMMENT '规则名称',
  `rule_code` varchar(50) DEFAULT NULL COMMENT '规则编码',
  `rule_type` varchar(50) DEFAULT NULL COMMENT '规则类型',
  `dimension` varchar(50) DEFAULT NULL COMMENT '质量维度',
  `target_ds_id` bigint DEFAULT NULL COMMENT '目标数据源',
  `target_table` varchar(200) DEFAULT NULL COMMENT '目标表',
  `target_column` varchar(100) DEFAULT NULL COMMENT '目标字段',
  `actual_value` text COMMENT '实际执行值',
  `threshold_value` text COMMENT '阈值/期望值',
  `result_value` decimal(20,4) DEFAULT NULL COMMENT '执行结果值',
  `pass_flag` char(1) DEFAULT NULL COMMENT '1=通过 0=失败',
  `error_level` varchar(20) DEFAULT NULL COMMENT '错误级别',
  `error_msg` text COMMENT '错误信息',
  `execute_sql` text COMMENT '实际执行的SQL',
  `execute_time` datetime DEFAULT NULL COMMENT '执行时间',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '耗时',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_pass_flag` (`pass_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC执行明细';

CREATE TABLE IF NOT EXISTS `dqc_rule_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_desc` varchar(500) DEFAULT NULL COMMENT '模板描述',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型',
  `apply_level` varchar(20) NOT NULL COMMENT '应用层级',
  `default_expr` text COMMENT '默认SQL模板',
  `threshold_json` json DEFAULT NULL COMMENT '默认阈值配置',
  `param_spec` json DEFAULT NULL COMMENT '参数规格',
  `dimension` varchar(50) DEFAULT NULL COMMENT '质量维度',
  `builtin` char(1) NOT NULL DEFAULT '1' COMMENT '是否内置',
  `enabled` char(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC规则模板';

CREATE TABLE IF NOT EXISTS `dqc_rule_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `template_id` bigint DEFAULT NULL COMMENT '关联模板ID',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型',
  `apply_level` varchar(20) NOT NULL COMMENT '应用层级',
  `dimensions` varchar(200) DEFAULT NULL COMMENT '质量维度',
  `rule_expr` text COMMENT '规则表达式/SQL/正则',
  `target_ds_id` bigint NOT NULL COMMENT '目标数据源ID',
  `target_table` varchar(200) DEFAULT NULL COMMENT '目标表名',
  `target_column` varchar(100) DEFAULT NULL COMMENT '目标字段',
  `compare_ds_id` bigint DEFAULT NULL COMMENT '对比数据源ID',
  `compare_table` varchar(200) DEFAULT NULL COMMENT '对比表名',
  `compare_column` varchar(100) DEFAULT NULL COMMENT '对比字段',
  `threshold_min` decimal(20,4) DEFAULT NULL COMMENT '最小阈值',
  `threshold_max` decimal(20,4) DEFAULT NULL COMMENT '最大阈值',
  `fluctuation_threshold` decimal(5,2) DEFAULT NULL COMMENT '波动阈值百分比',
  `regex_pattern` varchar(500) DEFAULT NULL COMMENT '正则表达式',
  `error_level` varchar(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '错误级别',
  `alert_receivers` varchar(500) DEFAULT NULL COMMENT '告警接收人',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `enabled` char(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_target_ds_id` (`target_ds_id`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC规则定义';

CREATE TABLE IF NOT EXISTS `dqc_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `plan_name` varchar(100) NOT NULL COMMENT '方案名称',
  `plan_code` varchar(50) NOT NULL COMMENT '方案编码',
  `plan_desc` varchar(500) DEFAULT NULL COMMENT '方案描述',
  `bind_type` varchar(20) DEFAULT 'TABLE' COMMENT '绑定类型 TABLE/DOMAIN/LAYER/PATTERN',
  `bind_value` text COMMENT '绑定值JSON',
  `layer_code` varchar(20) DEFAULT NULL COMMENT '质检数据层',
  `trigger_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式',
  `trigger_cron` varchar(100) DEFAULT NULL COMMENT 'Cron表达式',
  `alert_on_failure` char(1) NOT NULL DEFAULT '1' COMMENT '失败是否告警',
  `auto_block` char(1) NOT NULL DEFAULT '0' COMMENT '强规则失败是否阻塞',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `rule_count` int NOT NULL DEFAULT '0' COMMENT '关联规则数量',
  `table_count` int NOT NULL DEFAULT '0' COMMENT '涉及表数量',
  `last_execution_id` bigint DEFAULT NULL COMMENT '上次执行ID',
  `last_score` decimal(5,2) DEFAULT NULL COMMENT '上次得分',
  `last_execution_time` datetime DEFAULT NULL COMMENT '上次执行时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`),
  KEY `idx_layer_code` (`layer_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC质检方案';

CREATE TABLE IF NOT EXISTS `dqc_plan_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `plan_id` bigint NOT NULL COMMENT '方案ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_rule` (`plan_id`, `rule_id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC方案规则关联';

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
  `profile_level` varchar(20) NOT NULL DEFAULT 'DETAILED' COMMENT '探查级别',
  `table_pattern` varchar(500) DEFAULT NULL COMMENT '表名匹配模式',
  `trigger_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式',
  `cron_expr` varchar(50) DEFAULT NULL COMMENT 'Cron表达式',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近运行时间',
  `last_run_status` varchar(20) DEFAULT NULL COMMENT '最近运行状态',
  `total_tables` int DEFAULT '0' COMMENT '涉及表数量',
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
  `row_count` bigint DEFAULT '0' COMMENT '行数',
  `column_count` int DEFAULT '0' COMMENT '列数',
  `data_size_bytes` bigint DEFAULT '0' COMMENT '数据大小字节',
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
  `total_count` bigint DEFAULT '0' COMMENT '总记录数',
  `null_count` bigint DEFAULT '0' COMMENT '空值数',
  `null_rate` decimal(5,2) DEFAULT '0.00' COMMENT '空值率',
  `unique_count` bigint DEFAULT '0' COMMENT '唯一值数量',
  `unique_rate` decimal(5,2) DEFAULT '0.00' COMMENT '唯一值比例',
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
  `table_count` int DEFAULT '0' COMMENT '包含表数量',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_snapshot_code` (`snapshot_code`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据探查快照表';

-- ============================================================
-- 增量升级：兼容旧版 dqc_rule_def / dqc_plan 结构
-- 目标：已存在旧表时补齐新列，并将旧字段数据迁移到新字段
-- ============================================================

SET @has_dqc_rule_def = (
    SELECT COUNT(1)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'dqc_rule_def'
);

SET @has_rule_apply_level = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'apply_level'
);
SET @has_rule_dimensions = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'dimensions'
);
SET @has_rule_expr = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'rule_expr'
);
SET @has_rule_compare_ds_id = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'compare_ds_id'
);
SET @has_rule_compare_table = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'compare_table'
);
SET @has_rule_compare_column = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'compare_column'
);
SET @has_rule_threshold_min = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'threshold_min'
);
SET @has_rule_threshold_max = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'threshold_max'
);
SET @has_rule_fluctuation_threshold = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'fluctuation_threshold'
);
SET @has_rule_regex_pattern = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'regex_pattern'
);
SET @has_rule_alert_receivers = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'alert_receivers'
);
SET @has_rule_sort_order = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'sort_order'
);
SET @has_rule_legacy_dimension = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'dimension'
);
SET @has_rule_legacy_rule_params = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_rule_def' AND column_name = 'rule_params'
);

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_apply_level = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `apply_level` varchar(20) NOT NULL DEFAULT ''TABLE'' COMMENT ''应用层级'' AFTER `rule_type`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_dimensions = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `dimensions` varchar(200) DEFAULT NULL COMMENT ''质量维度'' AFTER `apply_level`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_expr = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `rule_expr` text COMMENT ''规则表达式/SQL/正则'' AFTER `dimensions`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_compare_ds_id = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `compare_ds_id` bigint DEFAULT NULL COMMENT ''对比数据源ID'' AFTER `target_column`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_compare_table = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `compare_table` varchar(200) DEFAULT NULL COMMENT ''对比表名'' AFTER `compare_ds_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_compare_column = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `compare_column` varchar(100) DEFAULT NULL COMMENT ''对比字段'' AFTER `compare_table`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_threshold_min = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `threshold_min` decimal(20,4) DEFAULT NULL COMMENT ''最小阈值'' AFTER `compare_column`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_threshold_max = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `threshold_max` decimal(20,4) DEFAULT NULL COMMENT ''最大阈值'' AFTER `threshold_min`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_fluctuation_threshold = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `fluctuation_threshold` decimal(5,2) DEFAULT NULL COMMENT ''波动阈值百分比'' AFTER `threshold_max`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_regex_pattern = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `regex_pattern` varchar(500) DEFAULT NULL COMMENT ''正则表达式'' AFTER `fluctuation_threshold`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_alert_receivers = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `alert_receivers` varchar(500) DEFAULT NULL COMMENT ''告警接收人'' AFTER `error_level`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_sort_order = 0,
    'ALTER TABLE `dqc_rule_def` ADD COLUMN `sort_order` int NOT NULL DEFAULT ''0'' COMMENT ''排序'' AFTER `alert_receivers`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_dimensions = 1 AND @has_rule_legacy_dimension = 1,
    'UPDATE `dqc_rule_def` SET `dimensions` = `dimension` WHERE (`dimensions` IS NULL OR `dimensions` = '''') AND `dimension` IS NOT NULL AND `dimension` <> ''''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_apply_level = 1,
    'UPDATE `dqc_rule_def` SET `apply_level` = CASE WHEN `compare_table` IS NOT NULL AND `compare_table` <> '''' THEN ''CROSS_TABLE'' WHEN `compare_column` IS NOT NULL AND `compare_column` <> '''' THEN ''CROSS_FIELD'' WHEN `target_column` IS NOT NULL AND `target_column` <> '''' THEN ''COLUMN'' ELSE ''TABLE'' END WHERE `apply_level` IS NULL OR `apply_level` = ''''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_legacy_rule_params = 1 AND @has_rule_expr = 1,
    'UPDATE `dqc_rule_def` SET `rule_expr` = JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.ruleExpr'')) WHERE (`rule_expr` IS NULL OR `rule_expr` = '''') AND JSON_VALID(`rule_params`) AND JSON_EXTRACT(`rule_params`, ''$.ruleExpr'') IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_legacy_rule_params = 1 AND @has_rule_compare_table = 1,
    'UPDATE `dqc_rule_def` SET `compare_table` = JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.compareTable'')) WHERE (`compare_table` IS NULL OR `compare_table` = '''') AND JSON_VALID(`rule_params`) AND JSON_EXTRACT(`rule_params`, ''$.compareTable'') IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_legacy_rule_params = 1 AND @has_rule_compare_column = 1,
    'UPDATE `dqc_rule_def` SET `compare_column` = JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.compareColumn'')) WHERE (`compare_column` IS NULL OR `compare_column` = '''') AND JSON_VALID(`rule_params`) AND JSON_EXTRACT(`rule_params`, ''$.compareColumn'') IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_legacy_rule_params = 1 AND @has_rule_threshold_min = 1,
    'UPDATE `dqc_rule_def` SET `threshold_min` = CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.thresholdMin'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.threshold_min'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.minValue'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.min_value'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.minThreshold'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.min_threshold''))) AS DECIMAL(20,4)) WHERE `threshold_min` IS NULL AND JSON_VALID(`rule_params`) AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.thresholdMin'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.threshold_min'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.minValue'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.min_value'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.minThreshold'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.min_threshold''))) IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_legacy_rule_params = 1 AND @has_rule_threshold_max = 1,
    'UPDATE `dqc_rule_def` SET `threshold_max` = CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.thresholdMax'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.threshold_max'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.maxValue'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.max_value'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.maxThreshold'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.max_threshold''))) AS DECIMAL(20,4)) WHERE `threshold_max` IS NULL AND JSON_VALID(`rule_params`) AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.thresholdMax'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.threshold_max'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.maxValue'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.max_value'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.maxThreshold'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.max_threshold''))) IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_legacy_rule_params = 1 AND @has_rule_fluctuation_threshold = 1,
    'UPDATE `dqc_rule_def` SET `fluctuation_threshold` = CAST(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.fluctuationThreshold'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.fluctuation_threshold'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.thresholdPct'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.threshold_pct'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.maxFluctuation'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.max_fluctuation''))) AS DECIMAL(5,2)) WHERE `fluctuation_threshold` IS NULL AND JSON_VALID(`rule_params`) AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.fluctuationThreshold'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.fluctuation_threshold'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.thresholdPct'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.threshold_pct'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.maxFluctuation'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.max_fluctuation''))) IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_rule_def = 1 AND @has_rule_legacy_rule_params = 1 AND @has_rule_regex_pattern = 1,
    'UPDATE `dqc_rule_def` SET `regex_pattern` = COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.pattern'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.regex''))) WHERE (`regex_pattern` IS NULL OR `regex_pattern` = '''') AND JSON_VALID(`rule_params`) AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.pattern'')), JSON_UNQUOTE(JSON_EXTRACT(`rule_params`, ''$.regex''))) IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_dqc_plan = (
    SELECT COUNT(1)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'dqc_plan'
);

SET @has_plan_bind_type = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'bind_type'
);
SET @has_plan_bind_value = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'bind_value'
);
SET @has_plan_trigger_cron = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'trigger_cron'
);
SET @has_plan_alert_on_failure = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'alert_on_failure'
);
SET @has_plan_auto_block = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'auto_block'
);
SET @has_plan_rule_count = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'rule_count'
);
SET @has_plan_table_count = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'table_count'
);
SET @has_plan_last_execution_id = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'last_execution_id'
);
SET @has_plan_last_score = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'last_score'
);
SET @has_plan_last_execution_time = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'last_execution_time'
);
SET @has_plan_legacy_ds_id = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'ds_id'
);
SET @has_plan_legacy_table_pattern = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'table_pattern'
);
SET @has_plan_legacy_cron_expr = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'cron_expr'
);
SET @has_plan_legacy_enabled = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'enabled'
);
SET @has_plan_legacy_total_rules = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'dqc_plan' AND column_name = 'total_rules'
);

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_bind_type = 0,
    'ALTER TABLE `dqc_plan` ADD COLUMN `bind_type` varchar(20) DEFAULT ''TABLE'' COMMENT ''绑定类型 TABLE/DOMAIN/LAYER/PATTERN'' AFTER `plan_desc`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_bind_value = 0,
    'ALTER TABLE `dqc_plan` ADD COLUMN `bind_value` text COMMENT ''绑定值JSON'' AFTER `bind_type`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_trigger_cron = 0,
    'ALTER TABLE `dqc_plan` ADD COLUMN `trigger_cron` varchar(100) DEFAULT NULL COMMENT ''触发Cron表达式'' AFTER `trigger_type`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_alert_on_failure = 0,
    'ALTER TABLE `dqc_plan` ADD COLUMN `alert_on_failure` char(1) NOT NULL DEFAULT ''1'' COMMENT ''失败是否告警'' AFTER `trigger_cron`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_auto_block = 0,
    'ALTER TABLE `dqc_plan` ADD COLUMN `auto_block` char(1) NOT NULL DEFAULT ''0'' COMMENT ''强规则失败是否阻塞'' AFTER `alert_on_failure`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_rule_count = 0,
    'ALTER TABLE `dqc_plan` ADD COLUMN `rule_count` int NOT NULL DEFAULT ''0'' COMMENT ''关联规则数量'' AFTER `status`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_table_count = 0,
    'ALTER TABLE `dqc_plan` ADD COLUMN `table_count` int NOT NULL DEFAULT ''0'' COMMENT ''涉及表数量'' AFTER `rule_count`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_last_execution_id = 0,
    'ALTER TABLE `dqc_plan` ADD COLUMN `last_execution_id` bigint DEFAULT NULL COMMENT ''上次执行ID'' AFTER `table_count`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_last_score = 0,
    'ALTER TABLE `dqc_plan` ADD COLUMN `last_score` decimal(5,2) DEFAULT NULL COMMENT ''上次得分'' AFTER `last_execution_id`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_last_execution_time = 0,
    'ALTER TABLE `dqc_plan` ADD COLUMN `last_execution_time` datetime DEFAULT NULL COMMENT ''上次执行时间'' AFTER `last_score`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @bind_type_expr = '''TABLE''';
SET @bind_value_expr = 'NULL';

SET @bind_type_expr = IF(
    @has_plan_legacy_ds_id = 1 AND @has_plan_legacy_table_pattern = 1,
    'CASE WHEN `ds_id` IS NOT NULL AND `table_pattern` IS NOT NULL AND `table_pattern` <> '''' THEN ''PATTERN'' WHEN `ds_id` IS NOT NULL THEN ''TABLE'' ELSE `bind_type` END',
    @bind_type_expr
);
SET @bind_value_expr = IF(
    @has_plan_legacy_ds_id = 1 AND @has_plan_legacy_table_pattern = 1,
    'CASE WHEN `ds_id` IS NOT NULL THEN JSON_OBJECT(''dsId'', `ds_id`, ''tablePattern'', `table_pattern`) ELSE NULL END',
    @bind_value_expr
);
SET @bind_type_expr = IF(
    @has_plan_legacy_ds_id = 1 AND @has_plan_legacy_table_pattern = 0,
    'CASE WHEN `ds_id` IS NOT NULL THEN ''TABLE'' ELSE `bind_type` END',
    @bind_type_expr
);
SET @bind_value_expr = IF(
    @has_plan_legacy_ds_id = 1 AND @has_plan_legacy_table_pattern = 0,
    'CASE WHEN `ds_id` IS NOT NULL THEN JSON_OBJECT(''dsId'', `ds_id`) ELSE NULL END',
    @bind_value_expr
);
SET @bind_type_expr = IF(
    @has_plan_legacy_ds_id = 0 AND @has_plan_legacy_table_pattern = 1,
    'CASE WHEN `table_pattern` IS NOT NULL AND `table_pattern` <> '''' THEN ''PATTERN'' ELSE `bind_type` END',
    @bind_type_expr
);
SET @bind_value_expr = IF(
    @has_plan_legacy_ds_id = 0 AND @has_plan_legacy_table_pattern = 1,
    'CASE WHEN `table_pattern` IS NOT NULL AND `table_pattern` <> '''' THEN JSON_OBJECT(''tablePattern'', `table_pattern`) ELSE NULL END',
    @bind_value_expr
);

SET @ddl = IF(
    @has_dqc_plan = 1 AND (@has_plan_legacy_ds_id = 1 OR @has_plan_legacy_table_pattern = 1),
    CONCAT(
        'UPDATE `dqc_plan` SET ',
        '`bind_type` = CASE WHEN (`bind_type` IS NULL OR `bind_type` = '''' OR `bind_type` IN (''INCLUDE'', ''EXCLUDE'', ''REGEX'')) THEN ',
        @bind_type_expr,
        ' ELSE `bind_type` END, ',
        '`bind_value` = CASE WHEN (`bind_value` IS NULL OR `bind_value` = '''') THEN ',
        @bind_value_expr,
        ' ELSE `bind_value` END'
    ),
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_legacy_cron_expr = 1 AND @has_plan_trigger_cron = 1,
    'UPDATE `dqc_plan` SET `trigger_cron` = `cron_expr` WHERE (`trigger_cron` IS NULL OR `trigger_cron` = '''') AND `cron_expr` IS NOT NULL AND `cron_expr` <> ''''',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
    @has_dqc_plan = 1 AND @has_plan_legacy_total_rules = 1 AND @has_plan_rule_count = 1,
    'UPDATE `dqc_plan` SET `rule_count` = `total_rules` WHERE (`rule_count` IS NULL OR `rule_count` = 0) AND `total_rules` IS NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @plan_status_expr = 'CASE WHEN `status` IN (''RUNNING'', ''SUCCESS'', ''FAILED'', ''STOPPED'') THEN ''PUBLISHED'' WHEN `status` IS NULL OR `status` = '''' THEN ''DRAFT'' ELSE `status` END';
SET @plan_status_where = '`status` IS NULL OR `status` = '''' OR `status` IN (''RUNNING'', ''SUCCESS'', ''FAILED'', ''STOPPED'')';
SET @plan_status_expr = IF(
    @has_plan_legacy_enabled = 1,
    'CASE WHEN `enabled` = ''1'' THEN ''DISABLED'' WHEN `status` IN (''RUNNING'', ''SUCCESS'', ''FAILED'', ''STOPPED'') THEN ''PUBLISHED'' WHEN `status` IS NULL OR `status` = '''' THEN ''DRAFT'' ELSE `status` END',
    @plan_status_expr
);
SET @plan_status_where = IF(
    @has_plan_legacy_enabled = 1,
    CONCAT(@plan_status_where, ' OR `enabled` = ''1'''),
    @plan_status_where
);

SET @ddl = IF(
    @has_dqc_plan = 1,
    CONCAT('UPDATE `dqc_plan` SET `status` = ', @plan_status_expr, ' WHERE ', @plan_status_where),
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
