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
) ENGINE=InnoDB AUTO_INCREMENT=2038857354410303490 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据域';


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
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数仓分层';


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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC执行记录';


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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC执行明细';


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
  `auto_block` char(1) NOT NULL DEFAULT '0' COMMENT '强规则失败是否阻塞',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC质检方案';


-- ry_bigdata_v1.dqc_plan_rule definition

CREATE TABLE `dqc_plan_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `plan_id` bigint NOT NULL COMMENT '方案ID',
  `rule_id` bigint NOT NULL COMMENT '规则ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_rule` (`plan_id`,`rule_id`),
  KEY `idx_rule_id` (`rule_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC方案规则关联';


-- ry_bigdata_v1.dqc_rule_def definition

CREATE TABLE `dqc_rule_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` varchar(20) DEFAULT '000000' COMMENT '租户编号',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `template_id` bigint DEFAULT NULL COMMENT '关联模板ID',
  `rule_type` varchar(50) NOT NULL COMMENT '规则类型',
  `apply_level` varchar(20) NOT NULL COMMENT '应用层级 TABLE/COLUMN/CROSS_FIELD/CROSS_TABLE',
  `dimensions` varchar(200) DEFAULT NULL COMMENT '质量维度，多个逗号分隔',
  `rule_expr` text COMMENT '规则表达式/SQL/正则',
  `target_ds_id` bigint NOT NULL COMMENT '目标数据源ID（引用 sys_datasource）',
  `target_table` varchar(200) DEFAULT NULL COMMENT '目标表名',
  `target_column` varchar(100) DEFAULT NULL COMMENT '目标字段',
  `compare_ds_id` bigint DEFAULT NULL COMMENT '对比数据源ID（跨表规则）',
  `compare_table` varchar(200) DEFAULT NULL COMMENT '对比表名',
  `compare_column` varchar(100) DEFAULT NULL COMMENT '对比字段',
  `threshold_min` decimal(20,4) DEFAULT NULL COMMENT '最小阈值',
  `threshold_max` decimal(20,4) DEFAULT NULL COMMENT '最大阈值',
  `fluctuation_threshold` decimal(5,2) DEFAULT NULL COMMENT '波动阈值百分比',
  `regex_pattern` varchar(500) DEFAULT NULL COMMENT '正则表达式',
  `error_level` varchar(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '错误级别 LOW/MEDIUM/HIGH/CRITICAL',
  `rule_strength` varchar(10) DEFAULT 'WEAK' COMMENT '规则强度：STRONG-强规则/WEAK-弱规则',
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
  KEY `idx_target_ds_id` (`target_ds_id`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC规则定义';


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
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='DQC规则模板';


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
) ENGINE=InnoDB AUTO_INCREMENT=151 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='元数据字段';


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
) ENGINE=InnoDB AUTO_INCREMENT=2038982807267696642 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='元数据扫描记录';


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
) ENGINE=InnoDB AUTO_INCREMENT=2038856913555398693 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='元数据表';


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
) ENGINE=InnoDB AUTO_INCREMENT=2038582379342303234 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据源配置表';