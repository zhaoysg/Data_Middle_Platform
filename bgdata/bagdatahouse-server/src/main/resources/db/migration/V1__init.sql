-- ============================================================
-- 数据中台工具 - V1__init.sql
-- 包含：完整 DDL（46张表）+ 内置初始数据 + ALTER补丁 + 菜单结构修复
-- 版本: 1.0.0 | 日期: 2026-03-24
--
-- 说明:
--   本文件为唯一迁移脚本，执行顺序如下：
--     1. CREATE TABLE（全部46张表）
--     2. 内置初始数据（分层、模板、管理员、分级）
--     3. ALTER 补丁（新增字段）
--     4. 菜单结构修复（修复 parent_id + path/component）
--
--   test_data.sql 为独立测试数据文件（不含 DDL），可单独导入或重复执行
-- ============================================================

-- ----------------------------
-- 1. 系统管理（7张）
-- ----------------------------

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `dept_id` BIGINT COMMENT '所属部门',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0-未删除，1-已删除',
    `last_login_ip` VARCHAR(50) COMMENT '最后登录IP',
    `last_login_time` DATETIME COMMENT '最后登录时间',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 部门/业务域表
CREATE TABLE IF NOT EXISTS `sys_dept` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID',
    `dept_name` VARCHAR(50) NOT NULL COMMENT '部门名称',
    `dept_code` VARCHAR(50) NOT NULL COMMENT '部门编码',
    `dept_type` VARCHAR(20) DEFAULT 'DEPT' COMMENT '类型：DEPT-部门，BUSINESS_DOMAIN-业务域',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `leader_id` BIGINT COMMENT '负责人ID',
    `phone` VARCHAR(20) COMMENT '联系电话',
    `email` VARCHAR(100) COMMENT '邮箱',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_code` (`dept_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门/业务域表';

