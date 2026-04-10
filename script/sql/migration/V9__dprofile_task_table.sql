-- ============================================================
-- 数据探查任务表 dprofile_task
-- 所属数据库: ry_bigdata_v1
-- 创建时间: 2026-04-09
-- ============================================================

USE ry_bigdata_v1;

CREATE TABLE IF NOT EXISTS `dprofile_task` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务编码',
  `task_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `task_desc` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务描述',
  `ds_id` bigint NOT NULL COMMENT '关联数据源ID',
  `profile_level` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DETAILED' COMMENT '探查级别：BASIC/DETAILED/FULL',
  `table_pattern` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表名匹配模式（支持通配符*）',
  `target_columns` varchar(4000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '指定探查的列名（逗号分隔，留空表示全部列）',
  `trigger_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'MANUAL' COMMENT '触发方式：MANUAL/SCHEDULE/API',
  `cron_expr` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Cron表达式',
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/RUNNING/SUCCESS/FAILED/STOPPED',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近运行时间',
  `last_run_status` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近运行状态',
  `total_tables` int DEFAULT '0' COMMENT '涉及表数量',
  `del_flag` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '删除标志（0=正常，1=删除）',
  `tenant_id` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_code` (`task_code`),
  KEY `idx_ds_id` (`ds_id`),
  KEY `idx_status` (`status`),
  KEY `idx_profile_level` (`profile_level`),
  KEY `idx_trigger_type` (`trigger_type`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据探查任务表';
