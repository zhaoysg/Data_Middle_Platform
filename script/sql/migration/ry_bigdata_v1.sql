-- ry_bigdata_v1.data_domain definition

CREATE TABLE `data_domain` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `domain_name` varchar(100) NOT NULL COMMENT '数据域名称',
  `domain_code` varchar(50) DEFAULT NULL COMMENT '数据域编码',
  `domain_desc` varchar(500) DEFAULT NULL COMMENT '数据域描述',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `status` varchar(20) DEFAULT '0' COMMENT '状态 0=正常 1=停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_domain_code` (`domain_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据域';


-- ry_bigdata_v1.data_layer definition

CREATE TABLE `data_layer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `layer_code` varchar(20) NOT NULL COMMENT '分层编码 ODS/DWD/DWS/ADS',
  `layer_name` varchar(50) NOT NULL COMMENT '分层名称',
  `layer_desc` varchar(500) DEFAULT NULL COMMENT '分层描述',
  `layer_color` varchar(20) DEFAULT '#1890ff' COMMENT '颜色标识（前端 Tag 颜色）',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `status` varchar(20) DEFAULT '0' COMMENT '状态 0=正常 1=停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_layer_code` (`tenant_id`,`layer_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2042197177986633762 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数仓分层';


-- ry_bigdata_v1.dprofile_column_report definition

CREATE TABLE `dprofile_column_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `report_id` bigint NOT NULL COMMENT '报告ID',
  `execution_id` varchar(64) DEFAULT NULL COMMENT 'execution id',
  `ds_id` bigint NOT NULL COMMENT '数据源ID',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `column_name` varchar(100) NOT NULL COMMENT '列名',
  `data_type` varchar(50) DEFAULT NULL COMMENT '数据类型',
  `data_type_category` varchar(20) DEFAULT NULL COMMENT 'NUMERIC/STRING/DATETIME/BOOLEAN/OTHER',
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
  `top_value_items` longtext COMMENT 'top value items json',
  `histogram` longtext COMMENT 'histogram json',
  `min_value` varchar(200) DEFAULT NULL COMMENT 'min value',
  `max_value` varchar(200) DEFAULT NULL COMMENT 'max value',
  `avg_value` decimal(20,6) DEFAULT NULL COMMENT 'avg value',
  `median_value` decimal(20,6) DEFAULT NULL COMMENT 'median value',
  `std_dev` decimal(20,6) DEFAULT NULL COMMENT 'std dev',
  `min_length` int DEFAULT NULL COMMENT 'min length',
  `max_length` int DEFAULT NULL COMMENT 'max length',
  `avg_length` decimal(20,6) DEFAULT NULL COMMENT 'avg length',
  `zero_count` bigint DEFAULT NULL COMMENT 'zero count',
  `zero_rate` decimal(10,4) DEFAULT NULL COMMENT 'zero rate',
  `negative_count` bigint DEFAULT NULL COMMENT 'negative count',
  `outlier_count` bigint DEFAULT NULL COMMENT 'outlier count',
  `outlier_rate` decimal(10,4) DEFAULT NULL COMMENT 'outlier rate',
  `outlier_method` varchar(20) DEFAULT NULL COMMENT 'IQR/3SIGMA',
  `outlier_detail` longtext COMMENT 'outlier detail json',
  `warning_messages` longtext COMMENT 'warning messages json',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_ds_table` (`ds_id`,`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='列级探查报告表';


-- ry_bigdata_v1.dprofile_execution definition

CREATE TABLE `dprofile_execution` (
  `execution_id` varchar(64) NOT NULL COMMENT 'execution id',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT 'tenant id',
  `create_dept` bigint DEFAULT NULL COMMENT 'create dept',
  `create_by` bigint DEFAULT NULL COMMENT 'create by',
  `create_time` datetime DEFAULT NULL COMMENT 'create time',
  `update_by` bigint DEFAULT NULL COMMENT 'update by',
  `update_time` datetime DEFAULT NULL COMMENT 'update time',
  `task_id` bigint NOT NULL COMMENT 'task id',
  `trigger_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/SCHEDULE/API',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED/CANCELLED',
  `phase` varchar(30) DEFAULT NULL COMMENT 'PREPARE/TABLE_ANALYSIS/COLUMN_ANALYSIS/ANOMALY_ANALYSIS/COMPLETED/FAILED',
  `progress` int NOT NULL DEFAULT '0' COMMENT '0-100',
  `message` varchar(500) DEFAULT NULL COMMENT 'message',
  `error_message` text COMMENT 'error message',
  `current_table` varchar(200) DEFAULT NULL COMMENT 'current table',
  `total_tables` int NOT NULL DEFAULT '0' COMMENT 'total tables',
  `processed_tables` int NOT NULL DEFAULT '0' COMMENT 'processed tables',
  `start_time` datetime DEFAULT NULL COMMENT 'start time',
  `end_time` datetime DEFAULT NULL COMMENT 'end time',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT 'delete flag',
  PRIMARY KEY (`execution_id`),
  KEY `idx_dprofile_execution_task_id` (`task_id`),
  KEY `idx_dprofile_execution_status` (`status`),
  KEY `idx_dprofile_execution_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='dprofile execution record';


-- ry_bigdata_v1.dprofile_report definition

CREATE TABLE `dprofile_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '探查时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `task_id` bigint NOT NULL COMMENT '探查任务ID',
  `execution_id` varchar(64) DEFAULT NULL COMMENT 'execution id',
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
  KEY `idx_table_name` (`ds_id`,`table_name`),
  KEY `idx_snapshot_id` (`snapshot_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_execution_id` (`execution_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2040008043108941827 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据探查报告表';


-- ry_bigdata_v1.dprofile_snapshot definition

CREATE TABLE `dprofile_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `snapshot_code` varchar(64) NOT NULL COMMENT '快照编码',
  `snapshot_name` varchar(100) NOT NULL COMMENT '快照名称',
  `snapshot_desc` varchar(500) DEFAULT NULL COMMENT '快照描述',
  `ds_id` bigint NOT NULL COMMENT '数据源ID',
  `report_id` bigint DEFAULT NULL COMMENT 'report id',
  `table_name` varchar(200) DEFAULT NULL COMMENT 'table name',
  `row_count` bigint DEFAULT NULL COMMENT 'row count',
  `column_count` int DEFAULT NULL COMMENT 'column count',
  `snapshot_type` varchar(20) NOT NULL DEFAULT 'LEGACY' COMMENT 'TABLE/LEGACY',
  `legacy_source_id` bigint DEFAULT NULL COMMENT 'legacy source snapshot id',
  `snapshot_data` longtext COMMENT '快照元数据 JSON',
  `table_count` int DEFAULT '0' COMMENT '包含表数量',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_snapshot_code` (`snapshot_code`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_ds_table_snapshot` (`ds_id`,`table_name`,`snapshot_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据探查快照表';


-- ry_bigdata_v1.dprofile_task definition

CREATE TABLE `dprofile_task` (
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
  `target_columns` varchar(4000) DEFAULT NULL COMMENT '指定探查的列名（逗号分隔，留空表示全部列）',
  `selected_columns` text COMMENT 'selected columns json',
  `trigger_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式 MANUAL/SCHEDULE/API',
  `cron_expr` varchar(50) DEFAULT NULL COMMENT 'Cron表达式',
  `execution_timeout_minutes` int DEFAULT NULL COMMENT '执行超时分钟数，空则使用全局配置',
  `enabled` char(1) NOT NULL DEFAULT '1' COMMENT 'enabled',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/RUNNING/SUCCESS/FAILED/STOPPED',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近运行时间',
  `last_run_status` varchar(20) DEFAULT NULL COMMENT '最近运行状态',
  `total_tables` int DEFAULT '0' COMMENT '涉及表数量',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_code` (`task_code`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2040008030152736770 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据探查任务表';


-- ry_bigdata_v1.dqc_execution definition

CREATE TABLE `dqc_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `execution_no` varchar(64) NOT NULL COMMENT '执行编号',
  `plan_id` bigint DEFAULT NULL COMMENT '方案ID',
  `plan_name` varchar(100) DEFAULT NULL COMMENT '方案名称（冗余）',
  `layer_code` varchar(20) DEFAULT NULL COMMENT '质检数据层',
  `trigger_type` varchar(20) NOT NULL COMMENT '触发方式 MANUAL/SCHEDULE/API',
  `trigger_user` bigint DEFAULT NULL COMMENT '触发用户',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '耗时',
  `total_rules` int DEFAULT '0' COMMENT '总规则数',
  `passed_count` int DEFAULT '0' COMMENT '通过数',
  `failed_count` int DEFAULT '0' COMMENT '失败数',
  `blocked_count` int DEFAULT '0' COMMENT '阻塞数',
  `overall_score` decimal(5,2) DEFAULT NULL COMMENT '综合得分',
  `status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态 RUNNING/SUCCESS/FAILED/PARTIAL',
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC执行记录';


-- ry_bigdata_v1.dqc_execution_detail definition

CREATE TABLE `dqc_execution_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `execution_id` bigint NOT NULL COMMENT '执行ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `rule_name` varchar(100) DEFAULT NULL COMMENT '规则名称（冗余）',
  `rule_code` varchar(50) DEFAULT NULL COMMENT '规则编码（冗余）',
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
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC执行明细';


-- ry_bigdata_v1.dqc_plan definition

CREATE TABLE `dqc_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `plan_name` varchar(100) NOT NULL COMMENT '方案名称',
  `plan_code` varchar(50) NOT NULL COMMENT '方案编码',
  `plan_desc` varchar(500) DEFAULT NULL COMMENT '方案描述',
  `bind_type` varchar(20) DEFAULT 'TABLE' COMMENT '绑定类型 TABLE/DOMAIN/LAYER/PATTERN',
  `bind_value` text COMMENT '绑定值JSON',
  `layer_code` varchar(20) DEFAULT NULL COMMENT '质检数据层（引用 data_layer.layer_code）',
  `trigger_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式 MANUAL/SCHEDULE/API',
  `trigger_cron` varchar(100) DEFAULT NULL COMMENT 'Cron表达式',
  `alert_on_failure` char(1) NOT NULL DEFAULT '1' COMMENT '失败是否告警',
  `alert_on_success` char(1) NOT NULL DEFAULT '0' COMMENT '成功是否通知',
  `auto_block` char(1) NOT NULL DEFAULT '0' COMMENT '强规则失败是否阻塞',
  `sensitivity_level` varchar(20) DEFAULT NULL COMMENT '敏感等级 L1/L2/L3/L4',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PUBLISHED/DISABLED',
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
) ENGINE=InnoDB AUTO_INCREMENT=2042791210711658498 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC质检方案';


-- ry_bigdata_v1.dqc_plan_rule definition

CREATE TABLE `dqc_plan_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `plan_id` bigint NOT NULL COMMENT '方案ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `target_table` varchar(200) DEFAULT NULL COMMENT '目标表名',
  `target_column` varchar(100) DEFAULT NULL COMMENT '目标字段',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `skip_on_failure` char(1) DEFAULT '0' COMMENT '失败是否跳过 0=否 1=是',
  `enabled` char(1) NOT NULL DEFAULT '1' COMMENT '是否启用 0=否 1=是',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_rule` (`plan_id`,`rule_id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC方案规则关联';


-- ry_bigdata_v1.dqc_quality_score definition

CREATE TABLE `dqc_quality_score` (
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
  KEY `idx_ds_table` (`target_ds_id`,`target_table`),
  KEY `idx_plan_id` (`plan_id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_layer_code` (`layer_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据质量评分表';


-- ry_bigdata_v1.dqc_rule_def definition

CREATE TABLE `dqc_rule_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `template_id` bigint DEFAULT NULL COMMENT '关联模板ID',
  `table_id` bigint DEFAULT NULL COMMENT '关联元数据表ID (metadata_table.id)',
  `column_id` bigint DEFAULT NULL COMMENT '关联元数据字段ID (metadata_column.id)',
  `compare_table_id` bigint DEFAULT NULL COMMENT '对比表元数据ID (metadata_table.id)',
  `compare_column_id` bigint DEFAULT NULL COMMENT '对比字段元数据ID (metadata_column.id)',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型',
  `apply_level` varchar(20) NOT NULL COMMENT '应用层级 TABLE/COLUMN/CROSS_FIELD/CROSS_TABLE',
  `dimensions` varchar(200) DEFAULT NULL COMMENT '质量维度，多个逗号分隔',
  `rule_expr` text COMMENT '规则表达式/SQL/正则',
  `threshold_min` decimal(20,4) DEFAULT NULL COMMENT '最小阈值',
  `threshold_max` decimal(20,4) DEFAULT NULL COMMENT '最大阈值',
  `fluctuation_threshold` decimal(5,2) DEFAULT NULL COMMENT '波动阈值百分比',
  `regex_pattern` varchar(500) DEFAULT NULL COMMENT '正则表达式',
  `error_level` varchar(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '错误级别 LOW/MEDIUM/HIGH/CRITICAL',
  `rule_strength` varchar(20) DEFAULT 'WEAK' COMMENT '规则强度：STRONG-强规则 / WEAK-弱规则',
  `alert_receivers` varchar(500) DEFAULT NULL COMMENT '告警接收人，多个逗号分隔',
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
  KEY `idx_enabled` (`enabled`),
  KEY `idx_rule_def_table_id` (`table_id`),
  KEY `idx_rule_def_column_id` (`column_id`),
  KEY `idx_rule_def_compare_table_id` (`compare_table_id`),
  KEY `idx_rule_def_compare_column_id` (`compare_column_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2042533459455565827 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC规则定义';


-- ry_bigdata_v1.dqc_rule_template definition

CREATE TABLE `dqc_rule_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `template_code` varchar(50) NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `template_desc` varchar(500) DEFAULT NULL COMMENT '模板描述',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型',
  `apply_level` varchar(20) NOT NULL COMMENT '应用层级 TABLE/COLUMN/CROSS_FIELD/CROSS_TABLE',
  `default_expr` text COMMENT '默认SQL模板',
  `threshold_json` json DEFAULT NULL COMMENT '默认阈值配置',
  `param_spec` json DEFAULT NULL COMMENT '参数规格',
  `dimension` varchar(50) DEFAULT NULL COMMENT '质量维度',
  `builtin` char(1) NOT NULL DEFAULT '1' COMMENT '是否内置 1=内置',
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
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC规则模板';


-- ry_bigdata_v1.gov_glossary_category definition

CREATE TABLE `gov_glossary_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `category_name` varchar(100) NOT NULL COMMENT '分类名称',
  `category_code` varchar(50) DEFAULT NULL COMMENT '分类编码',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` varchar(20) DEFAULT '0' COMMENT '状态 0=正常 1=停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_category_code` (`category_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='术语分类';


-- ry_bigdata_v1.gov_glossary_mapping definition

CREATE TABLE `gov_glossary_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `term_id` bigint NOT NULL COMMENT '术语ID',
  `term_name` varchar(200) DEFAULT NULL COMMENT '术语名称（冗余）',
  `asset_type` varchar(20) NOT NULL COMMENT '资产类型 TABLE/COLUMN',
  `asset_id` bigint NOT NULL COMMENT 'metadata_table/column.id',
  `ds_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `table_name` varchar(200) DEFAULT NULL COMMENT '表名（冗余）',
  `column_name` varchar(100) DEFAULT NULL COMMENT '字段名（冗余）',
  `mapping_type` varchar(20) DEFAULT 'CONTAINS' COMMENT '关联类型 CONTAINS/DEFINES/REFERENCES',
  `confidence` int NOT NULL DEFAULT '100' COMMENT '置信度 0-100',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mapping` (`term_id`,`asset_type`,`asset_id`),
  KEY `idx_term_id` (`term_id`),
  KEY `idx_asset` (`asset_type`,`asset_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='术语资产映射';


-- ry_bigdata_v1.gov_glossary_term definition

CREATE TABLE `gov_glossary_term` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `term_name` varchar(200) NOT NULL COMMENT '术语名称（如：订单）',
  `term_alias` varchar(500) DEFAULT NULL COMMENT '别名，多个逗号分隔',
  `term_desc` varchar(1000) DEFAULT NULL COMMENT '术语定义/业务说明',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `biz_owner` bigint DEFAULT NULL COMMENT '业务负责人ID',
  `tech_owner` bigint DEFAULT NULL COMMENT '技术负责人ID',
  `status` varchar(20) DEFAULT 'DRAFT' COMMENT '状态 DRAFT/PUBLISHED/DEPRECATED',
  `tag_ids` varchar(500) DEFAULT NULL COMMENT '关联标签ID',
  `source` varchar(100) DEFAULT NULL COMMENT '来源',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_term_name` (`term_name`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务术语';


-- ry_bigdata_v1.gov_lineage definition

CREATE TABLE `gov_lineage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `src_ds_id` bigint NOT NULL COMMENT '源数据源ID',
  `src_ds_name` varchar(100) DEFAULT NULL COMMENT '源数据源名称（冗余）',
  `src_table_name` varchar(200) NOT NULL COMMENT '源表名',
  `src_column_name` varchar(100) DEFAULT NULL COMMENT '源字段（null=整表）',
  `tgt_ds_id` bigint NOT NULL COMMENT '目标数据源ID',
  `tgt_ds_name` varchar(100) DEFAULT NULL COMMENT '目标数据源名称（冗余）',
  `tgt_table_name` varchar(200) NOT NULL COMMENT '目标表名',
  `tgt_column_name` varchar(100) DEFAULT NULL COMMENT '目标字段',
  `lineage_type` varchar(20) NOT NULL COMMENT '血缘类型 DIRECT/DERIVED',
  `transform_type` varchar(20) DEFAULT 'ETL' COMMENT '转换类型 ETL/SQL/STREAMING/COPY',
  `transform_sql` text COMMENT '转换SQL片段',
  `biz_description` varchar(500) DEFAULT NULL COMMENT '业务说明',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `verify_status` varchar(20) DEFAULT 'UNVERIFIED' COMMENT '核验状态 UNVERIFIED/VERIFIED/INVALID',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lineage_pair` (`src_ds_id`,`src_table_name`,`src_column_name`,`tgt_ds_id`,`tgt_table_name`,`tgt_column_name`),
  KEY `idx_src` (`src_ds_id`,`src_table_name`),
  KEY `idx_tgt` (`tgt_ds_id`,`tgt_table_name`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据血缘';


-- ry_bigdata_v1.metadata_catalog definition

CREATE TABLE `metadata_catalog` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `catalog_name` varchar(100) NOT NULL COMMENT '目录名称',
  `catalog_code` varchar(50) DEFAULT NULL COMMENT '目录编码',
  `catalog_type` varchar(20) NOT NULL COMMENT '目录类型 BUSINESS_DOMAIN/DATA_DOMAIN/ALBUM',
  `parent_id` bigint DEFAULT '0' COMMENT '父目录ID（0=顶级）',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `status` varchar(20) DEFAULT '0' COMMENT '状态 0=正常 1=停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_catalog_code` (`catalog_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2038948407532380162 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产目录';


-- ry_bigdata_v1.metadata_column definition

CREATE TABLE `metadata_column` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `table_id` bigint NOT NULL COMMENT '关联 metadata_table.id',
  `ds_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `table_name` varchar(200) DEFAULT NULL COMMENT '表名（冗余存储）',
  `column_name` varchar(100) NOT NULL COMMENT '字段名',
  `column_alias` varchar(200) DEFAULT NULL COMMENT '字段中文别名（可编辑）',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '字段注释',
  `data_type` varchar(50) DEFAULT NULL COMMENT '数据类型',
  `is_nullable` varchar(10) DEFAULT 'YES' COMMENT '是否可空',
  `column_key` varchar(10) DEFAULT NULL COMMENT '键类型 PRI/UNI/MUL',
  `default_value` varchar(500) DEFAULT NULL COMMENT '默认值',
  `is_primary_key` tinyint(1) DEFAULT '0' COMMENT '是否主键',
  `is_foreign_key` tinyint(1) DEFAULT '0' COMMENT '是否外键',
  `fk_reference` varchar(200) DEFAULT NULL COMMENT '外键引用（格式: table.column）',
  `is_sensitive` tinyint(1) DEFAULT '0' COMMENT '是否敏感字段',
  `sensitivity_level` varchar(20) DEFAULT 'NORMAL' COMMENT '敏感等级',
  `sort_order` int DEFAULT '0' COMMENT '字段排序序号',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_table_column` (`tenant_id`,`ds_id`,`table_name`,`column_name`),
  KEY `idx_table_id` (`table_id`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=313 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='元数据字段';


-- ry_bigdata_v1.metadata_scan_log definition

CREATE TABLE `metadata_scan_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `ds_id` bigint NOT NULL COMMENT '数据源ID',
  `ds_name` varchar(100) DEFAULT NULL COMMENT '数据源名称',
  `ds_code` varchar(50) DEFAULT NULL COMMENT '数据源编码',
  `scan_type` varchar(20) DEFAULT 'FULL' COMMENT '扫描类型 TABLE_ONLY/FULL',
  `status` varchar(20) DEFAULT 'RUNNING' COMMENT '状态 RUNNING/SUCCESS/FAILED/PARTIAL',
  `total_tables` int DEFAULT '0' COMMENT '总表数',
  `success_count` int DEFAULT '0' COMMENT '成功数',
  `partial_count` int DEFAULT '0' COMMENT '部分成功数（字段级失败）',
  `failed_count` int DEFAULT '0' COMMENT '失败数',
  `error_detail` text COMMENT '错误详情（格式：表名:错误信息；表名:错误信息）',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `elapsed_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `scan_user_id` bigint DEFAULT NULL COMMENT '扫描触发用户ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `sensitive_matched` int DEFAULT NULL COMMENT '下游处理：敏感识别匹配数',
  `lineage_discovered` int DEFAULT NULL COMMENT '下游处理：血缘关系发现数',
  `dqc_binded` int DEFAULT NULL COMMENT '下游处理：DQC绑定数',
  `mask_applied` int DEFAULT NULL COMMENT '下游处理：脱敏应用数',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_status` (`status`),
  KEY `idx_scan_time` (`start_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2042188975245058050 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='元数据扫描记录';


-- ry_bigdata_v1.metadata_scan_schedule definition

CREATE TABLE `metadata_scan_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `ds_id` bigint NOT NULL COMMENT '数据源ID',
  `ds_name` varchar(100) DEFAULT NULL COMMENT '数据源名称（冗余）',
  `schedule_type` varchar(20) NOT NULL DEFAULT 'MANUAL' COMMENT '调度类型 MANUAL/CRON/EVENT',
  `cron_expr` varchar(50) DEFAULT NULL COMMENT 'Cron表达式',
  `enabled` char(1) NOT NULL DEFAULT '1' COMMENT '是否启用 1=是',
  `sync_column` char(1) NOT NULL DEFAULT '1' COMMENT '是否同步字段 1=是',
  `last_scan_time` datetime DEFAULT NULL COMMENT '上次扫描时间',
  `next_scan_time` datetime DEFAULT NULL COMMENT '下次扫描时间',
  `change_alert` char(1) NOT NULL DEFAULT '1' COMMENT '变更是否告警',
  `alert_targets` varchar(500) DEFAULT NULL COMMENT '告警接收人，多个逗号分隔',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_schedule_type` (`schedule_type`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='扫描调度配置';


-- ry_bigdata_v1.metadata_schema_history definition

CREATE TABLE `metadata_schema_history` (
  `script_name` varchar(255) NOT NULL COMMENT 'script file name',
  `executed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'execution time',
  PRIMARY KEY (`script_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='metadata init history';


-- ry_bigdata_v1.metadata_table definition

CREATE TABLE `metadata_table` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `ds_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `ds_name` varchar(100) DEFAULT NULL COMMENT '数据源名称',
  `ds_code` varchar(50) DEFAULT NULL COMMENT '数据源编码',
  `table_name` varchar(200) NOT NULL COMMENT '物理表名',
  `table_alias` varchar(200) DEFAULT NULL COMMENT '表中文别名（可编辑）',
  `table_comment` varchar(1000) DEFAULT NULL COMMENT '表注释',
  `table_type` varchar(30) DEFAULT 'TABLE' COMMENT '表类型 TABLE/VIEW',
  `data_layer` varchar(20) DEFAULT NULL COMMENT '数仓分层 ODS/DWD/DWS/ADS',
  `data_domain` varchar(50) DEFAULT NULL COMMENT '数据域',
  `row_count` bigint DEFAULT NULL COMMENT '行数',
  `storage_bytes` bigint DEFAULT NULL COMMENT '存储大小估算',
  `source_update_time` datetime DEFAULT NULL COMMENT '源表最后更新时间',
  `sensitivity_level` varchar(20) DEFAULT 'NORMAL' COMMENT '敏感等级 NORMAL/INNER/SENSITIVE/HIGHLY_SENSITIVE',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `catalog_id` bigint DEFAULT NULL COMMENT '资产目录ID',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签',
  `last_scan_time` datetime DEFAULT NULL COMMENT '最后扫描时间',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/ARCHIVED/DEPRECATED',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_table` (`tenant_id`,`ds_id`,`table_name`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_data_layer` (`data_layer`),
  KEY `idx_data_domain` (`data_domain`),
  KEY `idx_status` (`status`),
  KEY `idx_catalog_id` (`catalog_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_last_scan_time` (`last_scan_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2038856913555398716 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='元数据表';


-- ry_bigdata_v1.metadata_table_history definition

CREATE TABLE `metadata_table_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `table_id` bigint NOT NULL COMMENT 'metadata_table.id',
  `ds_id` bigint DEFAULT NULL COMMENT '数据源ID',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `snapshot_time` datetime NOT NULL COMMENT '快照时间',
  `row_count` bigint DEFAULT NULL COMMENT '当时行数',
  `column_count` int DEFAULT NULL COMMENT '当时字段数',
  `column_hash` varchar(64) DEFAULT NULL COMMENT '字段名MD5（检测字段变更）',
  `change_type` varchar(20) DEFAULT NULL COMMENT '变更类型 ADD/DROP/MODIFY/UNCHANGED',
  `change_detail` text COMMENT '变更详情JSON',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_table_id` (`table_id`),
  KEY `idx_snapshot_time` (`snapshot_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表变更历史';


-- ry_bigdata_v1.sec_access_log definition

CREATE TABLE `sec_access_log` (
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
  `result_rows` int DEFAULT '0' COMMENT '返回行数',
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


-- ry_bigdata_v1.sec_class_level_binding definition

CREATE TABLE `sec_class_level_binding` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT 'tenant id',
  `create_dept` bigint DEFAULT NULL COMMENT 'create dept',
  `create_by` bigint DEFAULT NULL COMMENT 'create by',
  `create_time` datetime DEFAULT NULL COMMENT 'create time',
  `update_by` bigint DEFAULT NULL COMMENT 'update by',
  `update_time` datetime DEFAULT NULL COMMENT 'update time',
  `class_code` varchar(64) NOT NULL COMMENT 'classification code',
  `level_code` varchar(64) NOT NULL COMMENT 'level code',
  `sort_order` int DEFAULT '0' COMMENT 'sort order',
  `enabled` char(1) NOT NULL DEFAULT '1' COMMENT 'enabled',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT 'delete flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sec_class_level_binding` (`class_code`,`level_code`,`del_flag`),
  KEY `idx_sec_class_level_binding_class` (`class_code`),
  KEY `idx_sec_class_level_binding_level` (`level_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2039970303076139011 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='classification level binding';


-- ry_bigdata_v1.sec_classification definition

CREATE TABLE `sec_classification` (
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
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `default_level_code` varchar(50) DEFAULT NULL COMMENT '默认关联敏感等级编码（关联 sec_level.level_code）',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用 0否 1是',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_class_code` (`class_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据分类表';


-- ry_bigdata_v1.sec_column_sensitivity definition

CREATE TABLE `sec_column_sensitivity` (
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
  `column_comment` varchar(500) DEFAULT NULL COMMENT '字段注释/备注',
  `data_type` varchar(50) DEFAULT NULL COMMENT '数据类型',
  `confidence` decimal(5,2) DEFAULT NULL COMMENT '识别置信度 0-100',
  `level_code` varchar(50) DEFAULT NULL COMMENT '敏感等级',
  `class_code` varchar(50) DEFAULT NULL COMMENT '数据分类',
  `match_rule_id` bigint DEFAULT NULL COMMENT '命中的识别规则ID',
  `mask_type` varchar(32) DEFAULT NULL COMMENT '建议脱敏类型 MASK/HIDE/ENCRYPT等',
  `mask_pattern` varchar(255) DEFAULT NULL COMMENT '建议脱敏模式/表达式',
  `identified_by` varchar(50) DEFAULT 'AUTO' COMMENT '识别方式 AUTO/MANUAL',
  `scan_task_id` bigint DEFAULT NULL COMMENT '扫描任务ID',
  `scan_batch_no` varchar(64) DEFAULT NULL COMMENT '最近扫描批次号',
  `scan_time` datetime DEFAULT NULL COMMENT '最近扫描时间',
  `confirmed` char(1) DEFAULT '0' COMMENT '是否确认 0待确认 1已确认',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_table_column` (`ds_id`,`table_name`,`column_name`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_level_code` (`level_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2039554329046847491 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字段敏感记录表';


-- ry_bigdata_v1.sec_level definition

CREATE TABLE `sec_level` (
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
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_code` (`level_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感等级表';


-- ry_bigdata_v1.sec_mask_strategy definition

CREATE TABLE `sec_mask_strategy` (
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
  `scene_type` varchar(32) DEFAULT NULL COMMENT 'scene type',
  `level_mask_mapping` longtext COMMENT 'level mask mapping json',
  `priority` int DEFAULT '100' COMMENT 'priority',
  `conflict_check` char(1) NOT NULL DEFAULT '1' COMMENT 'conflict check',
  `status` varchar(32) DEFAULT 'DRAFT' COMMENT 'status',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_code` (`strategy_code`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2039555775100260355 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脱敏策略表';


-- ry_bigdata_v1.sec_mask_strategy_detail definition

CREATE TABLE `sec_mask_strategy_detail` (
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


-- ry_bigdata_v1.sec_mask_template definition

CREATE TABLE `sec_mask_template` (
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
  `data_type` varchar(32) DEFAULT NULL COMMENT 'data type',
  `mask_type` varchar(32) DEFAULT NULL COMMENT 'mask type',
  `mask_position` varchar(32) DEFAULT NULL COMMENT 'mask position',
  `mask_char` varchar(32) DEFAULT NULL COMMENT 'mask char',
  `mask_head_keep` int DEFAULT NULL COMMENT 'mask head keep',
  `mask_tail_keep` int DEFAULT NULL COMMENT 'mask tail keep',
  `mask_pattern` varchar(255) DEFAULT NULL COMMENT 'mask pattern',
  `sort_order` int DEFAULT '0' COMMENT 'sort order',
  `mask_expr` varchar(500) NOT NULL COMMENT '脱敏表达式 如 MASK(name, ''*'', 3, 4)',
  `template_desc` varchar(500) DEFAULT NULL COMMENT '模板描述',
  `builtin` char(1) DEFAULT '0' COMMENT '是否内置',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='脱敏模板表';


-- ry_bigdata_v1.sec_sensitivity_rule definition

CREATE TABLE `sec_sensitivity_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `description` varchar(500) DEFAULT NULL COMMENT 'description',
  `rule_type` varchar(50) NOT NULL COMMENT '识别类型 REGEX/COLUMN_NAME/DATA_TYPE/CUSTOM',
  `match_type` varchar(32) DEFAULT NULL COMMENT 'match type',
  `match_expr` longtext COMMENT 'match expr',
  `match_expr_type` varchar(32) DEFAULT NULL COMMENT 'match expr type',
  `rule_expr` varchar(500) DEFAULT NULL COMMENT '匹配表达式 JSON格式',
  `target_level_code` varchar(50) DEFAULT NULL COMMENT '关联的敏感等级',
  `target_class_code` varchar(50) DEFAULT NULL COMMENT '关联的数据分类',
  `suggestion_action` varchar(32) DEFAULT NULL COMMENT 'suggestion action',
  `suggestion_mask_pattern` varchar(255) DEFAULT NULL COMMENT 'suggestion mask pattern',
  `suggestion_mask_type` varchar(32) DEFAULT NULL COMMENT 'suggestion mask type',
  `priority` int DEFAULT '100' COMMENT 'priority',
  `enabled` char(1) DEFAULT '1' COMMENT '是否启用',
  `builtin` char(1) DEFAULT '0' COMMENT '是否内置规则 0否 1是',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='敏感识别规则表';


-- ry_bigdata_v1.sys_datasource definition

CREATE TABLE `sys_datasource` (
  `ds_id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `ds_name` varchar(100) NOT NULL COMMENT '数据源名称',
  `ds_code` varchar(50) NOT NULL COMMENT '数据源编码',
  `ds_type` varchar(30) NOT NULL COMMENT '数据源类型：MYSQL/SQLSERVER/ORACLE/TIDB/POSTGRESQL',
  `host` varchar(255) DEFAULT NULL COMMENT '主机地址',
  `port` int DEFAULT NULL COMMENT '端口',
  `database_name` varchar(100) DEFAULT NULL COMMENT '数据库名',
  `schema_name` varchar(100) DEFAULT NULL COMMENT 'Schema名称(用于PostgreSQL)',
  `username` varchar(100) DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) DEFAULT NULL COMMENT '密码',
  `connection_params` text COMMENT '额外连接参数JSON',
  `data_layer` varchar(20) DEFAULT NULL COMMENT '数仓层标记：ODS/DWD/DWS/ADS',
  `data_source` varchar(32) DEFAULT NULL COMMENT '数据来源：K3DC/K3HW/K1/K2/OTHER',
  `ds_flag` char(1) DEFAULT '0' COMMENT '数据源标识（0内部 1外部）',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` bigint DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`ds_id`),
  UNIQUE KEY `uk_ds_code` (`ds_code`),
  KEY `idx_ds_type` (`ds_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2038582379342303235 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据源配置表';

-- =============================================
-- 修复 dqc_execution_detail 缺失审计字段
-- DqcExecutionDetail 实体继承 BaseEntity，期望有 create_time/update_time 字段
-- 但 ry_bigdata_v1.sql 中 dqc_execution_detail 表定义缺少这两个字段
-- =============================================
ALTER TABLE `dqc_execution_detail`
  ADD COLUMN `create_dept` bigint DEFAULT NULL COMMENT '创建部门' AFTER `del_flag`,
  ADD COLUMN `create_by` varchar(64) DEFAULT '' COMMENT '创建者' AFTER `create_dept`,
  ADD COLUMN `create_time` datetime DEFAULT NULL COMMENT '创建时间' AFTER `create_by`,
  ADD COLUMN `update_by` varchar(64) DEFAULT '' COMMENT '更新者' AFTER `create_time`,
  ADD COLUMN `update_time` datetime DEFAULT NULL COMMENT '更新时间' AFTER `update_by`;