-- 菜单权限表
CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
    `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
    `menu_code` VARCHAR(50) COMMENT '菜单编码',
    `menu_type` VARCHAR(20) NOT NULL COMMENT '类型：CATALOG-目录，MENU-菜单，BUTTON-按钮',
    `path` VARCHAR(200) COMMENT '路由路径',
    `component` VARCHAR(255) COMMENT '组件路径',
    `icon` VARCHAR(100) COMMENT '图标',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏，1-显示',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `perms` VARCHAR(100) COMMENT '权限标识',
    `cached` TINYINT NOT NULL DEFAULT 0 COMMENT '是否缓存：0-否，1-是',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_menu_code` (`menu_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单权限表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
    `role_type` VARCHAR(20) DEFAULT 'CUSTOM' COMMENT '角色类型：CUSTOM-自定义，SYSTEM-系统角色',
    `data_scope` VARCHAR(50) DEFAULT 'CUSTOM' COMMENT '数据权限：CUSTOM-自定义，DEPT-本部门，ALL-全部',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';


-- ----------------------------
-- 2. 数据源管理（3张）
-- ----------------------------

-- 数据源配置表
CREATE TABLE IF NOT EXISTS `dq_datasource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据源ID',
    `ds_name` VARCHAR(100) NOT NULL COMMENT '数据源名称',
    `ds_code` VARCHAR(50) NOT NULL COMMENT '数据源编码',
    `ds_type` VARCHAR(30) NOT NULL COMMENT '数据源类型：MYSQL/SQLSERVER/ORACLE/TIDB/POSTGRESQL',
    `host` VARCHAR(255) COMMENT '主机地址',
    `port` INT COMMENT '端口',
    `database_name` VARCHAR(100) COMMENT '数据库名',
    `schema_name` VARCHAR(100) DEFAULT NULL COMMENT 'Schema名称(用于PostgreSQL等，可选)',
    `username` VARCHAR(100) COMMENT '用户名',
    `password` VARCHAR(255) COMMENT '密码（AES加密存储）',
    `connection_params` TEXT COMMENT '额外连接参数JSON',
    `data_layer` VARCHAR(20) COMMENT '数仓层标记：ODS/DWD/DWS/ADS',
    `dept_id` BIGINT COMMENT '所属业务域',
    `owner_id` BIGINT COMMENT '负责人',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `last_test_time` DATETIME COMMENT '最后测试连接时间',
    `last_test_result` VARCHAR(50) COMMENT '最后测试结果：SUCCESS/FAILED',
    `remark` VARCHAR(500) COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ds_code` (`ds_code`),
    KEY `idx_ds_type` (`ds_type`),
    KEY `idx_status` (`status`),
    KEY `idx_data_layer` (`data_layer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源配置表';

-- 数仓分层配置表
CREATE TABLE IF NOT EXISTS `dq_data_layer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `layer_code` VARCHAR(20) NOT NULL COMMENT '分层编码：ODS/DWD/DWS/ADS',
    `layer_name` VARCHAR(50) NOT NULL COMMENT '分层名称',
    `layer_desc` VARCHAR(200) COMMENT '分层描述',
    `layer_color` VARCHAR(20) DEFAULT '#1677FF' COMMENT '展示颜色',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_layer_code` (`layer_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数仓分层配置表';

-- 数据域配置表
CREATE TABLE IF NOT EXISTS `dq_data_domain` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据域ID',
    `domain_code` VARCHAR(50) NOT NULL COMMENT '数据域编码',
    `domain_name` VARCHAR(100) NOT NULL COMMENT '数据域名称',
    `domain_desc` VARCHAR(500) COMMENT '数据域描述',
    `dept_id` BIGINT COMMENT '所属部门',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_domain_code` (`domain_code`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据域配置表';


-- ----------------------------
-- 3. 数据质量（7张）
-- ----------------------------

-- 规则模板表
CREATE TABLE IF NOT EXISTS `dqc_rule_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_desc` VARCHAR(500) COMMENT '模板描述',
    `rule_type` VARCHAR(50) NOT NULL COMMENT '规则类型：NULL_CHECK/UNIQUE/REGEX/THRESHOLD/SQL/FLUCTUATION/CUSTOM_FUNC',
    `apply_level` VARCHAR(20) NOT NULL COMMENT '适用级别：DATABASE/TABLE/COLUMN/CROSS_FIELD/CROSS_TABLE',
    `default_expr` TEXT COMMENT '默认表达式模板',
    `default_threshold` JSON COMMENT '默认阈值配置',
    `param_spec` JSON COMMENT '参数规格说明',
    `dimension` VARCHAR(50) COMMENT '所属质量维度',
    `builtin` TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置：0-自定义，1-内置',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY `idx_rule_type` (`rule_type`),
    KEY `idx_apply_level` (`apply_level`),
    KEY `idx_builtin` (`builtin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则模板表';

-- 规则定义表
CREATE TABLE IF NOT EXISTS `dqc_rule_def` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码',
    `template_id` BIGINT COMMENT '关联模板ID',
    `rule_type` VARCHAR(50) NOT NULL COMMENT '规则类型',
    `apply_level` VARCHAR(20) NOT NULL COMMENT '适用级别：DATABASE/TABLE/COLUMN/CROSS_FIELD/CROSS_TABLE',
    `dimensions` VARCHAR(200) COMMENT '质量维度，多个逗号分隔',
    `rule_expr` TEXT COMMENT '规则表达式/SQL/正则',
    `target_ds_id` BIGINT COMMENT '目标数据源ID',
    `target_table` VARCHAR(200) COMMENT '目标表名',
    `target_column` VARCHAR(100) COMMENT '目标列名',
    `compare_ds_id` BIGINT COMMENT '对比数据源ID（跨表规则）',
    `compare_table` VARCHAR(200) COMMENT '对比表名',
    `compare_column` VARCHAR(100) COMMENT '对比列名',
    `threshold_min` DECIMAL(20,4) COMMENT '最小阈值',
    `threshold_max` DECIMAL(20,4) COMMENT '最大阈值',
    `fluctuation_threshold` DECIMAL(5,2) COMMENT '波动阈值百分比',
    `regex_pattern` VARCHAR(500) COMMENT '正则表达式',
    `error_level` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '错误级别：LOW/MEDIUM/HIGH/CRITICAL',
    `rule_strength` VARCHAR(10) NOT NULL DEFAULT 'WEAK' COMMENT '规则强度：STRONG-强规则/WEAK-弱规则',
    `alert_receivers` VARCHAR(500) COMMENT '告警接收人',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `dept_id` BIGINT COMMENT '所属部门',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_code` (`rule_code`),
    KEY `idx_rule_type` (`rule_type`),
    KEY `idx_target` (`target_ds_id`, `target_table`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_ds_id` (`target_ds_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则定义表';

-- 质检方案表
CREATE TABLE IF NOT EXISTS `dqc_plan` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '方案ID',
    `plan_name` VARCHAR(100) NOT NULL COMMENT '方案名称',
    `plan_code` VARCHAR(50) NOT NULL COMMENT '方案编码',
    `plan_desc` VARCHAR(500) COMMENT '方案描述',
    `bind_type` VARCHAR(20) COMMENT '绑定类型：TABLE-按表/DOMAIN-按数据域/LAYER-按数据层/PATTERN-按模式',
    `bind_value` TEXT COMMENT '绑定值JSON',
    `layer_code` VARCHAR(20) COMMENT '质检数据层：ODS/DWD/DWS/ADS',
    `dept_id` BIGINT COMMENT '所属部门',
    `trigger_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式：MANUAL-手动/SCHEDULE-定时/API-API触发',
    `trigger_cron` VARCHAR(100) COMMENT '定时Cron表达式',
    `alert_on_success` TINYINT NOT NULL DEFAULT 0 COMMENT '成功是否告警：0-否，1-是',
    `alert_on_failure` TINYINT NOT NULL DEFAULT 1 COMMENT '失败是否告警：0-否，1-是',
    `auto_block` TINYINT NOT NULL DEFAULT 1 COMMENT '强规则失败是否阻塞：0-否，1-是',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿/PUBLISHED-已发布/DISABLED-已禁用',
    `rule_count` INT NOT NULL DEFAULT 0 COMMENT '关联规则数量',
    `table_count` INT NOT NULL DEFAULT 0 COMMENT '涉及表数量',
    `last_execution_id` BIGINT COMMENT '最后执行记录ID',
    `last_execution_time` DATETIME COMMENT '最后执行时间',
    `last_execution_score` DECIMAL(5,2) COMMENT '最后执行得分',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_code` (`plan_code`),
    KEY `idx_status` (`status`),
    KEY `idx_trigger_type` (`trigger_type`),
    KEY `idx_layer_code` (`layer_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质检方案表';

-- 方案-规则关联表
CREATE TABLE IF NOT EXISTS `dqc_plan_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plan_id` BIGINT NOT NULL COMMENT '方案ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `rule_order` INT NOT NULL DEFAULT 0 COMMENT '规则执行顺序',
    `custom_threshold` JSON COMMENT '自定义阈值，覆盖规则原阈值',
    `skip_on_failure` TINYINT NOT NULL DEFAULT 0 COMMENT '失败时是否跳过：0-否，1-是',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_plan_id` (`plan_id`),
    KEY `idx_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='方案-规则关联表';

-- 质检执行记录表
CREATE TABLE IF NOT EXISTS `dqc_execution` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '执行记录ID',
    `execution_no` VARCHAR(64) NOT NULL COMMENT '执行编号',
    `plan_id` BIGINT COMMENT '方案ID',
    `plan_name` VARCHAR(100) COMMENT '方案名称',
    `plan_code` VARCHAR(50) COMMENT '方案编码',
    `layer_code` VARCHAR(20) COMMENT '质检数据层',
    `dept_id` BIGINT COMMENT '执行部门',
    `trigger_type` VARCHAR(20) NOT NULL COMMENT '触发方式：MANUAL-手动/SCHEDULE-定时/API-API触发',
    `trigger_user` BIGINT COMMENT '触发用户',
    `trigger_params` JSON COMMENT '触发参数',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `elapsed_ms` BIGINT COMMENT '执行耗时（毫秒）',
    `status` VARCHAR(20) NOT NULL COMMENT '状态：RUNNING-执行中/SUCCESS-成功/FAILED-失败/SKIPPED-跳过/BLOCKED-阻塞',
    `total_rules` INT COMMENT '总规则数',
    `passed_rules` INT COMMENT '通过规则数',
    `failed_rules` INT COMMENT '失败规则数',
    `skipped_rules` INT COMMENT '跳过规则数',
    `total_tables` INT COMMENT '质检表数量',
    `quality_score` DECIMAL(5,2) COMMENT '质量得分（0-100）',
    `score_breakdown` JSON COMMENT '得分明细',
    `dimension_scores` JSON COMMENT '各维度得分',
    `error_detail` TEXT COMMENT '错误详情',
    `log_path` VARCHAR(500) COMMENT '执行日志路径',
    `blocked` TINYINT NOT NULL DEFAULT 0 COMMENT '是否被强规则阻塞：0-否，1-是',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_execution_no` (`execution_no`),
    KEY `idx_plan_id` (`plan_id`),
    KEY `idx_status` (`status`),
    KEY `idx_trigger_type` (`trigger_type`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_layer_code` (`layer_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质检执行记录表';

-- 质检执行明细表（每条规则的执行结果）
CREATE TABLE IF NOT EXISTS `dqc_execution_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `execution_id` BIGINT NOT NULL COMMENT '执行记录ID',
    `execution_no` VARCHAR(64) NOT NULL COMMENT '执行编号',
    `rule_id` BIGINT COMMENT '规则ID',
    `rule_name` VARCHAR(100) COMMENT '规则名称',
    `rule_code` VARCHAR(50) COMMENT '规则编码',
    `rule_type` VARCHAR(50) COMMENT '规则类型',
    `rule_strength` VARCHAR(10) COMMENT '规则强度',
    `dimensions` VARCHAR(200) COMMENT '质量维度',
    `target_ds_id` BIGINT COMMENT '目标数据源ID',
    `target_table` VARCHAR(200) COMMENT '目标表名',
    `target_column` VARCHAR(100) COMMENT '目标列名',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `elapsed_ms` BIGINT COMMENT '耗时（毫秒）',
    `status` VARCHAR(20) NOT NULL COMMENT '状态：SUCCESS-成功/FAILED-失败/SKIPPED-跳过',
    `total_count` BIGINT COMMENT '总记录数',
    `error_count` BIGINT COMMENT '错误记录数',
    `pass_count` BIGINT COMMENT '通过记录数',
    `actual_value` DECIMAL(20,6) COMMENT '实际值',
    `threshold_min` DECIMAL(20,4) COMMENT '最小阈值',
    `threshold_max` DECIMAL(20,4) COMMENT '最大阈值',
    `quality_score` DECIMAL(5,2) COMMENT '该规则得分',
    `error_detail` TEXT COMMENT '错误详情',
    `sql_content` TEXT COMMENT '实际执行的SQL',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_execution_id` (`execution_id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_status` (`status`),
    KEY `idx_target` (`target_ds_id`, `target_table`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质检执行明细表';

-- 质量评分历史表
CREATE TABLE IF NOT EXISTS `dqc_quality_score` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `check_date` DATE NOT NULL COMMENT '统计日期',
    `target_ds_id` BIGINT COMMENT '数据源ID',
    `target_table` VARCHAR(200) COMMENT '表名',
    `layer_code` VARCHAR(20) COMMENT '数据层：ODS/DWD/DWS/ADS',
    `dept_id` BIGINT COMMENT '所属部门',
    `completeness_score` DECIMAL(5,2) COMMENT '完整性得分',
    `uniqueness_score` DECIMAL(5,2) COMMENT '唯一性得分',
    `accuracy_score` DECIMAL(5,2) COMMENT '准确性得分',
    `consistency_score` DECIMAL(5,2) COMMENT '一致性得分',
    `timeliness_score` DECIMAL(5,2) COMMENT '及时性得分',
    `validity_score` DECIMAL(5,2) COMMENT '有效性得分',
    `overall_score` DECIMAL(5,2) COMMENT '综合得分',
    `rule_pass_rate` DECIMAL(5,2) COMMENT '规则通过率',
    `rule_total_count` INT COMMENT '质检规则总数',
    `rule_pass_count` INT COMMENT '通过规则数',
    `rule_fail_count` INT COMMENT '失败规则数',
    `execution_id` BIGINT COMMENT '关联执行记录ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_check_date_table` (`check_date`, `target_ds_id`, `target_table`),
    KEY `idx_ds_id` (`target_ds_id`),
    KEY `idx_layer_code` (`layer_code`),
    KEY `idx_check_date` (`check_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质量评分历史表';


-- ----------------------------
-- 4. 数据探查（6张）
-- ----------------------------

-- 探查任务表
CREATE TABLE IF NOT EXISTS `dprofile_profile_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `task_code` VARCHAR(50) NOT NULL COMMENT '任务编码',
    `task_desc` VARCHAR(500) COMMENT '任务描述',
    `target_ds_id` BIGINT NOT NULL COMMENT '目标数据源ID',
    `target_table` VARCHAR(200) NOT NULL COMMENT '目标表名',
    `target_columns` TEXT COMMENT '指定列，NULL=全部列',
    `target_schema` VARCHAR(100) COMMENT '目标schema（PostgreSQL专用）',
    `profile_level` VARCHAR(20) NOT NULL DEFAULT 'TABLE_AND_COLUMN' COMMENT '探查级别：TABLE_ONLY-仅表级/TABLE_AND_COLUMN-表+列级',
    `trigger_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式：MANUAL-手动/SCHEDULE-定时',
    `trigger_cron` VARCHAR(100) COMMENT '定时Cron表达式',
    `timeout_minutes` INT NOT NULL DEFAULT 60 COMMENT '超时时间（分钟）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿/PUBLISHED-已发布/DISABLED-已禁用',
    `last_execution_id` BIGINT COMMENT '最后执行记录ID',
    `last_execution_time` DATETIME COMMENT '最后执行时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_code` (`task_code`),
    KEY `idx_target` (`target_ds_id`, `target_table`),
    KEY `idx_status` (`status`),
    KEY `idx_trigger_type` (`trigger_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='探查任务表';

-- 表级统计结果表
CREATE TABLE IF NOT EXISTS `dprofile_table_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` BIGINT COMMENT '探查任务ID',
    `execution_id` BIGINT COMMENT '执行记录ID',
    `ds_id` BIGINT NOT NULL COMMENT '数据源ID',
    `table_name` VARCHAR(200) NOT NULL COMMENT '表名',
    `profile_time` DATETIME NOT NULL COMMENT '探查时间',
    `row_count` BIGINT COMMENT '行数',
    `column_count` INT COMMENT '列数',
    `storage_bytes` BIGINT COMMENT '存储大小（字节）',
    `table_comment` VARCHAR(500) COMMENT '表注释',
    `update_time` DATETIME COMMENT '数据更新时间',
    `increment_rows` BIGINT COMMENT '增量行数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_table_time` (`ds_id`, `table_name`, `profile_time`),
    KEY `idx_ds_id` (`ds_id`),
    KEY `idx_profile_time` (`profile_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表级统计结果表';

-- 列级统计结果表
CREATE TABLE IF NOT EXISTS `dprofile_column_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `table_stats_id` BIGINT COMMENT '表统计ID',
    `execution_id` BIGINT COMMENT '执行记录ID',
    `ds_id` BIGINT NOT NULL COMMENT '数据源ID',
    `table_name` VARCHAR(200) NOT NULL COMMENT '表名',
    `column_name` VARCHAR(100) NOT NULL COMMENT '列名',
    `profile_time` DATETIME NOT NULL COMMENT '探查时间',
    `data_type` VARCHAR(50) COMMENT '数据类型',
    `nullable` TINYINT COMMENT '是否可空：0-否，1-是',
    `total_count` BIGINT COMMENT '总记录数',
    `null_count` BIGINT COMMENT '空值数',
    `null_rate` DECIMAL(10,4) COMMENT '空值率',
    `unique_count` BIGINT COMMENT '唯一值数',
    `unique_rate` DECIMAL(10,4) COMMENT '唯一值比例',
    `min_value` VARCHAR(200) COMMENT '最小值',
    `max_value` VARCHAR(200) COMMENT '最大值',
    `avg_value` DECIMAL(20,6) COMMENT '平均值',
    `median_value` VARCHAR(200) COMMENT '中位数',
    `std_dev` DECIMAL(20,6) COMMENT '标准差',
    `min_length` INT COMMENT '最小长度',
    `max_length` INT COMMENT '最大长度',
    `avg_length` DECIMAL(10,2) COMMENT '平均长度',
    `top_values` JSON COMMENT 'Top-N高频值',
    `histogram` JSON COMMENT '直方图数据',
    `outlier_count` INT COMMENT '异常值数量',
    `zero_count` BIGINT COMMENT '零值数量',
    `zero_rate` DECIMAL(10,4) COMMENT '零值比例',
    `negative_count` BIGINT COMMENT '负值数量',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_column_time` (`ds_id`, `table_name`, `column_name`, `profile_time`),
    KEY `idx_ds_table` (`ds_id`, `table_name`),
    KEY `idx_profile_time` (`profile_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='列级统计结果表';

-- 探查快照表
CREATE TABLE IF NOT EXISTS `dprofile_snapshot` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '快照ID',
    `snapshot_name` VARCHAR(100) NOT NULL COMMENT '快照名称',
    `snapshot_code` VARCHAR(50) NOT NULL COMMENT '快照编码',
    `target_ds_id` BIGINT NOT NULL COMMENT '数据源ID',
    `target_table` VARCHAR(200) NOT NULL COMMENT '表名',
    `snapshot_desc` VARCHAR(500) COMMENT '快照描述',
    `table_stats_id` BIGINT COMMENT '表统计记录ID',
    `column_count` INT COMMENT '列统计记录数',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_snapshot_code` (`snapshot_code`),
    KEY `idx_target` (`target_ds_id`, `target_table`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='探查快照表';

-- 快照比对结果表
CREATE TABLE IF NOT EXISTS `dprofile_compare_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '比对结果ID',
    `compare_name` VARCHAR(100) NOT NULL COMMENT '比对名称',
    `compare_code` VARCHAR(50) NOT NULL COMMENT '比对编码',
    `snapshot_a_id` BIGINT NOT NULL COMMENT '快照A ID',
    `snapshot_b_id` BIGINT NOT NULL COMMENT '快照B ID',
    `compare_type` VARCHAR(20) COMMENT '比对类型',
    `compare_result` JSON COMMENT '比对结果JSON',
    `row_count_change` BIGINT COMMENT '行数变化',
    `column_changes` JSON COMMENT '列变化明细',
    `schema_changes` JSON COMMENT '结构变化',
    `diff_count` INT COMMENT '差异数量',
    `diff_detail` TEXT COMMENT '差异详情',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_compare_code` (`compare_code`),
    KEY `idx_snapshot_a` (`snapshot_a_id`),
    KEY `idx_snapshot_b` (`snapshot_b_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='快照比对结果表';


-- ----------------------------
-- 5. 数据监控（5张）
-- ----------------------------

-- 监控指标定义表
CREATE TABLE IF NOT EXISTS `monitor_metric_def` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '指标ID',
    `metric_code` VARCHAR(50) NOT NULL COMMENT '指标编码',
    `metric_name` VARCHAR(100) NOT NULL COMMENT '指标名称',
    `metric_desc` VARCHAR(500) COMMENT '指标描述',
    `metric_type` VARCHAR(30) NOT NULL COMMENT '指标类型：TASK-任务指标/METRIC-业务指标/SYSTEM-系统指标/QUALITY-质量指标',
    `unit` VARCHAR(20) COMMENT '单位',
    `data_type` VARCHAR(20) COMMENT '数据类型：INT/BIGINT/DECIMAL/VARCHAR',
    `query_expr` TEXT COMMENT '采集查询表达式',
    `aggregation_type` VARCHAR(20) COMMENT '聚合类型：SUM/AVG/MAX/MIN/COUNT',
    `target_ds_id` BIGINT COMMENT '关联数据源',
    `dept_id` BIGINT COMMENT '所属部门',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_metric_code` (`metric_code`),
    KEY `idx_metric_type` (`metric_type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='监控指标定义表';

-- 监控指标时序数据表
CREATE TABLE IF NOT EXISTS `monitor_metric_data` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `metric_code` VARCHAR(50) NOT NULL COMMENT '指标编码',
    `metric_value` DECIMAL(20,6) NOT NULL COMMENT '指标值',
    `metric_time` DATETIME NOT NULL COMMENT '指标时间',
    `tags` JSON COMMENT '标签JSON',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_metric_time` (`metric_code`, `metric_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='监控指标时序数据表';

-- 任务执行记录表
CREATE TABLE IF NOT EXISTS `monitor_task_execution` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '执行记录ID',
    `task_id` BIGINT COMMENT '任务ID',
    `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
    `task_type` VARCHAR(30) COMMENT '任务类型',
    `ds_id` BIGINT COMMENT '数据源ID',
    `trigger_type` VARCHAR(20) COMMENT '触发方式',
    `trigger_user` BIGINT COMMENT '触发用户',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `elapsed_ms` BIGINT COMMENT '耗时（毫秒）',
    `status` VARCHAR(20) COMMENT '状态：RUNNING-运行中/SUCCESS-成功/FAILED-失败/TIMEOUT-超时',
    `progress` INT COMMENT '进度（0-100）',
    `log_content` TEXT COMMENT '执行日志',
    `error_msg` TEXT COMMENT '错误信息',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行记录表';

-- 告警规则表（告警基础设施）
CREATE TABLE IF NOT EXISTS `monitor_alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '告警规则ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码',
    `rule_type` VARCHAR(30) NOT NULL COMMENT '规则类型：THRESHOLD-阈值/FLUCTUATION-波动/SYSTEM-系统/QUALITY-质量',
    `target_type` VARCHAR(30) COMMENT '目标类型',
    `target_id` VARCHAR(100) COMMENT '目标ID',
    `condition_type` VARCHAR(20) COMMENT '条件类型：GT-大于/LT-小于/EQ-等于/BETWEEN-区间/FLUCTUATION-波动',
    `threshold_value` DECIMAL(20,6) COMMENT '阈值',
    `threshold_max_value` DECIMAL(20,6) COMMENT '最大阈值',
    `fluctuation_pct` DECIMAL(5,2) COMMENT '波动百分比',
    `comparison_type` VARCHAR(20) COMMENT '对比类型：D_DAY-日环比/W_WEEK-周环比/M_MONTH-月环比',
    `consecutive_triggers` INT NOT NULL DEFAULT 1 COMMENT '连续触发次数',
    `alert_level` VARCHAR(20) NOT NULL DEFAULT 'WARN' COMMENT '告警级别：INFO-通知/WARN-警告/ERROR-错误/CRITICAL-严重',
    `alert_receivers` VARCHAR(500) COMMENT '告警接收人',
    `alert_channels` VARCHAR(200) COMMENT '告警渠道（预留）',
    `alert_title_template` VARCHAR(200) COMMENT '告警标题模板',
    `alert_content_template` TEXT COMMENT '告警内容模板',
    `mute_until` DATETIME COMMENT '静默截止时间',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `dept_id` BIGINT COMMENT '所属部门',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_code` (`rule_code`),
    KEY `idx_rule_type` (`rule_type`),
    KEY `idx_alert_level` (`alert_level`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则表';

-- 告警记录表
CREATE TABLE IF NOT EXISTS `monitor_alert_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '告警记录ID',
    `alert_no` VARCHAR(64) NOT NULL COMMENT '告警编号',
    `rule_id` BIGINT COMMENT '关联规则ID',
    `rule_name` VARCHAR(100) COMMENT '规则名称',
    `rule_code` VARCHAR(50) COMMENT '规则编码',
    `alert_level` VARCHAR(20) COMMENT '告警级别',
    `alert_title` VARCHAR(200) NOT NULL COMMENT '告警标题',
    `alert_content` TEXT COMMENT '告警内容',
    `target_type` VARCHAR(30) COMMENT '目标类型',
    `target_id` VARCHAR(100) COMMENT '目标ID',
    `target_name` VARCHAR(200) COMMENT '目标名称',
    `trigger_value` DECIMAL(20,6) COMMENT '触发值',
    `threshold_value` DECIMAL(20,6) COMMENT '阈值',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待发送/SENT-已发送/READ-已读/RESOLVED-已解决',
    `sent_channels` VARCHAR(200) COMMENT '已发送渠道',
    `sent_time` DATETIME COMMENT '发送时间',
    `read_time` DATETIME COMMENT '阅读时间',
    `read_user` BIGINT COMMENT '阅读用户',
    `resolved_time` DATETIME COMMENT '解决时间',
    `resolve_user` BIGINT COMMENT '解决用户',
    `resolve_comment` VARCHAR(500) COMMENT '解决说明',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_alert_no` (`alert_no`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_status` (`status`),
    KEY `idx_alert_level` (`alert_level`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警记录表';


-- ----------------------------
-- 6. 数据治理（4张）
-- ----------------------------

-- 元数据表
CREATE TABLE IF NOT EXISTS `gov_metadata` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '元数据ID',
    `ds_id` BIGINT NOT NULL COMMENT '数据源ID',
    `table_name` VARCHAR(200) NOT NULL COMMENT '表名',
    `table_alias` VARCHAR(200) COMMENT '表中文名',
    `table_comment` VARCHAR(1000) COMMENT '表注释',
    `table_type` VARCHAR(30) NOT NULL DEFAULT 'TABLE' COMMENT '表类型：TABLE-表/VIEW-视图',
    `data_layer` VARCHAR(20) COMMENT '数据层：ODS/DWD/DWS/ADS',
    `data_domain` VARCHAR(50) COMMENT '数据域',
    `biz_domain` VARCHAR(50) COMMENT '业务域',
    `lifecycle_days` INT COMMENT '生命周期（天）',
    `is_partitioned` TINYINT NOT NULL DEFAULT 0 COMMENT '是否分区表：0-否，1-是',
    `partition_column` VARCHAR(100) COMMENT '分区字段',
    `storage_bytes` BIGINT COMMENT '存储大小',
    `row_count` BIGINT COMMENT '行数',
    `access_freq` INT NOT NULL DEFAULT 0 COMMENT '访问频率',
    `sensitivity_level` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '敏感等级：NORMAL-普通/INNER-内部/SENSITIVE-敏感/HIGHLY_SENSITIVE-高度敏感',
    `owner_id` BIGINT COMMENT '负责人',
    `dept_id` BIGINT COMMENT '所属部门',
    `tags` VARCHAR(500) COMMENT '标签，逗号分隔',
    `last_profiled_at` DATETIME COMMENT '最后探查时间',
    `last_modified_at` DATETIME COMMENT '最后修改时间',
    `last_accessed_at` DATETIME COMMENT '最后访问时间',
    `etl_source` VARCHAR(200) COMMENT 'ETL来源',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-活跃/ARCHIVED-归档/DEPRECATED-废弃',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ds_table` (`ds_id`, `table_name`),
    KEY `idx_data_layer` (`data_layer`),
    KEY `idx_data_domain` (`data_domain`),
    KEY `idx_sensitivity` (`sensitivity_level`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='元数据表';

-- 元数据字段表
CREATE TABLE IF NOT EXISTS `gov_metadata_column` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '字段ID',
    `metadata_id` BIGINT NOT NULL COMMENT '元数据ID',
    `column_name` VARCHAR(100) NOT NULL COMMENT '字段名',
    `column_alias` VARCHAR(200) COMMENT '字段中文名',
    `column_comment` VARCHAR(500) COMMENT '字段注释',
    `data_type` VARCHAR(50) NOT NULL COMMENT '数据类型',
    `is_nullable` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可空：0-否，1-是',
    `column_key` VARCHAR(10) COMMENT '键类型：PRI-主键/UNI-唯一键/MUL-普通索引',
    `default_value` VARCHAR(200) COMMENT '默认值',
    `is_primary_key` TINYINT NOT NULL DEFAULT 0 COMMENT '是否主键：0-否，1-是',
    `is_foreign_key` TINYINT NOT NULL DEFAULT 0 COMMENT '是否外键：0-否，1-是',
    `fk_reference` VARCHAR(200) COMMENT '外键引用',
    `is_sensitive` TINYINT NOT NULL DEFAULT 0 COMMENT '是否敏感：0-否，1-是',
    `sensitivity_level` VARCHAR(20) DEFAULT 'NORMAL' COMMENT '敏感等级',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_metadata_column` (`metadata_id`, `column_name`),
    KEY `idx_metadata_id` (`metadata_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='元数据字段表';

-- 数据血缘表（支持表级+字段级完整血缘）
CREATE TABLE IF NOT EXISTS `gov_lineage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '血缘ID',
    `lineage_type` VARCHAR(20) NOT NULL COMMENT '血缘类型：TABLE-表级血缘/COLUMN-字段级血缘',
    `source_ds_id` BIGINT NOT NULL COMMENT '源数据源ID',
    `source_table` VARCHAR(200) NOT NULL COMMENT '源表名',
    `source_column` VARCHAR(100) COMMENT '源字段名',
    `source_column_alias` VARCHAR(200) COMMENT '源字段中文名',
    `target_ds_id` BIGINT NOT NULL COMMENT '目标数据源ID',
    `target_table` VARCHAR(200) NOT NULL COMMENT '目标表名',
    `target_column` VARCHAR(100) COMMENT '目标字段名',
    `target_column_alias` VARCHAR(200) COMMENT '目标字段中文名',
    `transform_type` VARCHAR(50) COMMENT '转换类型：DIRECT/SUM/AVG/COUNT/MAX/MIN/CONCAT/CASE_WHEN/CUSTOM_EXPR',
    `transform_expr` TEXT COMMENT '转换表达式描述',
    `job_id` BIGINT COMMENT '来源作业ID（Kettle接入后填充）',
    `job_name` VARCHAR(200) COMMENT '来源作业名称',
    `lineage_source` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '血缘来源：MANUAL-手动录入/AUTO_PARSER-自动解析',
    `dept_id` BIGINT COMMENT '所属部门',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-活跃/DEPRECATED-废弃',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_source` (`source_ds_id`, `source_table`),
    KEY `idx_target` (`target_ds_id`, `target_table`),
    KEY `idx_lineage_type` (`lineage_type`),
    KEY `idx_lineage_source` (`lineage_source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据血缘表';

-- 数据资产目录表
CREATE TABLE IF NOT EXISTS `gov_asset_catalog` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '目录ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父目录ID',
    `catalog_type` VARCHAR(20) NOT NULL COMMENT '目录类型：BUSINESS_DOMAIN-业务域/DATA_DOMAIN-数据域/ALBUM-数据专辑',
    `catalog_name` VARCHAR(100) NOT NULL COMMENT '目录名称',
    `catalog_code` VARCHAR(50) COMMENT '目录编码',
    `catalog_desc` VARCHAR(500) COMMENT '目录描述',
    `cover_image` VARCHAR(255) COMMENT '专辑封面图片',
    `item_count` INT NOT NULL DEFAULT 0 COMMENT '包含资产数量',
    `access_count` INT NOT NULL DEFAULT 0 COMMENT '访问次数',
    `owner_id` BIGINT COMMENT '负责人',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见：0-隐藏，1-显示',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_catalog_code` (`catalog_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据资产目录表';

-- 资产目录元数据关联表
CREATE TABLE IF NOT EXISTS `gov_catalog_metadata` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `catalog_id` BIGINT NOT NULL COMMENT '目录ID',
    `metadata_id` BIGINT COMMENT '元数据ID',
    `ds_id` BIGINT COMMENT '数据源ID',
    `ds_name` VARCHAR(100) COMMENT '数据源名称',
    `ds_type` VARCHAR(30) COMMENT '数据源类型',
    `table_name` VARCHAR(200) COMMENT '表名',
    `table_alias` VARCHAR(200) COMMENT '表中文名',
    `data_layer` VARCHAR(20) COMMENT '数据层',
    `biz_domain` VARCHAR(50) COMMENT '业务域',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_catalog_id` (`catalog_id`),
    KEY `idx_metadata_id` (`metadata_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产目录元数据关联表';

-- 术语分类表
CREATE TABLE IF NOT EXISTS `gov_glossary_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID',
    `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `category_code` VARCHAR(50) COMMENT '分类编码',
    `category_desc` VARCHAR(500) COMMENT '分类描述',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='术语分类表';

-- 术语表
CREATE TABLE IF NOT EXISTS `gov_glossary_term` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '术语ID',
    `term_code` VARCHAR(50) NOT NULL COMMENT '术语编码',
    `term_name` VARCHAR(100) NOT NULL COMMENT '术语名称',
    `term_name_en` VARCHAR(100) COMMENT '英文名称',
    `term_alias` VARCHAR(200) COMMENT '别名，逗号分隔',
    `category_id` BIGINT COMMENT '所属分类ID',
    `biz_domain` VARCHAR(50) COMMENT '业务域',
    `definition` TEXT COMMENT '术语定义',
    `formula` TEXT COMMENT '计算公式',
    `data_type` VARCHAR(50) COMMENT '数据类型',
    `unit` VARCHAR(20) COMMENT '单位',
    `example_value` VARCHAR(200) COMMENT '示例值',
    `sensitivity_level` VARCHAR(20) DEFAULT 'INTERNAL' COMMENT '敏感等级',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿/PUBLISHED-已发布',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `published_time` DATETIME COMMENT '发布时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_term_code` (`term_code`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='术语表';

-- 术语-字段映射表
CREATE TABLE IF NOT EXISTS `gov_glossary_mapping` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '映射ID',
    `term_id` BIGINT NOT NULL COMMENT '术语ID',
    `ds_id` BIGINT COMMENT '数据源ID',
    `table_name` VARCHAR(200) COMMENT '表名',
    `column_name` VARCHAR(100) COMMENT '字段名',
    `mapping_type` VARCHAR(20) NOT NULL DEFAULT 'DIRECT' COMMENT '映射类型：DIRECT-直接映射/AGGREGATE-聚合映射',
    `mapping_desc` VARCHAR(500) COMMENT '映射说明',
    `confidence` DECIMAL(5,2) COMMENT '置信度',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待审核/APPROVED-已审核/REJECTED-已驳回',
    `approved_by` BIGINT COMMENT '审核人',
    `approved_time` DATETIME COMMENT '审核时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_term_id` (`term_id`),
    KEY `idx_ds_table` (`ds_id`, `table_name`, `column_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='术语-字段映射表';

-- 数据标准表
CREATE TABLE IF NOT EXISTS `gov_data_standard` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标准ID',
    `standard_code` VARCHAR(50) NOT NULL COMMENT '标准编码',
    `standard_name` VARCHAR(100) NOT NULL COMMENT '标准名称',
    `standard_type` VARCHAR(30) NOT NULL COMMENT '标准类型：CODE_STANDARD-编码标准/NAMING_STANDARD-命名规范/PRIMARY_DATA-主数据标准',
    `standard_category` VARCHAR(50) COMMENT '标准分类',
    `standard_desc` VARCHAR(500) COMMENT '标准描述',
    `rule_expr` TEXT COMMENT '规则表达式/正则',
    `example_value` VARCHAR(500) COMMENT '示例值',
    `applicable_object` VARCHAR(50) COMMENT '适用对象：TABLE_NAME/COLUMN_NAME/DATA_VALUE',
    `enforce_action` VARCHAR(20) DEFAULT 'ALERT' COMMENT '执行动作：ALERT-告警/BLOCK-阻止',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT-草稿/PUBLISHED-已发布',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_standard_code` (`standard_code`),
    KEY `idx_standard_type` (`standard_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据标准表';

-- 数据标准绑定表
CREATE TABLE IF NOT EXISTS `gov_standard_binding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
    `standard_id` BIGINT NOT NULL COMMENT '标准ID',
    `metadata_id` BIGINT COMMENT '元数据ID',
    `ds_id` BIGINT COMMENT '数据源ID',
    `target_table` VARCHAR(200) COMMENT '目标表名',
    `target_column` VARCHAR(100) COMMENT '目标字段名',
    `compliance_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '合规状态：PENDING-待检查/COMPLIANT-合规/NON_COMPLIANT-不合规',
    `violation_count` INT DEFAULT 0 COMMENT '违规数量',
    `last_check_time` DATETIME COMMENT '最后检查时间',
    `last_check_result` TEXT COMMENT '最后检查结果',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_standard_id` (`standard_id`),
    KEY `idx_metadata_id` (`metadata_id`),
    KEY `idx_ds_table` (`ds_id`, `target_table`, `target_column`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据标准绑定表';

-- ----------------------------
-- 7. 初始化数据
-- ----------------------------

-- 初始化数仓分层
INSERT INTO `dq_data_layer` (`layer_code`, `layer_name`, `layer_desc`, `layer_color`, `sort_order`) VALUES
('ODS', '原始层', 'Operational Data Store，贴源层，存放原始数据', '#909399', 1),
('DWD', '明细层', 'Wide Data，明细事实层，进行数据清洗和规范化', '#409EFF', 2),
('DWS', '汇总层', 'Data Warehouse Service，汇总事实层，按主题汇总', '#67C23A', 3),
('ADS', '应用层', 'Application Data Store，应用层，面向业务需求', '#E6A23C', 4);

-- 初始化管理员用户（密码: admin123）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `email`, `status`, `create_time`) VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '系统管理员', 'admin@bagdatahouse.com', 1, NOW());

-- 初始化数据质量维度
INSERT INTO `dqc_rule_template` (`template_code`, `template_name`, `template_desc`, `rule_type`, `apply_level`, `default_expr`, `default_threshold`, `dimension`, `builtin`, `enabled`) VALUES
-- 表级规则
('TEMPLATE_001', '行数非0校验', '校验表行数是否大于0', 'ROW_COUNT_NOT_ZERO', 'TABLE', 'SELECT COUNT(*) FROM ${table}', '{"check": "row_count > 0"}', 'COMPLETENESS', 1, 1),
('TEMPLATE_002', '行数波动监测', '监测表行数较历史是否有异常波动', 'ROW_COUNT_FLUCTUATION', 'TABLE', 'SELECT COUNT(*) FROM ${table}', '{"dimension": "fluctuation", "threshold": 20}', 'COMPLETENESS', 1, 1),
('TEMPLATE_003', '表更新时效监控', '监控表数据是否在预期时间内更新', 'TABLE_UPDATE_TIMELINESS', 'TABLE', 'SELECT MAX(${partition_column}) FROM ${table}', '{"dimension": "timeliness"}', 'TIMELINESS', 1, 1),
-- 字段级规则
('TEMPLATE_004', '空值检查', '校验字段空值率是否在阈值范围内', 'NULL_CHECK', 'COLUMN', 'SELECT COUNT(*) - COUNT(${column}) FROM ${table}', '{"check": "null_rate <= threshold", "threshold": 5}', 'COMPLETENESS', 1, 1),
('TEMPLATE_005', '唯一性校验', '校验字段唯一值数量是否符合预期', 'UNIQUE_CHECK', 'COLUMN', 'SELECT COUNT(DISTINCT ${column}) FROM ${table}', '{"check": "duplicate_count = 0"}', 'UNIQUENESS', 1, 1),
('TEMPLATE_006', '重复值检测', '检测字段是否存在重复值', 'DUPLICATE_CHECK', 'COLUMN', 'SELECT ${column}, COUNT(*) as cnt FROM ${table} GROUP BY ${column} HAVING COUNT(*) > 1', '{"check": "duplicate_count = 0"}', 'UNIQUENESS', 1, 1),
('TEMPLATE_007', '值域校验-最小值', '校验数值字段最小值是否在合理范围内', 'THRESHOLD_MIN', 'COLUMN', 'SELECT MIN(${column}) FROM ${table}', '{"check": "min_value >= threshold_min"}', 'ACCURACY', 1, 1),
('TEMPLATE_008', '值域校验-最大值', '校验数值字段最大值是否在合理范围内', 'THRESHOLD_MAX', 'COLUMN', 'SELECT MAX(${column}) FROM ${table}', '{"check": "max_value <= threshold_max"}', 'ACCURACY', 1, 1),
('TEMPLATE_009', '值域校验-范围', '校验数值字段值是否在指定范围内', 'THRESHOLD_RANGE', 'COLUMN', 'SELECT COUNT(*) FROM ${table} WHERE ${column} NOT BETWEEN ${threshold_min} AND ${threshold_max}', '{"check": "out_of_range_count = 0"}', 'ACCURACY', 1, 1),
('TEMPLATE_010', '枚举值校验', '校验字段值是否在允许的枚举值列表中', 'ENUM_CHECK', 'COLUMN', 'SELECT COUNT(*) FROM ${table} WHERE ${column} NOT IN (${enum_values})', '{"check": "invalid_count = 0"}', 'CONSISTENCY', 1, 1),
('TEMPLATE_011', '手机号格式校验', '校验手机号字段格式是否正确', 'REGEX_PHONE', 'COLUMN', 'SELECT COUNT(*) FROM ${table} WHERE ${column} IS NOT NULL AND ${column} NOT REGEXP ''^1[3-9][0-9]{9}$''', '{"pattern": "^1[3-9][0-9]{9}$", "check": "invalid_count = 0"}', 'VALIDITY', 1, 1),
('TEMPLATE_012', '邮箱格式校验', '校验邮箱字段格式是否正确', 'REGEX_EMAIL', 'COLUMN', 'SELECT COUNT(*) FROM ${table} WHERE ${column} IS NOT NULL AND ${column} NOT REGEXP ''^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}$''', '{"pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\.[a-zA-Z]{2,}$", "check": "invalid_count = 0"}', 'VALIDITY', 1, 1),
('TEMPLATE_013', '身份证号格式校验', '校验身份证号字段格式是否正确', 'REGEX_IDCARD', 'COLUMN', 'SELECT COUNT(*) FROM ${table} WHERE ${column} IS NOT NULL AND (LENGTH(${column}) != 18 OR ${column} NOT REGEXP ''^[1-9][0-9]{5}(19|20)[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[0-9]{3}[0-9Xx]$'')', '{"pattern": "^[1-9][0-9]{5}(19|20)[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[0-9]{3}[0-9Xx]$", "check": "invalid_count = 0"}', 'VALIDITY', 1, 1),
('TEMPLATE_014', '离散度分析', '分析字段值分布的离散程度', 'CARDINALITY', 'COLUMN', 'SELECT COUNT(DISTINCT ${column}) * 100.0 / COUNT(*) FROM ${table}', '{"dimension": "cardinality", "threshold": 10}', 'UNIQUENESS', 1, 1),
('TEMPLATE_015', '字符串长度校验', '校验字符串字段长度是否在指定范围内', 'LENGTH_CHECK', 'COLUMN', 'SELECT COUNT(*) FROM ${table} WHERE LENGTH(${column}) NOT BETWEEN ${min_length} AND ${max_length}', '{"check": "invalid_count = 0"}', 'VALIDITY', 1, 1),
-- 跨字段规则
('TEMPLATE_016', '跨字段逻辑校验-A大于B', '校验字段A的值是否大于字段B', 'CROSS_FIELD_COMPARE', 'CROSS_FIELD', 'SELECT COUNT(*) FROM ${table} WHERE NOT (${column_a} > ${column_b})', '{"check": "invalid_count = 0"}', 'CONSISTENCY', 1, 1),
('TEMPLATE_017', '跨字段逻辑校验-和值校验', '校验A+B=C的关系是否成立', 'CROSS_FIELD_SUM', 'CROSS_FIELD', 'SELECT COUNT(*) FROM ${table} WHERE NOT (${column_a} + ${column_b} = ${column_c})', '{"check": "invalid_count = 0"}', 'CONSISTENCY', 1, 1),
('TEMPLATE_018', '跨字段逻辑校验-空值一致性', '校验关联字段的空值是否一致', 'CROSS_FIELD_NULL_CHECK', 'CROSS_FIELD', 'SELECT COUNT(*) FROM ${table} WHERE (${column_a} IS NULL AND ${column_b} IS NOT NULL) OR (${column_a} IS NOT NULL AND ${column_b} IS NULL)', '{"check": "inconsistent_count = 0"}', 'CONSISTENCY', 1, 1),
-- 跨表规则
('TEMPLATE_019', '跨表记录数一致性', '校验源表和目标表的记录数是否一致', 'CROSS_TABLE_COUNT', 'CROSS_TABLE', 'SELECT (SELECT COUNT(*) FROM ${source_table}) - (SELECT COUNT(*) FROM ${target_table})', '{"check": "diff_count = 0"}', 'CONSISTENCY', 1, 1),
('TEMPLATE_020', '跨表主键一致性', '校验两个表的主键值是否完全一致', 'CROSS_TABLE_PRIMARY_KEY', 'CROSS_TABLE', 'SELECT COUNT(*) FROM (SELECT ${primary_key} FROM ${source_table} UNION SELECT ${primary_key} FROM ${target_table}) t GROUP BY ${primary_key} HAVING COUNT(*) = 1', '{"check": "consistent_count = 0"}', 'CONSISTENCY', 1, 1),
-- 自定义规则
('TEMPLATE_021', '自定义SQL规则', '用户编写自定义SQL进行数据质量校验', 'CUSTOM_SQL', 'TABLE', '${custom_sql}', '{"check": "result_count = 0"}', 'CUSTOM', 1, 1);


-- ----------------------------
-- 8. 数据安全（8张）
-- ----------------------------

-- 数据分类表
CREATE TABLE IF NOT EXISTS `sec_classification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `class_code` VARCHAR(50) NOT NULL COMMENT '分类编码，唯一，如 C01',
    `class_name` VARCHAR(100) NOT NULL COMMENT '分类名称，如 客户数据',
    `class_desc` VARCHAR(500) COMMENT '分类说明',
    `class_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_code` (`class_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据分类表';

-- 数据分级表
CREATE TABLE IF NOT EXISTS `sec_level` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分级ID',
    `level_code` VARCHAR(50) NOT NULL COMMENT '分级编码，唯一，如 L1/L2/L3/L4',
    `level_name` VARCHAR(100) NOT NULL COMMENT '分级名称，如 公开/内部/敏感/机密',
    `level_desc` VARCHAR(500) COMMENT '分级说明',
    `level_value` INT NOT NULL DEFAULT 1 COMMENT '等级值 1-4，数值越大越敏感',
    `color` VARCHAR(20) DEFAULT '#67C23A' COMMENT '页面展示颜色',
    `icon` VARCHAR(50) COMMENT '展示图标',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_level_code` (`level_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据分级表';

-- 敏感字段识别规则表
CREATE TABLE IF NOT EXISTS `sec_sensitivity_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码，唯一',
    `description` VARCHAR(500) COMMENT '规则描述',
    `class_id` BIGINT COMMENT '所属分类ID',
    `level_id` BIGINT COMMENT '推荐分级ID',
    `match_type` VARCHAR(30) NOT NULL DEFAULT 'COLUMN_NAME' COMMENT '匹配类型：COLUMN_NAME/COLUMN_COMMENT/DATA_TYPE/REGEX',
    `match_expr` VARCHAR(200) NOT NULL COMMENT '匹配表达式',
    `match_expr_type` VARCHAR(20) NOT NULL DEFAULT 'CONTAINS' COMMENT '匹配方式：CONTAINS/EQUALS/STARTS_WITH/REGEX',
    `suggestion_action` VARCHAR(20) COMMENT '推荐处理方式：NONE/MASK/HIDE/ENCRYPT/DELETE',
    `suggestion_mask_pattern` VARCHAR(200) COMMENT '推荐脱敏格式',
    `suggestion_mask_type` VARCHAR(20) COMMENT '脱敏类型：CENTER/TAIL/HEAD/FULL',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '规则优先级，数值越大优先级越高',
    `builtin` TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置规则：0-自定义，1-内置',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_code` (`rule_code`),
    KEY `idx_match_type` (`match_type`),
    KEY `idx_builtin` (`builtin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感字段识别规则表';

-- 脱敏规则模板表
CREATE TABLE IF NOT EXISTS `sec_mask_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码，唯一',
    `template_desc` VARCHAR(500) COMMENT '模板说明',
    `data_type` VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '适用数据类型：STRING/NUMBER/DATE/ALL',
    `mask_type` VARCHAR(20) NOT NULL DEFAULT 'MASK' COMMENT '脱敏类型：NONE/MASK/HIDE/ENCRYPT/DELETE',
    `mask_position` VARCHAR(20) COMMENT '脱敏位置：CENTER/TAIL/HEAD/FULL',
    `mask_char` VARCHAR(10) DEFAULT '*' COMMENT '脱敏字符',
    `mask_head_keep` INT COMMENT '头部保留字符数',
    `mask_tail_keep` INT COMMENT '尾部保留字符数',
    `mask_pattern` VARCHAR(200) COMMENT '脱敏格式模板，如 {HEAD6}****{TAIL4}',
    `builtin` TINYINT NOT NULL DEFAULT 0 COMMENT '是否内置：0-自定义，1-内置',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY `idx_mask_type` (`mask_type`),
    KEY `idx_builtin` (`builtin`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='脱敏规则模板表';

-- 字段敏感等级记录表
CREATE TABLE IF NOT EXISTS `sec_column_sensitivity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `ds_id` BIGINT COMMENT '数据源ID',
    `metadata_id` BIGINT COMMENT '元数据ID',
    `table_name` VARCHAR(200) COMMENT '表名',
    `column_name` VARCHAR(100) COMMENT '字段名',
    `column_comment` VARCHAR(200) COMMENT '字段注释',
    `data_type` VARCHAR(50) COMMENT '数据类型',
    `class_id` BIGINT COMMENT '数据分类ID',
    `level_id` BIGINT COMMENT '数据分级ID',
    `match_rule_id` BIGINT COMMENT '匹配到的识别规则ID',
    `mask_type` VARCHAR(20) COMMENT '脱敏方式：NONE/MASK/HIDE/ENCRYPT/DELETE',
    `mask_pattern` VARCHAR(200) COMMENT '脱敏格式',
    `mask_position` VARCHAR(20) COMMENT '脱敏位置：CENTER/TAIL/HEAD/FULL',
    `mask_char` VARCHAR(10) DEFAULT '*' COMMENT '脱敏字符',
    `confidence` DECIMAL(5,2) COMMENT '识别置信度 0-100',
    `scan_batch_no` VARCHAR(64) COMMENT '扫描批次号',
    `scan_time` DATETIME COMMENT '扫描时间',
    `review_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
    `review_comment` VARCHAR(500) COMMENT '审核意见',
    `approved_by` BIGINT COMMENT '审核人',
    `approved_time` DATETIME COMMENT '审核时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_ds_table` (`ds_id`, `table_name`),
    KEY `idx_review_status` (`review_status`),
    KEY `idx_scan_batch` (`scan_batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段敏感等级记录表';

-- 敏感字段访问申请表
CREATE TABLE IF NOT EXISTS `sec_access_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `application_no` VARCHAR(64) NOT NULL COMMENT '申请编号',
    `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
    `applicant_name` VARCHAR(50) COMMENT '申请人名称',
    `dept_id` BIGINT COMMENT '申请部门ID',
    `dept_name` VARCHAR(100) COMMENT '申请部门名称',
    `target_ds_id` BIGINT COMMENT '目标数据源ID',
    `target_ds_name` VARCHAR(100) COMMENT '目标数据源名称',
    `target_table` VARCHAR(200) COMMENT '目标表名',
    `target_columns` TEXT COMMENT '目标字段列表（JSON数组）',
    `target_column_names` VARCHAR(500) COMMENT '目标字段名称列表（逗号分隔）',
    `duration_type` VARCHAR(20) COMMENT '访问时长类型：HOUR/DAY/WEEK/CUSTOM',
    `duration_hours` INT COMMENT '访问时长值（小时）',
    `start_time` DATETIME COMMENT '申请开始时间',
    `end_time` DATETIME COMMENT '申请截止时间',
    `apply_reason` TEXT COMMENT '申请理由',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING/APPROVED/REJECTED/EXPIRED/CANCELLED',
    `approver_id` BIGINT COMMENT '审批人ID',
    `approver_name` VARCHAR(50) COMMENT '审批人名称',
    `approval_comment` TEXT COMMENT '审批意见',
    `approval_time` DATETIME COMMENT '审批时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_application_no` (`application_no`),
    KEY `idx_applicant_id` (`applicant_id`),
    KEY `idx_status` (`status`),
    KEY `idx_target_ds_table` (`target_ds_id`, `target_table`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感字段访问申请表';

-- 敏感字段访问审批记录表
CREATE TABLE IF NOT EXISTS `sec_access_audit` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审批记录ID',
    `application_id` BIGINT NOT NULL COMMENT '申请ID',
    `application_no` VARCHAR(64) COMMENT '申请编号',
    `approver_id` BIGINT COMMENT '审批人ID',
    `approver_name` VARCHAR(50) COMMENT '审批人名称',
    `approver_dept` VARCHAR(100) COMMENT '审批人部门',
    `action` VARCHAR(20) NOT NULL COMMENT '审批操作：APPROVE/REJECT',
    `comment` TEXT COMMENT '审批意见',
    `result` VARCHAR(20) COMMENT '审批结果：AGREE/REJECT',
    `audit_node` VARCHAR(20) COMMENT '审批节点：FIRST/SECOND/FINAL',
    `audit_time` DATETIME COMMENT '审批时间',
    `audit_source` VARCHAR(20) DEFAULT 'MANUAL' COMMENT '审批来源：SYSTEM/MANUAL',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_approver_id` (`approver_id`),
    KEY `idx_audit_time` (`audit_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感字段访问审批记录表';

-- 敏感字段访问审计日志表
CREATE TABLE IF NOT EXISTS `sec_access_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `application_id` BIGINT COMMENT '关联的申请ID',
    `application_no` VARCHAR(64) COMMENT '申请编号',
    `operator_id` BIGINT COMMENT '操作人ID',
    `operator_name` VARCHAR(50) COMMENT '操作人名称',
    `operator_dept` VARCHAR(100) COMMENT '操作人部门',
    `operation_type` VARCHAR(30) NOT NULL COMMENT '操作类型：APPLY/APPROVE/REJECT/ACCESS/CANCEL/EXPIRE',
    `operation_content` TEXT COMMENT '操作内容描述',
    `target_ds_id` BIGINT COMMENT '目标数据源ID',
    `target_table` VARCHAR(200) COMMENT '目标表名',
    `target_column` VARCHAR(200) COMMENT '目标字段名',
    `ip_address` VARCHAR(50) COMMENT 'IP地址',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `status` VARCHAR(20) COMMENT '操作后状态',
    `remark` VARCHAR(500) COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_user` BIGINT COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_user` BIGINT COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感字段访问审计日志表';


-- ----------------------------
-- 9. 初始化数据 - 数据安全
-- ----------------------------

-- 初始化数据分级（4级）
INSERT INTO `sec_level` (`level_code`, `level_name`, `level_desc`, `level_value`, `color`, `status`) VALUES
('L1', '公开',   '可对外公开的数据，无任何访问限制',    1, '#67C23A', 1),
('L2', '内部',   '仅限公司内部员工访问，外部不可见',    2, '#409EFF', 1),
('L3', '敏感',   '涉及用户隐私或商业秘密，需审批后访问', 3, '#E6A23C', 1),
('L4', '机密',   '高度敏感数据，限特定人员访问，全程审计', 4, '#F56C6C', 1);


-- ============================================================
-- 10. 表结构补丁（ALTER，原 V10/V11 的增量字段）
-- ============================================================

-- dqc_rule_def 表新增字段（支持自定义函数和字段级规则）
ALTER TABLE `dqc_rule_def`
ADD COLUMN `custom_function_class` VARCHAR(255) COMMENT '自定义函数类全限定名(CUSTOM_FUNC类型必填)' AFTER `update_time`,
ADD COLUMN `custom_function_params` TEXT COMMENT '自定义函数参数字符串(JSON格式)' AFTER `custom_function_class`,
ADD COLUMN `layer_code` VARCHAR(20) COMMENT '数据层编码: ODS/DWD/DWS/ADS' AFTER `apply_level`;

-- dprofile_column_stats 表新增字段（支持异常值分析）
ALTER TABLE `dprofile_column_stats`
ADD COLUMN `outlier_rate` DECIMAL(10,4) COMMENT '异常值比例' AFTER `outlier_count`,
ADD COLUMN `outlier_method` VARCHAR(50) COMMENT '异常值检测方法：IQR/ZSCORE/DBSCAN' AFTER `outlier_rate`;

-- dq_datasource 表新增 schema_name 字段（支持 PostgreSQL 等带 schema 概念的数据库）
ALTER TABLE `dq_datasource`
ADD COLUMN `schema_name` VARCHAR(100) DEFAULT NULL COMMENT 'Schema名称(用于PostgreSQL等，可选)' AFTER `database_name`;


-- ============================================================
-- 11. 菜单结构修复（解决父菜单点击后跳转/路由冲突问题）
--
-- 根因：test_data.sql 中所有子菜单 parent_id 错误地设为 0
--       导致子菜单被当作叶子节点，与父菜单路由冲突
--
-- 前端路由注册依赖 path 前缀白名单（router/index.ts）：
--   /dqc/*, /dprofile/*, /monitor/*, /governance/*,
--   /security/*, /system/*, /dashboard
--
-- component 路径必须与 views 目录结构一致（componentMap.ts）
-- ============================================================

-- 11.1 修复所有子菜单的 parent_id（CATALOG 节点 ID：3=数据质量, 5=数据监控, 6=数据治理, 7=数据安全, 8=系统管理）
UPDATE `sys_menu` SET `parent_id` = 3 WHERE `menu_code` IN ('QUALITY_PLAN', 'QUALITY_RULE', 'QUALITY_TEMPLATE', 'QUALITY_EXECUTION', 'QUALITY_REPORT');
UPDATE `sys_menu` SET `parent_id` = 5 WHERE `menu_code` IN ('MONITOR_METRIC', 'MONITOR_ALERT', 'MONITOR_ALERT_RECORD', 'MONITOR_EXECUTION');
UPDATE `sys_menu` SET `parent_id` = 6 WHERE `menu_code` IN ('GOV_METADATA', 'GOV_LINEAGE', 'GOV_STANDARD', 'GOV_GLOSSARY', 'GOV_CATALOG');
UPDATE `sys_menu` SET `parent_id` = 7 WHERE `menu_code` IN ('SEC_CLASSIFICATION', 'SEC_SENSITIVITY', 'SEC_MASK', 'SEC_ACCESS', 'SEC_AUDIT');
UPDATE `sys_menu` SET `parent_id` = 8 WHERE `menu_code` IN ('SYS_USER', 'SYS_ROLE', 'SYS_MENU', 'SYS_DEPT');

-- 11.2 统一更新所有菜单的 path 和 component

-- 数据质量模块：path 使用 /dqc 前缀
UPDATE `sys_menu` SET `path` = '/dqc/plan',      `component` = 'dqc/plan/index'           WHERE `menu_code` = 'QUALITY_PLAN';
UPDATE `sys_menu` SET `path` = '/dqc/rule',    `component` = 'dqc/rule-def/index'       WHERE `menu_code` = 'QUALITY_RULE';
UPDATE `sys_menu` SET `path` = '/dqc/template', `component` = 'dqc/rule-template/index' WHERE `menu_code` = 'QUALITY_TEMPLATE';
UPDATE `sys_menu` SET `path` = '/dqc/execution', `component` = 'dqc/execution/index'       WHERE `menu_code` = 'QUALITY_EXECUTION';
UPDATE `sys_menu` SET `path` = '/dqc/report',   `component` = 'dqc/report/index'         WHERE `menu_code` = 'QUALITY_REPORT';

-- 数据监控模块：path 使用 /monitor 前缀
UPDATE `sys_menu` SET `path` = '/monitor/index',         `component` = 'monitor/index'               WHERE `menu_code` = 'MONITOR_METRIC';
UPDATE `sys_menu` SET `path` = '/monitor/alert',         `component` = 'monitor/alert-rule/index'    WHERE `menu_code` = 'MONITOR_ALERT';
UPDATE `sys_menu` SET `path` = '/monitor/alert-record',  `component` = 'monitor/alert-record/index'   WHERE `menu_code` = 'MONITOR_ALERT_RECORD';
UPDATE `sys_menu` SET `path` = '/monitor/execution',     `component` = 'monitor/task-execution/index' WHERE `menu_code` = 'MONITOR_EXECUTION';

-- 数据治理模块：path 使用 /governance 前缀
UPDATE `sys_menu` SET `path` = '/governance/metadata', `component` = 'governance/Metadata/index' WHERE `menu_code` = 'GOV_METADATA';
UPDATE `sys_menu` SET `path` = '/governance/lineage',   `component` = 'governance/Lineage/index'   WHERE `menu_code` = 'GOV_LINEAGE';
UPDATE `sys_menu` SET `path` = '/governance/standard',  `component` = 'governance/Standard/index'  WHERE `menu_code` = 'GOV_STANDARD';
UPDATE `sys_menu` SET `path` = '/governance/glossary',  `component` = 'governance/Glossary/index' WHERE `menu_code` = 'GOV_GLOSSARY';
UPDATE `sys_menu` SET `path` = '/governance/catalog',   `component` = 'governance/Asset/index'    WHERE `menu_code` = 'GOV_CATALOG';

-- 数据安全模块：path 使用 /security 前缀
UPDATE `sys_menu` SET `path` = '/security/classification', `component` = 'security/classification/index' WHERE `menu_code` = 'SEC_CLASSIFICATION';
UPDATE `sys_menu` SET `path` = '/security/sensitivity',    `component` = 'security/sensitivity/index'    WHERE `menu_code` = 'SEC_SENSITIVITY';
UPDATE `sys_menu` SET `path` = '/security/mask',          `component` = 'security/mask/index'          WHERE `menu_code` = 'SEC_MASK';
UPDATE `sys_menu` SET `path` = '/security/access',        `component` = 'security/access/index'        WHERE `menu_code` = 'SEC_ACCESS';
UPDATE `sys_menu` SET `path` = '/security/audit',        `component` = 'security/audit/index'         WHERE `menu_code` = 'SEC_AUDIT';

-- 数据探查模块：path 使用 /dprofile 前缀
UPDATE `sys_menu` SET `path` = '/dprofile/task',   `component` = 'dprofile/task/index'   WHERE `menu_code` = 'PROFILE_TASK';
UPDATE `sys_menu` SET `path` = '/dprofile/report', `component` = 'dprofile/report/index' WHERE `menu_code` = 'PROFILE_REPORT';

-- 数据源管理：path 使用 /dqc 前缀
UPDATE `sys_menu` SET `path` = '/dqc/datasource',  `component` = 'dqc/datasource/index'  WHERE `menu_code` = 'DS_LIST';
UPDATE `sys_menu` SET `path` = '/dqc/data-domain', `component` = 'dqc/data-domain/index' WHERE `menu_code` = 'DS_DOMAIN';

-- 工作台首页
UPDATE `sys_menu` SET `path` = '/dashboard', `component` = 'dashboard/index' WHERE `menu_code` = 'WORKBENCH_INDEX';

-- 11.3 新增缺失的菜单项（数据源管理/数据探查/工作台子菜单）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `visible`) VALUES
(21,  2, '数据源列表', 'DS_LIST',        'MENU', '/dqc/datasource',  'dqc/datasource/index',  'database',   1, 1),
(22,  2, '数据域管理', 'DS_DOMAIN',      'MENU', '/dqc/data-domain', 'dqc/data-domain/index', 'apartment',  2, 1),
(11,  1, '工作台首页', 'WORKBENCH_INDEX', 'MENU', '/dashboard',       'dashboard/index',        'dashboard',  1, 1),
(41,  4, '探查任务',   'PROFILE_TASK',    'MENU', '/dprofile/task',   'dprofile/task/index',   'experiment', 1, 1),
(42,  4, '探查报告',  'PROFILE_REPORT',  'MENU', '/dprofile/report', 'dprofile/report/index', 'file-text',  2, 1)
ON DUPLICATE KEY UPDATE `parent_id` = VALUES(`parent_id`), `path` = VALUES(`path`), `component` = VALUES(`component`);

-- 11.4 分配新增菜单的权限
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.id FROM `sys_menu` m WHERE m.id IN (11, 21, 22, 41, 42)
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, m.id FROM `sys_menu` m WHERE m.id IN (11, 21, 22, 41, 42)
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3, 41), (3, 42), (3, 75)
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4, 41), (4, 42)
ON DUPLICATE KEY UPDATE `role_id` = `role_id`;