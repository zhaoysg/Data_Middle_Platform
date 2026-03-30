-- ============================================================
-- 数据中台 - 完整测试数据（含清表语句）
--
-- 使用方法:
--   1. 在全新数据库上，先执行 V1__init.sql 创建表结构
--   2. 再执行 V14__fix_menu_and_rule_def.sql 修复字段和菜单
--   3. 最后执行本文件 test_data.sql 导入测试数据
--
-- 说明:
--   每个 INSERT 前都有 TRUNCATE 语句，确保可重复执行时数据干净
--   如果是已存在数据的数据库，可以注释掉对应的 TRUNCATE 语句
--
-- 版本: 1.0.0 | 日期: 2026-03-24
-- ============================================================

-- ============================================================
-- 1. 系统管理 - 部门 / 用户 / 角色 / 菜单
-- ============================================================

-- 1.1 部门（树形结构）
TRUNCATE TABLE `sys_dept`;
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `dept_type`, `sort_order`, `status`) VALUES
(1, 0, '数据中台部', 'DEPT_DATA',  'DEPT', 1, 1),
(2, 1, '数据治理组', 'DEPT_GOV',   'DEPT', 1, 1),
(3, 1, '数据质量组', 'DEPT_DQC',   'DEPT', 2, 1),
(4, 1, '数据开发组', 'DEPT_DEV',   'DEPT', 3, 1),
(5, 0, '业务运营部', 'DEPT_BIZ',   'DEPT', 2, 1);

-- 1.2 超级管理员（必须在其他用户之前插入，确保ID=1）
TRUNCATE TABLE `sys_user`;
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `dept_id`, `status`) VALUES
(1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '系统管理员', 'admin@company.com', '13800000000', 1, 1);

-- 1.3 普通用户（密码统一为 admin123：240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9）
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `dept_id`, `status`) VALUES
(2, 'zhangsan', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '张三', 'zhangsan@company.com', '13800001001', 2, 1),
(3, 'lisi',     '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '李四', 'lisi@company.com',     '13800001002', 3, 1),
(4, 'wangwu',   '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '王五', 'wangwu@company.com',   '13800001003', 4, 1),
(5, 'viewer',   '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '访客用户', 'viewer@company.com', '13800001004', 5, 1);

-- 1.4 角色（包含超级管理员角色）
TRUNCATE TABLE `sys_role`;
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_type`, `data_scope`, `status`, `sort_order`, `remark`) VALUES
(1, '超级管理员', 'ROLE_SUPER_ADMIN',   'SYSTEM', 'ALL',    1, 0, '拥有系统全部权限'),
(2, '数据管理员', 'ROLE_DATA_ADMIN',     'CUSTOM', 'ALL',    1, 1, '拥有全部数据管理权限'),
(3, '数据治理专员', 'ROLE_DATA_STEWARD', 'CUSTOM', 'DEPT',   1, 2, '负责数据治理、质量、安全'),
(4, '数据分析师', 'ROLE_ANALYST',         'CUSTOM', 'DEPT',   1, 3, '数据查询和分析'),
(5, '访客',       'ROLE_VIEWER',         'CUSTOM', 'CUSTOM', 1, 4, '只读权限');

-- 1.5 用户-角色关联
TRUNCATE TABLE `sys_user_role`;
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5);

-- 1.6 菜单（左侧导航结构，path 和 component 已通过 V14 修复）
--     CATALOG 类型的目录（parent_id=0）path 必须为空，否则会被错误注册为叶子路由
--     INSERT 仅用于初始化，重复执行使用 INSERT IGNORE 避免主键冲突
TRUNCATE TABLE `sys_menu`;
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `icon`, `sort_order`, `visible`) VALUES
-- 一级目录（CATALOG：path 必须为空，否则会导致路由冲突）
(1, 0, '工作台',      'WORKBENCH',   'CATALOG', '',                  '',                     'dashboard',         1, 1),
(2, 0, '数据源管理', 'DATASOURCE',  'CATALOG', '',                  '',                     'database',         2, 1),
(3, 0, '数据质量',   'QUALITY',     'CATALOG', '',                  '',                     'check-circle',     3, 1),
(4, 0, '数据探查',   'PROFILE',     'CATALOG', '',                  '',                     'bar-chart',        4, 1),
(5, 0, '数据监控',   'MONITOR',     'CATALOG', '',                  '',                     'alert',            5, 1),
(6, 0, '数据治理',   'GOVERNANCE',  'CATALOG', '',                  '',                     'safety',           6, 1),
(7, 0, '数据安全',   'SECURITY',    'CATALOG', '',                  '',                     'lock',             7, 1),
(8, 0, '系统管理',   'SYSTEM',      'CATALOG', '',                  '',                     'setting',          8, 1),
-- 二级菜单 - 数据质量（parent_id=3，匹配一级目录 ID）
(31, 3, '质检方案',   'QUALITY_PLAN',      'MENU', '/dqc/plan',          'dqc/plan/index',            'solution',     1, 1),
(32, 3, '规则定义',   'QUALITY_RULE',       'MENU', '/dqc/rule',          'dqc/rule-def/index',        'file-search',  2, 1),
(33, 3, '规则模板',   'QUALITY_TEMPLATE',   'MENU', '/dqc/template',       'dqc/rule-template/index',    'copy',         3, 1),
(34, 3, '执行记录',   'QUALITY_EXECUTION', 'MENU', '/dqc/execution',      'dqc/execution/index',       'history',      4, 1),
(35, 3, '质量报告',   'QUALITY_REPORT',     'MENU', '/dqc/report',        'dqc/report/index',          'report',       5, 1),
(36, 3, '质量评分',   'QUALITY_SCORE',      'MENU', '/dqc/score',         'dqc/score/index',           'star',         6, 1),
-- 二级菜单 - 数据监控（parent_id=5）
(51, 5, '指标管理',   'MONITOR_METRIC',        'MENU', '/monitor/index',        'monitor/index',               'line-chart',   1, 1),
(52, 5, '告警规则',   'MONITOR_ALERT',         'MENU', '/monitor/alert',         'monitor/alert-rule/index',    'bell',         2, 1),
(53, 5, '告警记录',   'MONITOR_ALERT_RECORD',  'MENU', '/monitor/alert-record', 'monitor/alert-record/index',  'notification', 3, 1),
(54, 5, '任务执行',   'MONITOR_EXECUTION',     'MENU', '/monitor/execution',     'monitor/task-execution/index','play-circle',  4, 1),
-- 二级菜单 - 数据治理（parent_id=6）
(61, 6, '元数据',     'GOV_METADATA',  'MENU', '/governance/metadata', 'governance/Metadata/index', 'table',       1, 1),
(62, 6, '数据血缘',   'GOV_LINEAGE',  'MENU', '/governance/lineage',   'governance/Lineage/index',   'apartment',   2, 1),
(63, 6, '数据标准',   'GOV_STANDARD', 'MENU', '/governance/standard',  'governance/Standard/index',  'book',        3, 1),
(64, 6, '术语库',     'GOV_GLOSSARY', 'MENU', '/governance/glossary',  'governance/Glossary/index', 'profile',     4, 1),
(65, 6, '资产目录',   'GOV_CATALOG',  'MENU', '/governance/catalog',   'governance/Asset/index',    'folder',      5, 1),
-- 二级菜单 - 数据安全（parent_id=7）
(71, 7, '分类分级',   'SEC_CLASSIFICATION', 'MENU', '/security/classification', 'security/classification/index', 'safety-certificate', 1, 1),
(72, 7, '敏感识别',   'SEC_SENSITIVITY',    'MENU', '/security/sensitivity',    'security/sensitivity/index',    'eye-invisible',     2, 1),
(73, 7, '脱敏规则',   'SEC_MASK',            'MENU', '/security/mask',           'security/mask/index',           'border',             3, 1),
(74, 7, '访问审批',   'SEC_ACCESS',          'MENU', '/security/access',         'security/access/index',         'key',                4, 1),
(75, 7, '审计日志',   'SEC_AUDIT',           'MENU', '/security/audit',          'security/audit/index',          'audit',              5, 1),
-- 二级菜单 - 系统管理（parent_id=8）
(81, 8, '用户管理',   'SYS_USER', 'MENU', '/system/user', 'system/user/index', 'user',   1, 1),
(82, 8, '角色管理',   'SYS_ROLE', 'MENU', '/system/role', 'system/role/index', 'team',   2, 1),
(83, 8, '菜单管理',   'SYS_MENU', 'MENU', '/system/menu', 'system/menu/index', 'menu',   3, 1),
(84, 8, '部门管理',   'SYS_DEPT', 'MENU', '/system/dept', 'system/dept/index', 'cluster', 4, 1),
-- 三级菜单 - 数据探查（parent_id=4，路径前缀 /dprofile）
(41, 4, '探查任务', 'PROFILE_TASK',   'MENU', '/dprofile/task',   'dprofile/task/index',   'experiment', 1, 1),
(42, 4, '探查报告', 'PROFILE_REPORT', 'MENU', '/dprofile/report', 'dprofile/report/index', 'file-text',  2, 1),
-- 三级菜单 - 数据源管理（parent_id=2，路径前缀 /dqc）
(21, 2, '数据源列表', 'DS_LIST',    'MENU', '/dqc/datasource', 'dqc/datasource/index', 'database',   1, 1),
(22, 2, '数据域管理', 'DS_DOMAIN',  'MENU', '/dqc/data-domain','dqc/data-domain/index','apartment',  2, 1),
-- 三级菜单 - 工作台（parent_id=1）
(11, 1, '工作台首页', 'WORKBENCH_INDEX', 'MENU', '/dashboard', 'dashboard/index', 'dashboard', 1, 1);

-- 1.7 角色-菜单关联（超级管理员和数据管理员拥有全部菜单权限）
TRUNCATE TABLE `sys_role_menu`;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.id FROM `sys_menu` m WHERE m.id > 0;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, m.id FROM `sys_menu` m WHERE m.id > 0;

-- 数据治理专员 - 分配部分菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3,  1), (3,  3), (3, 31), (3, 32), (3, 33), (3, 34),
(3,  6), (3, 61), (3, 62), (3, 63), (3, 64), (3, 65),
(3,  7), (3, 71), (3, 72), (3, 73), (3, 75),
(3, 41), (3, 42);

-- 数据分析师 - 分配查看类菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4,  1), (4, 61), (4, 62), (4, 41), (4, 42);

-- ============================================================
-- 2. 数据源管理 - 数据源 / 数据域
-- ============================================================

-- 2.1 数据域
TRUNCATE TABLE `dq_data_domain`;
INSERT INTO `dq_data_domain` (`id`, `domain_code`, `domain_name`, `domain_desc`, `dept_id`, `sort_order`, `status`) VALUES
(1, 'DOMAIN_USER',   '用户域',   '用户相关的所有数据和业务', 2, 1, 1),
(2, 'DOMAIN_ORDER',  '订单域',   '订单、交易、支付相关数据', 3, 2, 1),
(3, 'DOMAIN_PRODUCT', '商品域',  '商品、库存、价格相关数据', 4, 3, 1),
(4, 'DOMAIN_FINANCE', '财务域',  '财务、结算、报表相关数据', 2, 4, 1);

-- 2.2 数据源
TRUNCATE TABLE `dq_datasource`;
INSERT INTO `dq_datasource` (`id`, `ds_name`, `ds_code`, `ds_type`, `host`, `port`, `database_name`, `username`, `password`, `connection_params`, `data_layer`, `dept_id`, `owner_id`, `status`, `remark`) VALUES
(1, 'MySQL源库(ODS)',  'mysql_ods',        'MYSQL',      '192.168.1.100', 3306, 'source_db',      'root', 'encrypted_password_placeholder', '{"charset":"utf8mb4","useSSL":false}', 'ODS', 1, 1, 1, 'ODS贴源层，存放原始数据'),
(2, 'MySQL数仓(DWD)',  'mysql_dwd',         'MYSQL',      '192.168.1.101', 3306, 'warehouse_dwd',  'root', 'encrypted_password_placeholder', '{"charset":"utf8mb4","useSSL":false}', 'DWD', 1, 2, 1, 'DWD明细层，数据清洗和规范化'),
(3, 'MySQL数仓(DWS)',  'mysql_dws',         'MYSQL',      '192.168.1.102', 3306, 'warehouse_dws',  'root', 'encrypted_password_placeholder', '{"charset":"utf8mb4","useSSL":false}', 'DWS', 1, 3, 1, 'DWS汇总层，按主题汇总'),
(4, 'MySQL应用库(ADS)', 'mysql_ads',        'MYSQL',      '192.168.1.103', 3306, 'app_ads',        'root', 'encrypted_password_placeholder', '{"charset":"utf8mb4","useSSL":false}', 'ADS', 1, 4, 1, 'ADS应用层，面向业务需求'),
(5, 'PostgreSQL业务库', 'pg_biz',          'POSTGRESQL', '192.168.1.110', 5432, 'business_db',    'pguser','encrypted_password_placeholder', '{"sslmode":"disable"}',                  'ODS', 1, 2, 1, '业务系统PostgreSQL数据库'),
(6, 'SQL Server历史库', 'sqlserver_history','SQLSERVER',  '192.168.1.120', 1433, 'history_db',     'sa',   'encrypted_password_placeholder', '{"encrypt":false}',                    'ODS', 1, 3, 1, '历史数据SQL Server数据库');

-- ============================================================
-- 3. 数据质量 - 规则定义 / 质检方案 / 执行记录
-- ============================================================

-- 3.1 数据质量规则定义
TRUNCATE TABLE `dqc_rule_def`;
INSERT INTO `dqc_rule_def` (`id`, `rule_name`, `rule_code`, `template_id`, `rule_type`, `apply_level`, `layer_code`, `dimensions`, `rule_expr`, `target_ds_id`, `target_table`, `target_column`, `threshold_min`, `threshold_max`, `error_level`, `rule_strength`, `alert_receivers`, `sort_order`, `enabled`, `dept_id`) VALUES
(1, '用户表行数非零校验',     'RULE_USER_ROW_COUNT',     1,  'ROW_COUNT_NOT_ZERO',      'TABLE',   'DWD', '["Completeness"]',       'SELECT COUNT(*) FROM dim_users',                                             2, 'dim_users',  NULL,         1,    NULL,   'HIGH',     'STRONG', 'admin@company.com', 1, 1, 2),
(2, '用户手机号空值率校验',   'RULE_USER_PHONE_NULL',     4,  'NULL_CHECK',               'COLUMN',  'DWD', '["Completeness"]',       'SELECT COUNT(*) - COUNT(user_phone) FROM dim_users WHERE is_deleted = 0',       2, 'dim_users',  'user_phone', NULL,  5.00,  'MEDIUM',   'WEAK',   'admin@company.com', 2, 1, 2),
(3, '订单表行数波动监测',     'RULE_ORDER_FLUCTUATION',   2,  'ROW_COUNT_FLUCTUATION',    'TABLE',   'DWD', '["Completeness","Timeliness"]', 'SELECT COUNT(*) FROM fact_orders',                             2, 'fact_orders', NULL,         NULL,  NULL,   'MEDIUM',   'STRONG', 'admin@company.com', 3, 1, 2),
(4, '身份证号格式校验',        'RULE_IDCARD_FORMAT',       13, 'REGEX_IDCARD',             'COLUMN',  'DWD', '["Validity"]',            'SELECT COUNT(*) FROM dim_users WHERE id_card IS NOT NULL AND NOT (LENGTH(id_card) = 18)', 2, 'dim_users', 'id_card', NULL, NULL, 'HIGH', 'STRONG', 'admin@company.com', 4, 1, 2),
(5, '订单金额最小值校验',      'RULE_ORDER_AMOUNT_MIN',    7,  'THRESHOLD_MIN',            'COLUMN',  'DWD', '["Accuracy"]',            'SELECT MIN(order_amount) FROM fact_orders',                                  2, 'fact_orders', 'order_amount', 0.01, NULL, 'MEDIUM', 'WEAK',   'admin@company.com', 5, 1, 2),
(6, '订单状态枚举校验',        'RULE_ORDER_STATUS_ENUM',   10, 'ENUM_CHECK',                'COLUMN',  'DWD', '["Consistency"]',         'SELECT COUNT(*) FROM fact_orders WHERE order_status NOT IN (''PAID'',''SHIPPED'',''COMPLETED'',''CANCELLED'')', 2, 'fact_orders', 'order_status', NULL, NULL, 'LOW', 'WEAK', NULL, 6, 1, 2),
(7, 'ODS层源表更新时效',      'RULE_ODS_UPDATE_TIMELINESS',3,  'TABLE_UPDATE_TIMELINESS',  'TABLE',   'ODS', '["Timeliness"]',          'SELECT MAX(${partition_column}) FROM ${table}',                                 1, 'ods_src_users', 'update_time', NULL, NULL, 'HIGH', 'STRONG', 'admin@company.com', 7, 1, 3),
(8, '用户唯一性校验',          'RULE_USER_UNIQUE',         5,  'UNIQUE_CHECK',             'COLUMN',  'DWD', '["Uniqueness"]',          'SELECT COUNT(DISTINCT user_id) - COUNT(user_id) FROM dim_users',               2, 'dim_users', 'user_id', NULL, NULL, 'CRITICAL','STRONG', 'admin@company.com', 8, 1, 2);

-- 3.2 质检方案
TRUNCATE TABLE `dqc_plan`;
INSERT INTO `dqc_plan` (`id`, `plan_name`, `plan_code`, `plan_desc`, `bind_type`, `layer_code`, `dept_id`, `trigger_type`, `trigger_cron`, `alert_on_success`, `alert_on_failure`, `auto_block`, `status`, `rule_count`, `table_count`) VALUES
(1, 'ODS层数据质量方案', 'PLAN_ODS_DAILY',  '每日ODS层数据质量检查，包括空值、格式、更新时效', 'LAYER',  'ODS', 1, 'SCHEDULE', '0 2 * * *', 0, 1, 1, 'PUBLISHED', 5,  3),
(2, 'DWD层数据质量方案', 'PLAN_DWD_DAILY',  '每日DWD层数据质量检查，包括唯一性、完整性',    'LAYER',  'DWD', 1, 'SCHEDULE', '0 3 * * *', 0, 1, 1, 'PUBLISHED', 10, 5),
(3, '用户域质量专题',     'PLAN_USER_DOMAIN', '用户域数据质量专题检查，覆盖用户表全部字段',   'DOMAIN', NULL,  2, 'SCHEDULE', '0 4 * * 1', 0, 1, 1, 'PUBLISHED', 8,  2),
(4, '订单域质量专题',     'PLAN_ORDER_DOMAIN','订单域数据质量专题，覆盖订单主表和事实表',    'DOMAIN', NULL,  2, 'MANUAL',   NULL,          0, 1, 1, 'DRAFT',     6,  3);

-- 3.3 方案-规则关联
TRUNCATE TABLE `dqc_plan_rule`;
INSERT INTO `dqc_plan_rule` (`id`, `plan_id`, `rule_id`, `rule_order`, `enabled`) VALUES
(1,  1, 7, 1, 1),
(2,  2, 1, 1, 1),
(3,  2, 2, 2, 1),
(4,  2, 4, 3, 1),
(5,  2, 8, 4, 1),
(6,  3, 1, 1, 1),
(7,  3, 2, 2, 1),
(8,  3, 4, 3, 1),
(9,  3, 8, 4, 1),
(10, 4, 3, 1, 1),
(11, 4, 5, 2, 1),
(12, 4, 6, 3, 1);

-- 3.4 质检执行记录（2024-01-01 ~ 2024-01-16，含各维度评分明细）
TRUNCATE TABLE `dqc_execution`;
INSERT INTO `dqc_execution` (`id`, `execution_no`, `plan_id`, `plan_name`, `plan_code`, `layer_code`, `dept_id`, `trigger_type`, `trigger_user`, `start_time`, `end_time`, `elapsed_ms`, `status`, `total_rules`, `passed_rules`, `failed_rules`, `skipped_rules`, `total_tables`, `quality_score`, `score_breakdown`, `dimension_scores`, `blocked`) VALUES
-- ODS层每日执行
(1,  'EXEC20240101001', 1, 'ODS层数据质量方案', 'PLAN_ODS_DAILY', 'ODS', 1, 'SCHEDULE', 1, '2024-01-01 02:00:00', '2024-01-01 02:04:12', 252000, 'SUCCESS', 2, 2, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":1,"totalRows":1480000,"avgCompleteness":98.50,"avgTimeliness":100.00}', '{"Completeness":100.00,"Uniqueness":null,"Accuracy":null,"Consistency":null,"Timeliness":100.00,"Validity":100.00}', 0),
(2,  'EXEC20240102001', 1, 'ODS层数据质量方案', 'PLAN_ODS_DAILY', 'ODS', 1, 'SCHEDULE', 1, '2024-01-02 02:00:00', '2024-01-02 02:03:45', 225000, 'SUCCESS', 2, 2, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":1,"totalRows":1485000,"avgCompleteness":99.20,"avgTimeliness":100.00}', '{"Completeness":100.00,"Uniqueness":null,"Accuracy":null,"Consistency":null,"Timeliness":100.00,"Validity":100.00}', 0),
(3,  'EXEC20240103001', 1, 'ODS层数据质量方案', 'PLAN_ODS_DAILY', 'ODS', 1, 'SCHEDULE', 1, '2024-01-03 02:00:00', '2024-01-03 02:05:30', 330000, 'SUCCESS', 2, 2, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":1,"totalRows":1490000,"avgCompleteness":99.50,"avgTimeliness":100.00}', '{"Completeness":100.00,"Uniqueness":null,"Accuracy":null,"Consistency":null,"Timeliness":100.00,"Validity":100.00}', 0),
(4,  'EXEC20240104001', 1, 'ODS层数据质量方案', 'PLAN_ODS_DAILY', 'ODS', 1, 'SCHEDULE', 1, '2024-01-04 02:00:00', '2024-01-04 02:08:10', 490000, 'FAILED',  2, 1, 1, 0, 1,  55.00, '{"totalTables":1,"checkedColumns":1,"totalRows":1495000,"avgCompleteness":60.00,"avgTimeliness":50.00}', '{"Completeness":60.00,"Uniqueness":null,"Accuracy":null,"Consistency":null,"Timeliness":50.00,"Validity":null}', 0),
(5,  'EXEC20240105001', 1, 'ODS层数据质量方案', 'PLAN_ODS_DAILY', 'ODS', 1, 'SCHEDULE', 1, '2024-01-05 02:00:00', '2024-01-05 02:04:55', 295000, 'SUCCESS', 2, 2, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":1,"totalRows":1500000,"avgCompleteness":99.80,"avgTimeliness":100.00}', '{"Completeness":100.00,"Uniqueness":null,"Accuracy":null,"Consistency":null,"Timeliness":100.00,"Validity":100.00}', 0),
(6,  'EXEC20240106001', 1, 'ODS层数据质量方案', 'PLAN_ODS_DAILY', 'ODS', 1, 'SCHEDULE', 1, '2024-01-06 02:00:00', '2024-01-06 02:03:20', 200000, 'SUCCESS', 2, 2, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":1,"totalRows":1502000,"avgCompleteness":99.90,"avgTimeliness":100.00}', '{"Completeness":100.00,"Uniqueness":null,"Accuracy":null,"Consistency":null,"Timeliness":100.00,"Validity":100.00}', 0),
(7,  'EXEC20240107001', 1, 'ODS层数据质量方案', 'PLAN_ODS_DAILY', 'ODS', 1, 'SCHEDULE', 1, '2024-01-07 02:00:00', '2024-01-07 02:06:00', 360000, 'SUCCESS', 2, 2, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":1,"totalRows":1505000,"avgCompleteness":99.95,"avgTimeliness":100.00}', '{"Completeness":100.00,"Uniqueness":null,"Accuracy":null,"Consistency":null,"Timeliness":100.00,"Validity":100.00}', 0),
-- DWD层每日执行
(8,  'EXEC20240101002', 2, 'DWD层数据质量方案', 'PLAN_DWD_DAILY', 'DWD', 1, 'SCHEDULE', 1, '2024-01-01 03:00:00', '2024-01-01 03:10:22', 622000, 'SUCCESS', 4, 4, 0, 0, 2, 100.00, '{"totalTables":2,"checkedColumns":3,"totalRows":1480000,"avgCompleteness":98.80,"avgUniqueness":100.00,"avgValidity":100.00}', '{"Completeness":98.00,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":100.00}', 0),
(9,  'EXEC20240102002', 2, 'DWD层数据质量方案', 'PLAN_DWD_DAILY', 'DWD', 1, 'SCHEDULE', 1, '2024-01-02 03:00:00', '2024-01-02 03:12:45', 765000, 'SUCCESS', 4, 3, 1, 0, 2,  88.00, '{"totalTables":2,"checkedColumns":3,"totalRows":1485000,"avgCompleteness":94.00,"avgUniqueness":100.00,"avgValidity":90.00}', '{"Completeness":94.00,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":90.00}', 0),
(10, 'EXEC20240103002', 2, 'DWD层数据质量方案', 'PLAN_DWD_DAILY', 'DWD', 1, 'SCHEDULE', 1, '2024-01-03 03:00:00', '2024-01-03 03:09:18', 558000, 'SUCCESS', 4, 4, 0, 0, 2, 100.00, '{"totalTables":2,"checkedColumns":3,"totalRows":1490000,"avgCompleteness":98.50,"avgUniqueness":100.00,"avgValidity":100.00}', '{"Completeness":98.50,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":100.00}', 0),
(11, 'EXEC20240104002', 2, 'DWD层数据质量方案', 'PLAN_DWD_DAILY', 'DWD', 1, 'SCHEDULE', 1, '2024-01-04 03:00:00', '2024-01-04 03:14:30', 870000, 'FAILED',  4, 2, 2, 0, 2,  65.00, '{"totalTables":2,"checkedColumns":3,"totalRows":1495000,"avgCompleteness":80.00,"avgUniqueness":80.00,"avgValidity":70.00}', '{"Completeness":80.00,"Uniqueness":80.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":70.00}', 0),
(12, 'EXEC20240105002', 2, 'DWD层数据质量方案', 'PLAN_DWD_DAILY', 'DWD', 1, 'SCHEDULE', 1, '2024-01-05 03:00:00', '2024-01-05 03:11:00', 660000, 'SUCCESS', 4, 3, 1, 0, 2,  90.00, '{"totalTables":2,"checkedColumns":3,"totalRows":1500000,"avgCompleteness":95.00,"avgUniqueness":100.00,"avgValidity":90.00}', '{"Completeness":95.00,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":90.00}', 0),
(13, 'EXEC20240106002', 2, 'DWD层数据质量方案', 'PLAN_DWD_DAILY', 'DWD', 1, 'SCHEDULE', 1, '2024-01-06 03:00:00', '2024-01-06 03:08:45', 525000, 'SUCCESS', 4, 4, 0, 0, 2, 100.00, '{"totalTables":2,"checkedColumns":3,"totalRows":1502000,"avgCompleteness":99.50,"avgUniqueness":100.00,"avgValidity":100.00}', '{"Completeness":99.50,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":100.00}', 0),
(14, 'EXEC20240107002', 2, 'DWD层数据质量方案', 'PLAN_DWD_DAILY', 'DWD', 1, 'SCHEDULE', 1, '2024-01-07 03:00:00', '2024-01-07 03:10:30', 630000, 'SUCCESS', 4, 4, 0, 0, 2, 100.00, '{"totalTables":2,"checkedColumns":3,"totalRows":1505000,"avgCompleteness":99.00,"avgUniqueness":100.00,"avgValidity":100.00}', '{"Completeness":99.00,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":100.00}', 0),
-- 用户域专题
(15, 'EXEC20240101003', 3, '用户域质量专题', 'PLAN_USER_DOMAIN', 'DWD', 2, 'SCHEDULE', 1, '2024-01-01 04:00:00', '2024-01-01 04:07:44', 464000, 'SUCCESS', 4, 4, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":3,"totalRows":1480000,"avgCompleteness":98.00,"avgUniqueness":100.00,"avgValidity":100.00}', '{"Completeness":98.00,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":100.00}', 0),
(16, 'EXEC20240102003', 3, '用户域质量专题', 'PLAN_USER_DOMAIN', 'DWD', 2, 'SCHEDULE', 1, '2024-01-02 04:00:00', '2024-01-02 04:08:30', 510000, 'SUCCESS', 4, 3, 1, 0, 1,  88.00, '{"totalTables":1,"checkedColumns":3,"totalRows":1485000,"avgCompleteness":94.00,"avgUniqueness":100.00,"avgValidity":90.00}', '{"Completeness":94.00,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":90.00}', 0),
(17, 'EXEC20240103003', 3, '用户域质量专题', 'PLAN_USER_DOMAIN', 'DWD', 2, 'SCHEDULE', 1, '2024-01-03 04:00:00', '2024-01-03 04:06:55', 415000, 'SUCCESS', 4, 4, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":3,"totalRows":1490000,"avgCompleteness":98.50,"avgUniqueness":100.00,"avgValidity":100.00}', '{"Completeness":98.50,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":100.00}', 0),
(18, 'EXEC20240104003', 3, '用户域质量专题', 'PLAN_USER_DOMAIN', 'DWD', 2, 'SCHEDULE', 1, '2024-01-04 04:00:00', '2024-01-04 04:09:10', 550000, 'FAILED',  4, 2, 2, 0, 1,  65.00, '{"totalTables":1,"checkedColumns":3,"totalRows":1495000,"avgCompleteness":80.00,"avgUniqueness":80.00,"avgValidity":70.00}', '{"Completeness":80.00,"Uniqueness":80.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":70.00}', 0),
(19, 'EXEC20240105003', 3, '用户域质量专题', 'PLAN_USER_DOMAIN', 'DWD', 2, 'SCHEDULE', 1, '2024-01-05 04:00:00', '2024-01-05 04:07:20', 440000, 'SUCCESS', 4, 3, 1, 0, 1,  90.00, '{"totalTables":1,"checkedColumns":3,"totalRows":1500000,"avgCompleteness":95.00,"avgUniqueness":100.00,"avgValidity":90.00}', '{"Completeness":95.00,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":90.00}', 0),
(20, 'EXEC20240106003', 3, '用户域质量专题', 'PLAN_USER_DOMAIN', 'DWD', 2, 'SCHEDULE', 1, '2024-01-06 04:00:00', '2024-01-06 04:08:00', 480000, 'SUCCESS', 4, 4, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":3,"totalRows":1502000,"avgCompleteness":99.50,"avgUniqueness":100.00,"avgValidity":100.00}', '{"Completeness":99.50,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":100.00}', 0),
(21, 'EXEC20240107003', 3, '用户域质量专题', 'PLAN_USER_DOMAIN', 'DWD', 2, 'SCHEDULE', 1, '2024-01-07 04:00:00', '2024-01-07 04:07:50', 470000, 'SUCCESS', 4, 4, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":3,"totalRows":1505000,"avgCompleteness":99.80,"avgUniqueness":100.00,"avgValidity":100.00}', '{"Completeness":99.80,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":100.00}', 0),
-- 2024-01-15~16 延续数据
(22, 'EXEC20240115001', 1, 'ODS层数据质量方案', 'PLAN_ODS_DAILY', 'ODS', 1, 'SCHEDULE', 1, '2024-01-15 02:00:00', '2024-01-15 02:05:23', 323000, 'SUCCESS', 2, 2, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":1,"totalRows":1520000,"avgCompleteness":99.80,"avgTimeliness":100.00}', '{"Completeness":100.00,"Uniqueness":null,"Accuracy":null,"Consistency":null,"Timeliness":100.00,"Validity":100.00}', 0),
(23, 'EXEC20240115002', 2, 'DWD层数据质量方案', 'PLAN_DWD_DAILY', 'DWD', 1, 'SCHEDULE', 1, '2024-01-15 03:00:00', '2024-01-15 03:12:45', 765000, 'SUCCESS', 4, 3, 1, 0, 2,  90.00, '{"totalTables":2,"checkedColumns":3,"totalRows":1520000,"avgCompleteness":95.00,"avgUniqueness":100.00,"avgValidity":90.00}', '{"Completeness":95.00,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":90.00}', 0),
(24, 'EXEC20240115003', 3, '用户域质量专题',     'PLAN_USER_DOMAIN', 'DWD', 2, 'SCHEDULE', 1, '2024-01-15 04:00:00', '2024-01-15 04:08:12', 492000, 'SUCCESS', 4, 3, 1, 0, 1,  90.00, '{"totalTables":1,"checkedColumns":3,"totalRows":1520000,"avgCompleteness":95.00,"avgUniqueness":100.00,"avgValidity":90.00}', '{"Completeness":95.00,"Uniqueness":100.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":90.00}', 0),
(25, 'EXEC20240116001', 1, 'ODS层数据质量方案', 'PLAN_ODS_DAILY', 'ODS', 1, 'SCHEDULE', 1, '2024-01-16 02:00:00', '2024-01-16 02:06:11', 371000, 'SUCCESS', 2, 2, 0, 0, 1, 100.00, '{"totalTables":1,"checkedColumns":1,"totalRows":1525000,"avgCompleteness":99.90,"avgTimeliness":100.00}', '{"Completeness":100.00,"Uniqueness":null,"Accuracy":null,"Consistency":null,"Timeliness":100.00,"Validity":100.00}', 0),
(26, 'EXEC20240116002', 2, 'DWD层数据质量方案', 'PLAN_DWD_DAILY', 'DWD', 1, 'SCHEDULE', 1, '2024-01-16 03:00:00', '2024-01-16 03:15:30', 930000, 'FAILED',  4, 2, 2, 0, 2,  68.00, '{"totalTables":2,"checkedColumns":3,"totalRows":1525000,"avgCompleteness":82.00,"avgUniqueness":85.00,"avgValidity":72.00}', '{"Completeness":82.00,"Uniqueness":85.00,"Accuracy":null,"Consistency":null,"Timeliness":null,"Validity":72.00}', 0);

-- 3.5 质检执行明细（各规则详细结果）
TRUNCATE TABLE `dqc_execution_detail`;
INSERT INTO `dqc_execution_detail` (`id`, `execution_id`, `execution_no`, `rule_id`, `rule_name`, `rule_code`, `rule_type`, `rule_strength`, `dimensions`, `target_ds_id`, `target_table`, `target_column`, `start_time`, `end_time`, `elapsed_ms`, `status`, `total_count`, `error_count`, `pass_count`, `actual_value`, `threshold_min`, `threshold_max`, `quality_score`, `error_detail`) VALUES
-- ODS 01-01 执行明细
(1,  1,  'EXEC20240101001', 7,  'ODS层源表更新时效',   'RULE_ODS_UPDATE_TIMELINESS', 'TABLE_UPDATE_TIMELINESS', 'STRONG', '["Timeliness"]',    1, 'ods_src_users', NULL,       '2024-01-01 02:00:00', '2024-01-01 02:04:12', 252000, 'SUCCESS', 1480000,       0,      1480000,   3.50,    NULL,  24, 100.00, NULL),
-- ODS 01-04 执行明细（失败：更新时效超时）
(3,  4,  'EXEC20240104001', 7,  'ODS层源表更新时效',   'RULE_ODS_UPDATE_TIMELINESS', 'TABLE_UPDATE_TIMELINESS', 'STRONG', '["Timeliness"]',    1, 'ods_src_users', NULL,       '2024-01-04 02:00:00', '2024-01-04 02:08:00', 480000, 'FAILED', 1495000,       0,      1495000,  28.00,   NULL,  24,   0.00, '数据更新延迟超过24小时阈值，当前28小时'),
-- DWD 01-01 执行明细
(4,  8,  'EXEC20240101002', 1,  '用户表行数非零校验',  'RULE_USER_ROW_COUNT',    'ROW_COUNT_NOT_ZERO',   'STRONG', '["Completeness"]', 2, 'dim_users',      NULL,       '2024-01-01 03:00:00', '2024-01-01 03:02:30', 150000, 'SUCCESS', 1480000,       0,      1480000, 1480000.000000, 1, NULL, 100.00, NULL),
(5,  8,  'EXEC20240101002', 2,  '用户手机号空值率校验','RULE_USER_PHONE_NULL',   'NULL_CHECK',           'WEAK',   '["Completeness"]', 2, 'dim_users',      'user_phone','2024-01-01 03:02:30', '2024-01-01 03:05:15', 165000, 'SUCCESS', 1480000,    14800,   1465200,    1.00,    NULL,  5,  100.00, NULL),
(6,  8,  'EXEC20240101002', 4,  '身份证号格式校验',    'RULE_IDCARD_FORMAT',    'REGEX_IDCARD',         'STRONG', '["Validity"]',    2, 'dim_users',      'id_card',  '2024-01-01 03:05:15', '2024-01-01 03:08:40', 205000, 'SUCCESS', 1184000,      118,    1183882,    0.01,    NULL,  0.1, 100.00, NULL),
(7,  8,  'EXEC20240101002', 8,  '用户唯一性校验',      'RULE_USER_UNIQUE',      'UNIQUE_CHECK',        'STRONG', '["Uniqueness"]',  2, 'dim_users',      'user_id',  '2024-01-01 03:08:40', '2024-01-01 03:12:15', 215000, 'SUCCESS', 1480000,       0,      1480000,    0.000000, NULL,  0,  100.00, NULL),
-- DWD 01-04 执行明细（失败）
(8,  11, 'EXEC20240104002', 1,  '用户表行数非零校验',  'RULE_USER_ROW_COUNT',    'ROW_COUNT_NOT_ZERO',   'STRONG', '["Completeness"]', 2, 'dim_users',      NULL,       '2024-01-04 03:00:00', '2024-01-04 03:03:00', 180000, 'SUCCESS', 1495000,       0,      1495000, 1495000.000000, 1, NULL, 100.00, NULL),
(9,  11, 'EXEC20240104002', 2,  '用户手机号空值率校验','RULE_USER_PHONE_NULL',   'NULL_CHECK',           'WEAK',   '["Completeness"]', 2, 'dim_users',      'user_phone','2024-01-04 03:03:00', '2024-01-04 03:06:00', 180000, 'FAILED', 1495000,    74750,   1420250,    5.00,    NULL,  5,    0.00, '手机号空值率 5.00% 达到阈值 5.00%'),
(10, 11, 'EXEC20240104002', 4,  '身份证号格式校验',    'RULE_IDCARD_FORMAT',    'REGEX_IDCARD',         'STRONG', '["Validity"]',    2, 'dim_users',      'id_card',  '2024-01-04 03:06:00', '2024-01-04 03:09:30', 210000, 'FAILED', 1495000,    22425,   1472575,    1.50,    NULL,  0.1,   0.00, '身份证格式不合规 22425 条，占比 1.50%，超过阈值 0.1%'),
(11, 11, 'EXEC20240104002', 8,  '用户唯一性校验',      'RULE_USER_UNIQUE',      'UNIQUE_CHECK',        'STRONG', '["Uniqueness"]',  2, 'dim_users',      'user_id',  '2024-01-04 03:09:30', '2024-01-04 03:13:00', 210000, 'FAILED', 1495000,      150,     1494850,    0.01,    NULL,  0,    0.00, '发现 150 条重复 user_id'),
-- 用户域 01-04 执行明细（失败）
(12, 18, 'EXEC20240104003', 1,  '用户表行数非零校验',  'RULE_USER_ROW_COUNT',    'ROW_COUNT_NOT_ZERO',   'STRONG', '["Completeness"]', 2, 'dim_users',      NULL,       '2024-01-04 04:00:00', '2024-01-04 04:02:30', 150000, 'SUCCESS', 1495000,       0,      1495000, 1495000.000000, 1, NULL, 100.00, NULL),
(13, 18, 'EXEC20240104003', 2,  '用户手机号空值率校验','RULE_USER_PHONE_NULL',   'NULL_CHECK',           'WEAK',   '["Completeness"]', 2, 'dim_users',      'user_phone','2024-01-04 04:02:30', '2024-01-04 04:05:30', 180000, 'FAILED', 1495000,    74750,   1420250,    5.00,    NULL,  5,    0.00, '手机号空值率 5.00% 达到阈值 5.00%'),
(14, 18, 'EXEC20240104003', 4,  '身份证号格式校验',    'RULE_IDCARD_FORMAT',    'REGEX_IDCARD',         'STRONG', '["Validity"]',    2, 'dim_users',      'id_card',  '2024-01-04 04:05:30', '2024-01-04 04:08:50', 200000, 'FAILED', 1495000,    22425,   1472575,    1.50,    NULL,  0.1,   0.00, '身份证格式不合规 22425 条，占比 1.50%，超过阈值 0.1%'),
(15, 18, 'EXEC20240104003', 8,  '用户唯一性校验',      'RULE_USER_UNIQUE',      'UNIQUE_CHECK',        'STRONG', '["Uniqueness"]',  2, 'dim_users',      'user_id',  '2024-01-04 04:08:50', '2024-01-04 04:11:30', 160000, 'FAILED', 1495000,      150,     1494850,    0.01,    NULL,  0,    0.00, '发现 150 条重复 user_id'),
-- 2024-01-15~16 执行明细
(16, 22, 'EXEC20240115001', 7,  'ODS层源表更新时效',   'RULE_ODS_UPDATE_TIMELINESS','TABLE_UPDATE_TIMELINESS','STRONG','["Timeliness"]',1,'ods_src_users','update_time','2024-01-15 03:00:00','2024-01-15 03:05:10',310000,'SUCCESS',1520000,15200,1504800,1.00,NULL,5,100.00,NULL),
(17, 23, 'EXEC20240115002', 1,  '用户表行数非零校验',  'RULE_USER_ROW_COUNT',   'ROW_COUNT_NOT_ZERO',   'STRONG', '["Completeness"]', 2, 'dim_users',      NULL,       '2024-01-15 03:00:00', '2024-01-15 03:02:30', 150000, 'SUCCESS', 1520000,       0,      1520000, 1520000.000000, 1, NULL, 100.00, NULL),
(18, 23, 'EXEC20240115002', 2,  '用户手机号空值率校验','RULE_USER_PHONE_NULL',  'NULL_CHECK',           'WEAK',   '["Completeness"]', 2, 'dim_users',      'user_phone','2024-01-15 03:02:30', '2024-01-15 03:05:15', 165000, 'SUCCESS', 1520000,    15200,   1504800,    1.00,    NULL,  5,  100.00, NULL),
(19, 23, 'EXEC20240115002', 4,  '身份证号格式校验',     'RULE_IDCARD_FORMAT',   'REGEX_IDCARD',         'STRONG', '["Validity"]',    2, 'dim_users',      'id_card',  '2024-01-15 03:05:15', '2024-01-15 03:08:45', 210000, 'SUCCESS', 1216000,      1216,   1214784,    0.10,    NULL,  0.1, 100.00, NULL),
(20, 23, 'EXEC20240115002', 8,  '用户唯一性校验',       'RULE_USER_UNIQUE',     'UNIQUE_CHECK',        'STRONG', '["Uniqueness"]',  2, 'dim_users',      'user_id',  '2024-01-15 03:08:45', '2024-01-15 03:12:45', 240000, 'SUCCESS', 1520000,       0,      1520000,    0.000000, NULL,  0,  100.00, NULL),
(21, 26, 'EXEC20240116002', 1,  '用户表行数非零校验',   'RULE_USER_ROW_COUNT',   'ROW_COUNT_NOT_ZERO',   'STRONG', '["Completeness"]', 2, 'dim_users',      NULL,       '2024-01-16 03:00:00', '2024-01-16 03:03:00', 180000, 'SUCCESS', 1525000,       0,      1525000, 1525000.000000, 1, NULL, 100.00, NULL),
(22, 26, 'EXEC20240116002', 2,  '用户手机号空值率校验', 'RULE_USER_PHONE_NULL',  'NULL_CHECK',           'WEAK',   '["Completeness"]', 2, 'dim_users',      'user_phone','2024-01-16 03:03:00', '2024-01-16 03:07:00', 240000, 'FAILED', 1525000,    152500,  1372500,   10.00,   NULL,  5,    0.00, '手机号空值率 10.00% 超过阈值 5%'),
(23, 26, 'EXEC20240116002', 4,  '身份证号格式校验',     'RULE_IDCARD_FORMAT',   'REGEX_IDCARD',         'STRONG', '["Validity"]',    2, 'dim_users',      'id_card',  '2024-01-16 03:07:00', '2024-01-16 03:11:00', 240000, 'FAILED', 1220000,     18300,   1201700,    1.50,    NULL,  0.1,   0.00, '身份证格式不合规 18300 条，占比 1.50%，超过阈值 0.1%'),
(24, 26, 'EXEC20240116002', 8,  '用户唯一性校验',       'RULE_USER_UNIQUE',     'UNIQUE_CHECK',        'STRONG', '["Uniqueness"]',  2, 'dim_users',      'user_id',  '2024-01-16 03:11:00', '2024-01-16 03:15:30', 270000, 'FAILED', 1525000,      305,     1524695,    0.02,    NULL,  0,    0.00, '发现 305 条重复 user_id');

-- 3.6 质量评分历史（按天、按表的多维度趋势）
TRUNCATE TABLE `dqc_quality_score`;
INSERT INTO `dqc_quality_score` (`check_date`, `target_ds_id`, `target_table`, `layer_code`, `dept_id`, `completeness_score`, `uniqueness_score`, `accuracy_score`, `consistency_score`, `timeliness_score`, `validity_score`, `overall_score`, `rule_pass_rate`, `rule_total_count`, `rule_pass_count`, `rule_fail_count`) VALUES
-- dim_users 表（DWD层）2024-01-01 ~ 01-16 每日趋势
('2024-01-01', 2, 'dim_users',       'DWD', 1,  98.00, 100.00,  NULL,   NULL,  NULL,  100.00,  99.00, 100.00,  4,  4,  0),
('2024-01-02', 2, 'dim_users',       'DWD', 1,  94.00, 100.00,  NULL,   NULL,  NULL,   90.00,  94.67,  75.00,  4,  3,  1),
('2024-01-03', 2, 'dim_users',       'DWD', 1,  98.50, 100.00,  NULL,   NULL,  NULL,  100.00,  99.50, 100.00,  4,  4,  0),
('2024-01-04', 2, 'dim_users',       'DWD', 1,  80.00,  80.00,  NULL,   NULL,  NULL,   70.00,  76.67,  50.00,  4,  2,  2),
('2024-01-05', 2, 'dim_users',       'DWD', 1,  95.00, 100.00,  NULL,   NULL,  NULL,   90.00,  95.00,  75.00,  4,  3,  1),
('2024-01-06', 2, 'dim_users',       'DWD', 1,  99.50, 100.00,  NULL,   NULL,  NULL,  100.00,  99.83, 100.00,  4,  4,  0),
('2024-01-07', 2, 'dim_users',       'DWD', 1,  99.80, 100.00,  NULL,   NULL,  NULL,  100.00,  99.93, 100.00,  4,  4,  0),
('2024-01-08', 2, 'dim_users',       'DWD', 1,  99.50, 100.00,  NULL,   NULL,  NULL,  100.00,  99.83, 100.00,  4,  4,  0),
('2024-01-09', 2, 'dim_users',       'DWD', 1,  99.80, 100.00,  NULL,   NULL,  NULL,  100.00,  99.93, 100.00,  4,  4,  0),
('2024-01-10', 2, 'dim_users',       'DWD', 1,  99.00, 100.00,  NULL,   NULL,  NULL,  100.00,  99.67, 100.00,  4,  4,  0),
('2024-01-11', 2, 'dim_users',       'DWD', 1,  99.20, 100.00,  NULL,   NULL,  NULL,  100.00,  99.73, 100.00,  4,  4,  0),
('2024-01-12', 2, 'dim_users',       'DWD', 1,  99.50, 100.00,  NULL,   NULL,  NULL,  100.00,  99.83, 100.00,  4,  4,  0),
('2024-01-13', 2, 'dim_users',       'DWD', 1,  99.00, 100.00,  NULL,   NULL,  NULL,  100.00,  99.67, 100.00,  4,  4,  0),
('2024-01-14', 2, 'dim_users',       'DWD', 1,  99.30, 100.00,  NULL,   NULL,  NULL,  100.00,  99.77, 100.00,  4,  4,  0),
('2024-01-15', 2, 'dim_users',       'DWD', 1,  95.00, 100.00,  NULL,   NULL,  NULL,   90.00,  95.00,  75.00,  4,  3,  1),
('2024-01-16', 2, 'dim_users',       'DWD', 1,  82.00,  85.00,  NULL,   NULL,  NULL,   72.00,  79.67,  50.00,  4,  2,  2),
-- fact_orders 表（DWD层）趋势
('2024-01-01', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-02', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-03', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-04', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-05', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-06', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-07', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-08', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-09', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-10', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-11', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-12', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-13', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-14', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-15', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
('2024-01-16', 2, 'fact_orders',     'DWD', 1, 100.00,  NULL,   NULL,   NULL,  NULL,   NULL,  100.00, 100.00,  2,  2,  0),
-- ods_src_users 表（ODS层）
('2024-01-01', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-02', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-03', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-04', 1, 'ods_src_users',   'ODS', 1,  60.00,  NULL,   NULL,   NULL,  50.00,  NULL,   55.00,  50.00,  1,  1,  1),
('2024-01-05', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-06', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-07', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-08', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-09', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-10', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-11', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-12', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-13', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-14', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-15', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
('2024-01-16', 1, 'ods_src_users',   'ODS', 1, 100.00,  NULL,   NULL,   NULL, 100.00,  NULL,  100.00, 100.00,  1,  1,  0),
-- 按层级（ALL）的全局汇总趋势
('2024-01-01', NULL, NULL,           'ALL', 1,  98.67, 100.00,  NULL,   NULL, 100.00, 100.00,  99.67, 100.00,  7,  7,  0),
('2024-01-02', NULL, NULL,           'ALL', 1,  96.00, 100.00,  NULL,   NULL, 100.00,  90.00,  96.67,  85.71,  7,  6,  1),
('2024-01-03', NULL, NULL,           'ALL', 1,  99.00, 100.00,  NULL,   NULL, 100.00, 100.00,  99.67, 100.00,  7,  7,  0),
('2024-01-04', NULL, NULL,           'ALL', 1,  80.00,  80.00,  NULL,   NULL,  50.00,  70.00,  70.00,  42.86,  7,  3,  4),
('2024-01-05', NULL, NULL,           'ALL', 1,  98.33, 100.00,  NULL,   NULL, 100.00,  90.00,  97.67,  85.71,  7,  6,  1),
('2024-01-06', NULL, NULL,           'ALL', 1,  99.67, 100.00,  NULL,   NULL, 100.00, 100.00,  99.89, 100.00,  7,  7,  0),
('2024-01-07', NULL, NULL,           'ALL', 1,  99.83, 100.00,  NULL,   NULL, 100.00, 100.00,  99.96, 100.00,  7,  7,  0),
('2024-01-08', NULL, NULL,           'ALL', 1,  99.67, 100.00,  NULL,   NULL, 100.00, 100.00,  99.89, 100.00,  7,  7,  0),
('2024-01-09', NULL, NULL,           'ALL', 1,  99.83, 100.00,  NULL,   NULL, 100.00, 100.00,  99.96, 100.00,  7,  7,  0),
('2024-01-10', NULL, NULL,           'ALL', 1,  99.50, 100.00,  NULL,   NULL, 100.00, 100.00,  99.83, 100.00,  7,  7,  0),
('2024-01-11', NULL, NULL,           'ALL', 1,  99.60, 100.00,  NULL,   NULL, 100.00, 100.00,  99.90, 100.00,  7,  7,  0),
('2024-01-12', NULL, NULL,           'ALL', 1,  99.75, 100.00,  NULL,   NULL, 100.00, 100.00,  99.94, 100.00,  7,  7,  0),
('2024-01-13', NULL, NULL,           'ALL', 1,  99.50, 100.00,  NULL,   NULL, 100.00, 100.00,  99.83, 100.00,  7,  7,  0),
('2024-01-14', NULL, NULL,           'ALL', 1,  99.65, 100.00,  NULL,   NULL, 100.00, 100.00,  99.91, 100.00,  7,  7,  0),
('2024-01-15', NULL, NULL,           'ALL', 1,  98.33, 100.00,  NULL,   NULL, 100.00,  90.00,  97.67,  85.71,  7,  6,  1),
('2024-01-16', NULL, NULL,           'ALL', 1,  94.00,  91.67,  NULL,   NULL, 100.00,  72.00,  89.42,  57.14,  7,  4,  3);

-- ============================================================
-- 4. 数据探查 - 探查任务 / 统计结果 / 快照
-- ============================================================

-- 4.1 探查任务
TRUNCATE TABLE `dprofile_profile_task`;
INSERT INTO `dprofile_profile_task` (`id`, `task_name`, `task_code`, `task_desc`, `target_ds_id`, `target_table`, `target_columns`, `profile_level`, `trigger_type`, `trigger_cron`, `timeout_minutes`, `status`) VALUES
(1, 'ODS用户源表探查',  'PROFILE_ODS_USERS',  '对ODS层用户源表进行全量探查',   1, 'ods_src_users', NULL, 'TABLE_AND_COLUMN', 'SCHEDULE', '0 6 * * *', 60, 'PUBLISHED'),
(2, 'DWD用户维度表探查','PROFILE_DWD_USERS', '对DWD层用户维度表进行探查',      2, 'dim_users',     NULL, 'TABLE_AND_COLUMN', 'SCHEDULE', '0 7 * * *', 60, 'PUBLISHED'),
(3, 'DWD订单事实表探查','PROFILE_DWD_ORDERS','对DWD层订单事实表进行探查',       2, 'fact_orders',   NULL, 'TABLE_AND_COLUMN', 'SCHEDULE', '0 8 * * *', 90, 'PUBLISHED');

-- 4.2 表级统计结果
TRUNCATE TABLE `dprofile_table_stats`;
INSERT INTO `dprofile_table_stats` (`id`, `task_id`, `execution_id`, `ds_id`, `table_name`, `profile_time`, `row_count`, `column_count`, `storage_bytes`, `table_comment`, `update_time`, `increment_rows`) VALUES
(1,   1, 1,   1, 'ods_src_users', '2024-01-15 06:00:00', 1500000, 25, 524288000, 'ODS用户源数据表', '2024-01-15 05:30:00',  5000),
(2,   2, 2,   2, 'dim_users',     '2024-01-15 07:00:00', 1000000, 30, 314572800, '用户维度表',     '2024-01-15 06:00:00',  2000),
(3,   3, 3,   2, 'fact_orders',   '2024-01-15 08:00:00', 5000000, 35, 2097152000,'订单事实表',     '2024-01-15 07:00:00', 15000),
-- 今日数据（用于概览统计）
(100, 1, 100, 1, 'ods_src_users', NOW(),                1500000, 25, 524288000, 'ODS用户源数据表', NOW(),                5000),
(101, 2, 101, 2, 'dim_users',     NOW(),                1000000, 30, 314572800, '用户维度表',     NOW(),                2000),
(102, 3, 102, 2, 'fact_orders',   NOW(),                5000000, 35, 2097152000,'订单事实表',     NOW(),               15000)
ON DUPLICATE KEY UPDATE `profile_time` = NOW();

-- 4.3 列级统计结果
TRUNCATE TABLE `dprofile_column_stats`;
INSERT INTO `dprofile_column_stats` (`id`, `table_stats_id`, `execution_id`, `ds_id`, `table_name`, `column_name`, `profile_time`, `data_type`, `nullable`, `total_count`, `null_count`, `null_rate`, `unique_count`, `unique_rate`, `min_value`, `max_value`, `avg_value`, `median_value`, `std_dev`, `min_length`, `max_length`, `avg_length`, `top_values`, `histogram`, `outlier_count`, `zero_count`, `zero_rate`, `negative_count`) VALUES
(1,  2, 2, 2, 'dim_users',     'user_id',       '2024-01-15 07:00:00', 'BIGINT',   0, 1000000,       0, 0.0000, 1000000, 1.0000, '6871947673600000001', '6872947673600000000', 35.50,        NULL,  NULL,        19, 19, 19.00, '[6871947673600000001,6871947673600000002,6871947673600000003]', NULL, NULL, 0, 0.0000, 0),
(2,  2, 2, 2, 'dim_users',     'user_name',     '2024-01-15 07:00:00', 'VARCHAR',  1, 1000000,    5000, 0.5000,  985000, 0.9850, NULL,                 NULL,                 NULL,        NULL, NULL,  2, 50,  8.50, '[{"value":"张三","count":15230},{"value":"李四","count":14890}]',          NULL, NULL, 0, 0.0000, 0),
(3,  2, 2, 2, 'dim_users',     'user_phone',    '2024-01-15 07:00:00', 'VARCHAR',  1, 1000000,   15000, 1.5000,  985000, 0.9850, NULL,                 NULL,                 NULL,        NULL, NULL, 11, 11, 11.00, '[13800138001,13800138002,13800138003]',                                NULL, NULL, 0, 0.0000, 0),
(4,  2, 2, 2, 'dim_users',     'age',           '2024-01-15 07:00:00', 'INT',      1, 1000000,    8000, 0.8000,      85, 0.0001, '18',                '85',                35.50,        NULL, NULL,        NULL, NULL, NULL, NULL,        NULL, NULL, 0, 0.0000, 0),
(5,  2, 2, 2, 'dim_users',     'id_card',       '2024-01-15 07:00:00', 'VARCHAR',  1, 1000000,  200000,20.0000, 800000, 0.8000, NULL,                 NULL,                 NULL,        NULL, NULL, 18, 18, 18.00, '[110101199001011234,110101199001011235]',                            NULL, NULL, 0, 0.0000, 0),
(6,  3, 3, 2, 'fact_orders',   'order_id',      '2024-01-15 08:00:00', 'BIGINT',   0, 5000000,       0, 0.0000, 5000000, 1.0000, '6872947673600000001', '6873947673600000000', NULL,        NULL, NULL,       19, 19, 19.00, '[6872947673600000001]',                                               NULL, NULL, 0, 0.0000, 0),
(7,  3, 3, 2, 'fact_orders',   'order_amount',  '2024-01-15 08:00:00', 'DECIMAL',  0, 5000000,       0, 0.0000,  500000, 0.1000, '0.01',             '99999.99',            156.78,     NULL, NULL,     NULL, NULL, NULL, NULL,        NULL, NULL, 0, 0.0000, 0),
(8,  3, 3, 2, 'fact_orders',   'order_status',  '2024-01-15 08:00:00', 'VARCHAR',  0, 5000000,       0, 0.0000,       5, 0.0000, NULL,                 NULL,                 NULL,        NULL, NULL,  7, 10,  8.00, '[{"value":"COMPLETED","count":2500000},{"value":"PAID","count":1500000}]', NULL, NULL, 0, 0.0000, 0);

-- 4.4 探查快照
TRUNCATE TABLE `dprofile_snapshot`;
INSERT INTO `dprofile_snapshot` (`id`, `snapshot_name`, `snapshot_code`, `target_ds_id`, `target_table`, `snapshot_desc`, `table_stats_id`, `column_count`, `create_user`) VALUES
(1, 'ODS用户源表快照-20240115', 'SNAP_ODS_USERS_20240115', 1, 'ods_src_users', 'ODS层用户源表探查快照（2024-01-15）', 1, 25, 1),
(2, 'DWD用户维度表快照-20240115','SNAP_DWD_USERS_20240115', 2, 'dim_users',     'DWD层用户维度表探查快照（2024-01-15）', 2, 30, 1),
(3, 'DWD订单事实表快照-20240115','SNAP_DWD_ORDERS_20240115',2, 'fact_orders',   'DWD层订单事实表探查快照（2024-01-15）', 3, 35, 1);

-- 4.5 快照比对结果
TRUNCATE TABLE `dprofile_compare_result`;
INSERT INTO `dprofile_compare_result` (`id`, `compare_name`, `compare_code`, `snapshot_a_id`, `snapshot_b_id`, `compare_type`, `compare_result`, `row_count_change`, `diff_count`, `diff_detail`) VALUES
(1, 'ODS用户表日环比', 'COMPARE_ODS_USERS_D1', 1, 1, 'DAILY', '{"table_stats":{"row_count_change":5000,"column_count_change":0},"column_changes":[]}', 5000, 0, '行数增加5000，无结构变化');

-- ============================================================
-- 5. 数据监控 - 指标定义 / 告警规则 / 告警记录
-- ============================================================

-- 5.1 监控指标定义
TRUNCATE TABLE `monitor_metric_def`;
INSERT INTO `monitor_metric_def` (`id`, `metric_code`, `metric_name`, `metric_desc`, `metric_type`, `unit`, `data_type`, `query_expr`, `aggregation_type`, `target_ds_id`, `dept_id`, `enabled`) VALUES
(1, 'METRIC_TASK_DAILY_RUN',       '每日任务执行数',   '每日调度的ETL任务执行总次数',        'TASK',    '次', 'INT',     'SELECT COUNT(*) FROM dqc_execution WHERE DATE(create_time) = CURDATE()',              'COUNT', NULL, 1, 1),
(2, 'METRIC_QUALITY_SCORE_AVG',    '平均质量得分',     '当日所有质检执行的平均质量得分',    'QUALITY', '分', 'DECIMAL', 'SELECT AVG(quality_score) FROM dqc_execution WHERE DATE(create_time) = CURDATE()',   'AVG',   NULL, 1, 1),
(3, 'METRIC_QUALITY_PASS_RATE',    '规则通过率',       '当日质检规则的通过率',              'QUALITY', '%',  'DECIMAL', 'SELECT SUM(passed_rules) * 100.0 / SUM(total_rules) FROM dqc_execution WHERE DATE(create_time) = CURDATE()', 'AVG', NULL, 1, 1),
(4, 'METRIC_ALERT_COUNT',          '告警数量',         '当日产生的告警总数',                'QUALITY', '条', 'INT',     'SELECT COUNT(*) FROM monitor_alert_record WHERE DATE(create_time) = CURDATE()', 'COUNT', NULL, 1, 1),
(5, 'METRIC_TABLE_ROW_COUNT',      'ODS表行数',        'ODS层源表的总行数监控',            'METRIC',  '行', 'BIGINT',  'SELECT SUM(row_count) FROM dprofile_table_stats WHERE ds_id = 1',                    'SUM',   1,   1, 1),
(6, 'METRIC_TASK_SUCCESS_RATE',   '任务成功率',       'ETL任务执行成功率',                 'TASK',    '%',  'DECIMAL', 'SELECT SUM(CASE WHEN status = ''SUCCESS'' THEN 1 ELSE 0 END) * 100.0 / COUNT(*) FROM dqc_execution WHERE DATE(create_time) = CURDATE()', 'AVG', NULL, 1, 1);

-- 5.2 监控指标时序数据
TRUNCATE TABLE `monitor_metric_data`;
INSERT INTO `monitor_metric_data` (`metric_code`, `metric_value`, `metric_time`, `tags`) VALUES
('METRIC_TASK_DAILY_RUN',       12.000000, '2024-01-15 23:59:00', '{"layer":"ODS"}'),
('METRIC_QUALITY_SCORE_AVG',    95.830000, '2024-01-15 23:59:00', '{"layer":"ALL"}'),
('METRIC_QUALITY_PASS_RATE',     90.000000, '2024-01-15 23:59:00', '{"layer":"ALL"}'),
('METRIC_TABLE_ROW_COUNT',    1500000.000000, '2024-01-15 23:59:00', '{"ds_code":"mysql_ods","table":"ods_src_users"}'),
('METRIC_TASK_DAILY_RUN',       15.000000, '2024-01-16 23:59:00', '{"layer":"ODS"}'),
('METRIC_QUALITY_SCORE_AVG',    87.500000, '2024-01-16 23:59:00', '{"layer":"ALL"}'),
('METRIC_QUALITY_PASS_RATE',     76.670000, '2024-01-16 23:59:00', '{"layer":"ALL"}'),
('METRIC_ALERT_COUNT',           3.000000, '2024-01-16 23:59:00', '{"level":"WARN"}'),
('METRIC_TABLE_ROW_COUNT',    1505000.000000, '2024-01-16 23:59:00', '{"ds_code":"mysql_ods","table":"ods_src_users"}');

-- 5.3 告警规则
TRUNCATE TABLE `monitor_alert_rule`;
INSERT INTO `monitor_alert_rule` (`id`, `rule_name`, `rule_code`, `rule_type`, `target_type`, `target_id`, `condition_type`, `threshold_value`, `threshold_max_value`, `fluctuation_pct`, `comparison_type`, `consecutive_triggers`, `alert_level`, `alert_receivers`, `alert_title_template`, `alert_content_template`, `enabled`, `dept_id`) VALUES
(1, '质量得分低于90分告警',  'ALERT_QUALITY_SCORE_LOW',  'QUALITY',      'PLAN', 'PLAN_ODS_DAILY', 'LT',          90.000000, NULL, NULL, NULL, 2, 'WARN',      'admin@company.com,zhangsan@company.com', '【数据质量告警】${plan_name} 质量得分过低',     '方案: ${plan_name}\\n得分: ${trigger_value}\\n阈值: ${threshold}\\n请及时处理', 1, 1),
(2, 'ODS表行数日波动超20%', 'ALERT_ODS_ROW_FLUCTUATION','FLUCTUATION', 'TABLE', 'ods_src_users', 'FLUCTUATION', NULL,       NULL, 20.00, 'D_DAY',     1, 'ERROR',     'admin@company.com',                           '【数据波动告警】ODS表行数异常波动',             '表: ${target_name}\\n今日行数: ${trigger_value}\\n昨日行数: ${yesterday_value}\\n波动率: ${fluctuation_pct}%',  1, 1),
(3, '规则通过率低于80%',     'ALERT_PASS_RATE_LOW',       'QUALITY',      'PLAN', 'PLAN_DWD_DAILY', 'LT',          80.000000, NULL, NULL, NULL, 3, 'CRITICAL',  'admin@company.com,zhangsan@company.com,lisi@company.com', '【严重告警】规则通过率过低', '方案: ${plan_name}\\n通过率: ${trigger_value}%\\n请立即处理', 1, 1),
(4, '每日任务失败告警',       'ALERT_TASK_FAILURE',        'SYSTEM',       'TASK', NULL,             'GT',           0.000000,  NULL, NULL, NULL, 1, 'ERROR',     'admin@company.com',                           '【任务失败告警】',                             '失败任务数: ${trigger_value}\\n请检查任务执行情况', 1, 1);

-- 5.4 告警记录
TRUNCATE TABLE `monitor_alert_record`;
INSERT INTO `monitor_alert_record` (`id`, `alert_no`, `rule_id`, `rule_name`, `rule_code`, `alert_level`, `alert_title`, `alert_content`, `target_type`, `target_id`, `target_name`, `trigger_value`, `threshold_value`, `status`, `sent_time`, `read_time`, `read_user`) VALUES
(1,  'ALERT20240115001', 1, '质量得分低于90分告警', 'ALERT_QUALITY_SCORE_LOW',   'WARN',     '【数据质量告警】DWD层数据质量方案 质量得分过低', '方案: DWD层数据质量方案\\n得分: 85.00\\n阈值: 90.00\\n请及时处理', 'PLAN', '2', 'DWD层数据质量方案',  85.000000, 90.000000, 'READ',    '2024-01-15 03:13:00', '2024-01-15 09:00:00', 1),
(2,  'ALERT20240116001', 3, '规则通过率低于80%',   'ALERT_PASS_RATE_LOW',       'CRITICAL', '【严重告警】规则通过率过低',                    '方案: DWD层数据质量方案\\n通过率: 70.00%\\n请立即处理',                     'PLAN', '2', 'DWD层数据质量方案',  70.000000, 80.000000, 'SENT',    '2024-01-16 03:16:00', NULL,           NULL),
(3,  'ALERT20240116002', 1, '质量得分低于90分告警', 'ALERT_QUALITY_SCORE_LOW',   'WARN',     '【数据质量告警】DWD层数据质量方案 质量得分过低', '方案: DWD层数据质量方案\\n得分: 75.00\\n阈值: 90.00\\n请及时处理', 'PLAN', '2', 'DWD层数据质量方案',  75.000000, 90.000000, 'PENDING', '2024-01-16 03:16:00', NULL,           NULL),
-- 今日告警记录
(100, CONCAT('ALERT', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), '01'), 1, '质量得分低于90分告警', 'ALERT_QUALITY_SCORE_LOW', 'WARN', '【数据质量告警】DWD层数据质量方案 质量得分过低', '方案: DWD层数据质量方案\\n得分: 85.00\\n阈值: 90.00\\n请及时处理', 'PLAN', '2', 'DWD层数据质量方案', 85.000000, 90.000000, 'PENDING', NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE `status` = 'PENDING';

-- 5.5 任务执行记录
TRUNCATE TABLE `monitor_task_execution`;
INSERT INTO `monitor_task_execution` (`id`, `task_id`, `task_name`, `task_type`, `ds_id`, `trigger_type`, `trigger_user`, `start_time`, `end_time`, `elapsed_ms`, `status`, `progress`, `log_content`, `error_msg`) VALUES
(1,   NULL, 'ODS层数据质量方案', 'DQC', 1, 'SCHEDULE', 1, '2024-01-15 02:00:00', '2024-01-15 02:05:23', 323000, 'SUCCESS', 100, '执行5条规则，全部通过', NULL),
(2,   NULL, 'DWD层数据质量方案', 'DQC', 2, 'SCHEDULE', 1, '2024-01-15 03:00:00', '2024-01-15 03:12:45', 765000, 'SUCCESS', 100, '执行10条规则，全部通过', NULL),
(3,   NULL, 'ODS层数据质量方案', 'DQC', 1, 'SCHEDULE', 1, '2024-01-16 02:00:00', '2024-01-16 02:06:11', 371000, 'SUCCESS', 100, '执行5条规则，全部通过', NULL),
(4,   NULL, 'DWD层数据质量方案', 'DQC', 2, 'SCHEDULE', 1, '2024-01-16 03:00:00', '2024-01-16 03:15:30', 930000, 'FAILED',  100, '执行10条规则，7通过3失败', '规则 RULE_USER_PHONE_NULL 失败：手机号空值率 10.00% 超过阈值 5.00%'),
(5,   NULL, '用户域质量专题',     'DQC', 2, 'SCHEDULE', 1, '2024-01-15 04:00:00', '2024-01-15 04:08:12', 492000, 'SUCCESS', 100, '执行8条规则，7通过1失败', NULL),
-- 今日任务执行记录
(100, NULL, 'ODS层数据质量方案', 'DQC', 1, 'SCHEDULE', 1, NOW(),                NOW(),                323000, 'SUCCESS', 100, '执行5条规则，全部通过', NULL),
(101, NULL, 'DWD层数据质量方案', 'DQC', 2, 'SCHEDULE', 1, NOW(),                NOW(),                765000, 'SUCCESS', 100, '执行10条规则，全部通过', NULL),
(102, NULL, 'ADS层数据质量方案', 'DQC', 2, 'MANUAL',   1, NOW(),                NOW(),                500000, 'RUNNING',  60, '正在执行中...', NULL),
(103, NULL, 'ODS层数据质量方案', 'DQC', 1, 'SCHEDULE', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 55 MINUTE), 330000, 'SUCCESS', 100, '执行5条规则，全部通过', NULL)
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`), `create_time` = NOW();

-- ============================================================
-- 6. 数据治理 - 元数据 / 字段 / 血缘 / 资产目录 / 术语库 / 数据标准
-- ============================================================

-- 6.1 元数据（资产注册）
TRUNCATE TABLE `gov_metadata`;
INSERT INTO `gov_metadata` (`id`, `ds_id`, `table_name`, `table_alias`, `table_comment`, `table_type`, `data_layer`, `data_domain`, `biz_domain`, `lifecycle_days`, `is_partitioned`, `partition_column`, `storage_bytes`, `row_count`, `sensitivity_level`, `owner_id`, `dept_id`, `tags`, `last_profiled_at`, `status`) VALUES
(1, 1, 'ods_src_users',  'ODS用户源表',     'ODS层用户原始数据表，承接CRM系统用户数据',            'TABLE', 'ODS', 'DOMAIN_USER',   '客户域', 90,  1, 'etl_date',  524288000,  1500000, 'NORMAL',         2, 2, 'ODS,用户,贴源',    '2024-01-15 06:00:00', 'ACTIVE'),
(2, 2, 'dim_users',      '用户维度表',      '用户维度表，统一用户视图，包含用户基本信息、属性',     'TABLE', 'DWD', 'DOMAIN_USER',   '客户域', 3650, 1, 'dw_date',   314572800,  1000000, 'SENSITIVE',       2, 2, 'DWD,维度表,用户,PII', '2024-01-15 07:00:00', 'ACTIVE'),
(3, 2, 'fact_orders',    '订单事实表',      '订单事实表，记录所有订单明细数据',                      'TABLE', 'DWD', 'DOMAIN_ORDER',  '订单域', 3650, 1, 'dw_date',  2097152000, 5000000, 'INNER',           3, 3, 'DWD,事实表,订单',  '2024-01-15 08:00:00', 'ACTIVE'),
(4, 3, 'dws_user_profile','用户画像宽表',    '用户画像宽表，汇总用户行为和属性特征',                 'TABLE', 'DWS', 'DOMAIN_USER',   '客户域', 1800, 1, 'dw_date',   524288000,  1000000, 'SENSITIVE',       2, 2, 'DWS,用户,画像',    '2024-01-15 09:00:00', 'ACTIVE'),
(5, 3, 'dws_order_stats', '订单汇总统计表',  '按日/渠道/商品维度的订单汇总统计',                     'TABLE', 'DWS', 'DOMAIN_ORDER',  '订单域', 1800, 1, 'stat_date', 209715200,   500000, 'INNER',           3, 3, 'DWS,汇总,订单',    '2024-01-15 09:30:00', 'ACTIVE'),
(6, 4, 'ads_user_analysis','用户分析报表',   '面向业务的用户分析报表',                               'TABLE', 'ADS', 'DOMAIN_USER',   '客户域', 730,  0, NULL,       104857600,   100000, 'NORMAL',         2, 2, 'ADS,报表,用户',    NULL,                 'ACTIVE');

-- 6.2 元数据字段
TRUNCATE TABLE `gov_metadata_column`;
INSERT INTO `gov_metadata_column` (`id`, `metadata_id`, `column_name`, `column_alias`, `column_comment`, `data_type`, `is_nullable`, `column_key`, `default_value`, `is_primary_key`, `is_foreign_key`, `is_sensitive`, `sensitivity_level`, `sort_order`) VALUES
-- dim_users 字段
(1,  2, 'user_id',        '用户ID',    '用户唯一标识，雪花算法生成',    'BIGINT',          0, 'PRI', NULL, 1, 0, 0, 'NORMAL',           1),
(2,  2, 'user_name',      '用户姓名',  '用户真实姓名',                 'VARCHAR',         1, 'MUL', NULL, 0, 0, 0, 'NORMAL',           2),
(3,  2, 'user_phone',     '手机号码',  '用户注册手机号',               'VARCHAR',         1, 'MUL', NULL, 0, 0, 1, 'SENSITIVE',        3),
(4,  2, 'email',          '邮箱',      '用户邮箱地址',                 'VARCHAR',         1, 'MUL', NULL, 0, 0, 1, 'SENSITIVE',        4),
(5,  2, 'age',            '年龄',      '用户年龄',                     'INT',             1, NULL, NULL, 0, 0, 0, 'NORMAL',           5),
(6,  2, 'gender',         '性别',      '用户性别：0-未知，1-男，2-女',  'TINYINT',         1, NULL, '0', 0, 0, 0, 'NORMAL',           6),
(7,  2, 'id_card',        '身份证号',  '用户身份证号（脱敏存储）',       'VARCHAR',         1, 'MUL', NULL, 0, 0, 1, 'HIGHLY_SENSITIVE', 7),
(8,  2, 'register_time',  '注册时间',  '用户在CRM系统的注册时间',       'DATETIME',        0, NULL, NULL, 0, 0, 0, 'NORMAL',           8),
(9,  2, 'dw_date',        '数据日期',  '数据仓库分区日期',             'DATE',            0, 'MUL', NULL, 0, 0, 0, 'NORMAL',           9),
-- fact_orders 字段
(10, 3, 'order_id',       '订单ID',    '订单唯一标识',                 'BIGINT',          0, 'PRI', NULL, 1, 0, 0, 'NORMAL',   1),
(11, 3, 'user_id',        '用户ID',    '下单用户ID',                   'BIGINT',          0, 'MUL', NULL, 0, 1, 0, 'NORMAL',   2),
(12, 3, 'order_amount',   '订单金额',  '订单实际支付金额（元）',         'DECIMAL(10,2)',   0, NULL, '0.00', 0, 0, 0, 'INNER',    3),
(13, 3, 'order_status',   '订单状态',  '订单状态：PAID/SHIPPED/COMPLETED/CANCELLED', 'VARCHAR', 0, 'MUL', NULL, 0, 0, 0, 'NORMAL', 4),
(14, 3, 'order_time',     '下单时间',  '用户下单时间',                   'DATETIME',        0, 'MUL', NULL, 0, 0, 0, 'NORMAL',   5),
(15, 3, 'pay_time',       '支付时间',  '订单支付时间',                   'DATETIME',        1, NULL, NULL, 0, 0, 0, 'INNER',    6),
(16, 3, 'dw_date',        '数据日期',  '数据仓库分区日期',               'DATE',            0, 'MUL', NULL, 0, 0, 0, 'NORMAL',   7);

-- 6.3 数据血缘（表级+字段级）
TRUNCATE TABLE `gov_lineage`;
INSERT INTO `gov_lineage` (`id`, `lineage_type`, `source_ds_id`, `source_table`, `source_column`, `target_ds_id`, `target_table`, `target_column`, `transform_type`, `lineage_source`, `status`) VALUES
-- ODS -> DWD 表级血缘
(1,  'TABLE',  1, 'ods_src_users',  NULL,             2, 'dim_users',      NULL, 'DIRECT', 'AUTO_PARSER', 'ACTIVE'),
(2,  'TABLE',  1, 'ods_src_orders', NULL,             2, 'fact_orders',    NULL, 'DIRECT', 'AUTO_PARSER', 'ACTIVE'),
-- DWD -> DWS 表级血缘
(3,  'TABLE',  2, 'dim_users',      NULL,             3, 'dws_user_profile', NULL, 'DIRECT', 'AUTO_PARSER', 'ACTIVE'),
(4,  'TABLE',  2, 'fact_orders',    NULL,             3, 'dws_order_stats',  NULL, 'SUM',    'AUTO_PARSER', 'ACTIVE'),
-- DWS -> ADS 表级血缘
(5,  'TABLE',  3, 'dws_user_profile', NULL,            4, 'ads_user_analysis', NULL, 'DIRECT', 'MANUAL', 'ACTIVE'),
-- dim_users 字段级血缘
(6,  'COLUMN', 1, 'ods_src_users',  'user_id',        2, 'dim_users',      'user_id',     'DIRECT',       'AUTO_PARSER', 'ACTIVE'),
(7,  'COLUMN', 1, 'ods_src_users',  'user_name',      2, 'dim_users',      'user_name',   'DIRECT',       'AUTO_PARSER', 'ACTIVE'),
(8,  'COLUMN', 1, 'ods_src_users',  'phone',          2, 'dim_users',      'user_phone',  'DIRECT',       'AUTO_PARSER', 'ACTIVE'),
(9,  'COLUMN', 1, 'ods_src_users',  'birth_date',     2, 'dim_users',      'age',         'CUSTOM_EXPR', 'AUTO_PARSER', 'ACTIVE'),
-- fact_orders 字段级血缘
(10, 'COLUMN', 1, 'ods_src_orders', 'order_id',       2, 'fact_orders',    'order_id',    'DIRECT',       'AUTO_PARSER', 'ACTIVE'),
(11, 'COLUMN', 1, 'ods_src_orders', 'user_id',        2, 'fact_orders',    'user_id',     'DIRECT',       'AUTO_PARSER', 'ACTIVE'),
(12, 'COLUMN', 1, 'ods_src_orders', 'total_amount',   2, 'fact_orders',    'order_amount','DIRECT',       'AUTO_PARSER', 'ACTIVE'),
(13, 'COLUMN', 1, 'ods_src_orders', 'status',         2, 'fact_orders',    'order_status', 'DIRECT',      'AUTO_PARSER', 'ACTIVE');

-- 6.4 资产目录
TRUNCATE TABLE `gov_asset_catalog`;
INSERT INTO `gov_asset_catalog` (`id`, `parent_id`, `catalog_type`, `catalog_name`, `catalog_code`, `catalog_desc`, `cover_image`, `item_count`, `access_count`, `owner_id`, `sort_order`, `visible`, `status`) VALUES
(1, 0, 'BUSINESS_DOMAIN', '客户域资产',     'CATALOG_USER',     '客户域相关数据资产集合',   NULL, 5, 120, 2, 1, 1, 'ACTIVE'),
(2, 0, 'BUSINESS_DOMAIN', '订单域资产',     'CATALOG_ORDER',    '订单交易域相关数据资产集合', NULL, 8,  85, 3, 2, 1, 'ACTIVE'),
(3, 0, 'BUSINESS_DOMAIN', '商品域资产',     'CATALOG_PRODUCT',  '商品相关数据资产集合',        NULL, 3,  45, 4, 3, 1, 'ACTIVE'),
(4, 1, 'DATA_DOMAIN',     '用户维度表',     'CATALOG_USER_DIM', '客户域下的维度表集合',       NULL, 2,  60, 2, 1, 1, 'ACTIVE'),
(5, 1, 'DATA_DOMAIN',     '用户行为事实表',  'CATALOG_USER_FACT','客户域下的事实表集合',       NULL, 3,  60, 2, 2, 1, 'ACTIVE');

-- 6.5 资产目录元数据关联
TRUNCATE TABLE `gov_catalog_metadata`;
INSERT INTO `gov_catalog_metadata` (`catalog_id`, `metadata_id`, `ds_id`, `ds_name`, `ds_type`, `table_name`, `table_alias`, `data_layer`, `biz_domain`, `sort_order`) VALUES
(4, 2, 2, 'MySQL数仓(DWD)', 'MYSQL', 'dim_users',     '用户维度表', 'DWD', '客户域', 1),
(5, 3, 2, 'MySQL数仓(DWD)', 'MYSQL', 'fact_orders',   '订单事实表', 'DWD', '订单域', 1);

-- 6.6 术语分类
TRUNCATE TABLE `gov_glossary_category`;
INSERT INTO `gov_glossary_category` (`id`, `parent_id`, `category_name`, `category_code`, `category_desc`, `sort_order`, `status`) VALUES
(1, 0, '客户域',       'CUSTOMER',         '客户相关业务术语',                   1, 1),
(2, 0, '订单域',       'ORDER',            '订单交易相关术语',                   2, 1),
(3, 0, '商品域',       'PRODUCT',          '商品相关术语',                       3, 1),
(4, 0, '财务域',       'FINANCE',          '财务结算相关术语',                   4, 1),
(5, 0, '通用域',       'COMMON',           '通用基础术语',                       5, 1),
(6, 1, '客户基本信息', 'CUSTOMER_BASE',    '客户基本信息的术语',                 1, 1),
(7, 1, '客户行为',     'CUSTOMER_BEHAVIOR','客户行为相关的术语',                 2, 1),
(8, 2, '订单基本信息', 'ORDER_BASE',       '订单基本信息术语',                   1, 1),
(9, 2, '支付信息',     'ORDER_PAYMENT',    '订单支付相关术语',                   2, 1);

-- 6.7 术语
TRUNCATE TABLE `gov_glossary_term`;
INSERT INTO `gov_glossary_term` (`id`, `term_code`, `term_name`, `term_name_en`, `term_alias`, `category_id`, `biz_domain`, `definition`, `formula`, `data_type`, `unit`, `example_value`, `sensitivity_level`, `status`, `enabled`, `published_time`) VALUES
(1,  'TERM_001', '客户ID',       'Customer ID',         'customer_id, cust_id',        6, '客户域', '在系统中唯一标识一个客户的编号，使用雪花算法生成',                              '雪花算法ID：6871947673600000001', 'NUMBER', '个', '6871947673600000001', 'SENSITIVE',   'PUBLISHED', 1, '2024-01-01 00:00:00'),
(2,  'TERM_002', '客户姓名',     'Customer Name',       'cust_name, customer_name',    6, '客户域', '客户的真实姓名或注册名称',                                                        NULL,                              'STRING',  NULL, '张三',            'SENSITIVE',   'PUBLISHED', 1, '2024-01-01 00:00:00'),
(3,  'TERM_003', '手机号码',     'Mobile Phone',         'phone, mobile, tel',           6, '客户域', '客户注册的手机号码，用于联系和短信通知',                                        NULL,                              'STRING',  NULL, '13800138000',     'SENSITIVE',   'PUBLISHED', 1, '2024-01-01 00:00:00'),
(4,  'TERM_004', '日活跃用户数', 'Daily Active Users',  'DAU, 日活',                   7, '客户域', '当天登录或使用产品的独立用户数量',                                              'COUNT(DISTINCT user_id) WHERE login_date = target_date', 'NUMBER', '个', '50000',           'INTERNAL',    'PUBLISHED', 1, '2024-01-01 00:00:00'),
(5,  'TERM_005', '月活跃用户数', 'Monthly Active Users', 'MAU, 月活',                  7, '客户域', '自然月内登录或使用产品的独立用户去重数量',                                       'COUNT(DISTINCT user_id) WHERE login_date BETWEEN start_of_month AND end_of_month', 'NUMBER', '个', '150000', 'INTERNAL', 'PUBLISHED', 1, '2024-01-01 00:00:00'),
(6,  'TERM_006', '订单ID',       'Order ID',             'order_id, order_no',          8, '订单域', '在系统中唯一标识一笔订单的编号',                                                '雪花算法ID',                       'NUMBER', '个', '6872947673600000001', 'INTERNAL', 'PUBLISHED', 1, '2024-01-01 00:00:00'),
(7,  'TERM_007', '订单金额',     'Order Amount',         'order_amt, total_amount',     8, '订单域', '订单的实际支付金额，不含优惠抵扣',                                                NULL,                              'NUMBER', '元', '299.00',          'INTERNAL',    'PUBLISHED', 1, '2024-01-01 00:00:00'),
(8,  'TERM_008', '下单时间',     'Order Time',            'order_time, create_time',     8, '订单域', '用户提交订单的时间点',                                                          NULL,                              'DATE',   NULL, '2024-01-15 10:30:00', 'INTERNAL', 'PUBLISHED', 1, '2024-01-01 00:00:00'),
(9,  'TERM_009', '支付状态',     'Payment Status',        'pay_status, payment_status',  9, '订单域', '订单的支付状态：UNPAID/PAID/REFUNDED/CANCELLED',                                NULL,                              'STRING', NULL, 'PAID',            'INTERNAL',    'PUBLISHED', 1, '2024-01-01 00:00:00'),
(10, 'TERM_010', '订单转化率',  'Order Conversion Rate', 'conversion_rate',              8, '订单域', '从浏览到实际下单的转化百分比',                                                  'orders / visitors * 100%',        'NUMBER', '%',  '3.5%',             'INTERNAL',    'PUBLISHED', 1, '2024-01-01 00:00:00');

-- 6.8 术语-字段映射
TRUNCATE TABLE `gov_glossary_mapping`;
INSERT INTO `gov_glossary_mapping` (`id`, `term_id`, `ds_id`, `table_name`, `column_name`, `mapping_type`, `mapping_desc`, `confidence`, `status`, `approved_by`, `approved_time`) VALUES
(1, 1, 2, 'dim_users',     'user_id',      'DIRECT',    '用户ID直接映射',           100.00, 'APPROVED', 2, '2024-01-10 10:00:00'),
(2, 2, 2, 'dim_users',     'user_name',    'DIRECT',    '客户姓名直接映射',          100.00, 'APPROVED', 2, '2024-01-10 10:00:00'),
(3, 3, 2, 'dim_users',     'user_phone',   'DIRECT',    '手机号码直接映射',          100.00, 'APPROVED', 2, '2024-01-10 10:00:00'),
(4, 6, 2, 'fact_orders',   'order_id',     'DIRECT',    '订单ID直接映射',            100.00, 'APPROVED', 3, '2024-01-10 10:00:00'),
(5, 7, 2, 'fact_orders',   'order_amount', 'DIRECT',    '订单金额直接映射',          100.00, 'APPROVED', 3, '2024-01-10 10:00:00'),
(6, 4, 3, 'dws_user_profile', 'dau',      'AGGREGATE', '日活跃用户数聚合映射',      95.00,  'PENDING',   NULL, NULL);

-- 6.9 数据标准
TRUNCATE TABLE `gov_data_standard`;
INSERT INTO `gov_data_standard` (`id`, `standard_code`, `standard_name`, `standard_type`, `standard_category`, `standard_desc`, `rule_expr`, `example_value`, `applicable_object`, `enforce_action`, `status`, `enabled`, `sort_order`) VALUES
(1, 'CODE_ORDER_STATUS',  '订单状态编码标准',    'CODE_STANDARD',     'ORDER_STATUS',   '统一订单状态编码：UNPAID/PAID/SHIPPED/COMPLETED/CANCELLED/REFUNDED',                                                                                                           '^(UNPAID|PAID|SHIPPED|COMPLETED|CANCELLED|REFUNDED)$',                            'PAID,COMPLETED',          'DATA_VALUE',   'ALERT', 'PUBLISHED', 1, 1),
(2, 'CODE_USER_STATUS',   '用户状态编码标准',    'CODE_STANDARD',     'USER_STATUS',    '统一用户状态编码：0-正常、1-禁用、2-注销',                                                                                                                              '^(0|1|2)$',                                                                                   '0,1,2',                   'DATA_VALUE',   'ALERT', 'PUBLISHED', 1, 2),
(3, 'CODE_GENDER',        '性别编码标准',         'CODE_STANDARD',     'GENDER',         '统一性别编码：0-未知、1-男、2-女',                                                                                                                                      '^(0|1|2)$',                                                                                   '0,1,2',                   'DATA_VALUE',   'ALERT', 'PUBLISHED', 1, 3),
(4, 'CODE_YES_NO',        '是否标志编码标准',    'CODE_STANDARD',     'YES_NO',         '统一是否标志：Y-是、N-否',                                                                                                                                             '^([YN])$',                                                                                     'Y,N',                    'DATA_VALUE',   'ALERT', 'PUBLISHED', 1, 4),
(5, 'NAME_TABLE_PREFIX',  '表名命名规范',         'NAMING_STANDARD',   'TABLE_NAMING',   '表名统一使用前缀：dim_/fact_/ods_/dws_/ads_',                                                                                                                          '^(ods_|dim_|fact_|dws_|ads_|tmp_)[a-z_]+$',                                           'dim_user,fact_order',    'TABLE_NAME',   'ALERT', 'PUBLISHED', 1, 5),
(6, 'NAME_COLUMN_PREFIX', '字段命名规范',         'NAMING_STANDARD',   'COLUMN_NAMING',  '字段名统一使用小写+下划线格式，不允许出现大写和特殊字符',                                                                                                              '^[a-z][a-z0-9_]*$',                                                                          'user_name,create_time',    'COLUMN_NAME',  'ALERT', 'PUBLISHED', 1, 6),
(7, 'PRIMARY_CUSTOMER_ID', '客户ID主数据标准',    'PRIMARY_DATA',      'CUSTOMER',       '客户ID统一使用雪花算法生成的19位Long类型',                                                                                                                              '^[0-9]{19}$',                                                                                 '6871947673600000001',      'DATA_VALUE',   'ALERT', 'PUBLISHED', 1, 7),
(8, 'PRIMARY_ORDER_ID',   '订单ID主数据标准',    'PRIMARY_DATA',      'ORDER',          '订单ID统一使用雪花算法生成的19位Long类型',                                                                                                                              '^[0-9]{19}$',                                                                                 '6872947673600000001',      'DATA_VALUE',   'ALERT', 'PUBLISHED', 1, 8);

-- 6.10 数据标准绑定
TRUNCATE TABLE `gov_standard_binding`;
INSERT INTO `gov_standard_binding` (`id`, `standard_id`, `metadata_id`, `ds_id`, `target_table`, `target_column`, `compliance_status`, `violation_count`, `last_check_time`, `last_check_result`) VALUES
(1, 5, 2, 2, 'dim_users',      NULL,          'COMPLIANT', 0, '2024-01-15 00:00:00', '表名 dim_users 符合命名规范'),
(2, 5, 3, 2, 'fact_orders',    NULL,          'COMPLIANT', 0, '2024-01-15 00:00:00', '表名 fact_orders 符合命名规范'),
(3, 6, 2, 2, 'dim_users',      'user_phone',  'COMPLIANT', 0, '2024-01-15 00:00:00', '字段名 user_phone 符合命名规范'),
(4, 6, 2, 2, 'dim_users',      'user_name',   'COMPLIANT', 0, '2024-01-15 00:00:00', '字段名 user_name 符合命名规范'),
(5, 1, 3, 2, 'fact_orders',   'order_status', 'COMPLIANT', 0, '2024-01-15 00:00:00', '字段值符合订单状态编码规范'),
(6, 7, 2, 2, 'dim_users',      'user_id',     'COMPLIANT', 0, '2024-01-15 00:00:00', 'user_id 符合客户ID主数据标准'),
-- 今日新增（V11 追加数据）
(100, 5, 1, 1, 'ods_src_users', 'order_status', 'PENDING', 0, NOW(), '表名待审核'),
(101, 2, 2, 1, 'ods_src_users', 'user_status',  'PENDING', 0, NOW(), '字段名待审核')
ON DUPLICATE KEY UPDATE `compliance_status` = 'PENDING';

-- ============================================================
-- 7. 数据安全 - 分类分级 / 敏感识别 / 脱敏规则 / 访问审批
-- ============================================================

-- 7.1 数据分类
TRUNCATE TABLE `sec_classification`;
INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`) VALUES
(1, 'C01', '客户数据',   '包含个人身份信息、联系方式、财务信息等客户相关数据',          1, 1),
(2, 'C02', '交易数据',   '订单、支付、结算等交易相关数据',                             2, 1),
(3, 'C03', '产品数据',   '商品、库存、价格等业务产品相关数据',                         3, 1),
(4, 'C04', '运营数据',   '运营活动、用户行为、流量统计等数据',                         4, 1),
(5, 'C05', '财务数据',   '财务报表、税务、预算等财务相关数据',                         5, 1),
(6, 'C06', '员工数据',   '员工信息、薪酬、绩效等人力资源相关数据',                      6, 1),
(7, 'C07', '合同数据',   '合同、协议、证书等法务相关数据',                             7, 1),
(8, 'C08', '系统数据',   '系统配置、日志、审计等基础设施数据',                          8, 1),
(9, 'C09', '日志数据',   '系统日志、访问日志、操作日志等',                             9, 1),
(10,'C10', '配置数据',   '系统配置、业务参数等',                                       10, 1);

-- 7.2 数据分级
TRUNCATE TABLE `sec_level`;
INSERT INTO `sec_level` (`level_code`, `level_name`, `level_desc`, `level_value`, `color`, `status`) VALUES
('L1', '公开',   '可对外公开的数据，无任何访问限制',         1, '#67C23A', 1),
('L2', '内部',   '仅限公司内部员工访问，外部不可见',         2, '#409EFF', 1),
('L3', '敏感',   '涉及用户隐私或商业秘密，需审批后访问',     3, '#E6A23C', 1),
('L4', '机密',   '高度敏感数据，限特定人员访问，全程审计',   4, '#F56C6C', 1)
ON DUPLICATE KEY UPDATE `level_value` = VALUES(`level_value`);

-- 7.3 敏感字段识别规则（内置）
TRUNCATE TABLE `sec_sensitivity_rule`;
INSERT INTO `sec_sensitivity_rule` (`id`, `rule_name`, `rule_code`, `description`, `class_id`, `level_id`, `match_type`, `match_expr`, `match_expr_type`, `suggestion_action`, `suggestion_mask_pattern`, `suggestion_mask_type`, `priority`, `builtin`, `status`) VALUES
(1,  '手机号识别规则',    'RULE_PHONE',      '匹配手机号相关字段名和注释',             1, 3, 'COLUMN_NAME',     'phone',               'CONTAINS',     'MASK',   '{HEAD3}****{TAIL4}',     'CENTER', 10, 1, 1),
(2,  '邮箱识别规则',      'RULE_EMAIL',      '匹配邮箱相关字段名和注释',               1, 3, 'COLUMN_NAME',     'email',               'CONTAINS',     'MASK',   '{HEAD1}***@***.{TAIL3}',  'CENTER', 10, 1, 1),
(3,  '身份证识别规则',    'RULE_IDCARD',     '匹配身份证号字段名和注释',               1, 4, 'COLUMN_NAME',     'id_card',             'CONTAINS',     'MASK',   '{HEAD6}********{TAIL4}',  'CENTER', 10, 1, 1),
(4,  '姓名识别规则',      'RULE_USERNAME',   '匹配姓名相关字段名和注释（需谨慎）',    1, 3, 'COLUMN_NAME',     'name',                'CONTAINS',     'MASK',   '{HEAD1}*{TAIL1}',        'CENTER',  5, 1, 1),
(5,  '银行卡号识别规则',  'RULE_BANKCARD',   '匹配银行卡号字段',                       5, 4, 'COLUMN_NAME',     'bank_card',           'CONTAINS',     'MASK',   '{HEAD6}**********{TAIL4}','CENTER', 10, 1, 1),
(6,  '密码识别规则',      'RULE_PASSWORD',   '匹配密码相关字段',                       8, 4, 'COLUMN_NAME',     'password',            'CONTAINS',     'HIDE',   NULL,                      NULL,     10, 1, 1);

-- 7.4 脱敏规则模板
TRUNCATE TABLE `sec_mask_template`;
INSERT INTO `sec_mask_template` (`id`, `template_name`, `template_code`, `template_desc`, `data_type`, `mask_type`, `mask_position`, `mask_char`, `mask_head_keep`, `mask_tail_keep`, `mask_pattern`, `builtin`, `sort_order`, `status`) VALUES
(1,  '手机号中间脱敏',     'MASK_PHONE_CENTER',  '138****1234，保留前3后4位',           'STRING',  'MASK', 'CENTER', '*', 3, 4,  NULL,                    1, 1, 1),
(2,  '手机号末尾脱敏',     'MASK_PHONE_TAIL',    '138****，只保留前3位',                 'STRING',  'MASK', 'TAIL',   '*', 3, 0,  NULL,                    1, 2, 1),
(3,  '邮箱脱敏',           'MASK_EMAIL',          't***@***.com，隐藏中间部分',          'STRING',  'MASK', 'CENTER', '*', 1, 3,  NULL,                    1, 3, 1),
(4,  '身份证脱敏',         'MASK_IDCARD',         '110101**********1234，保留前6后4位',  'STRING',  'MASK', 'CENTER', '*', 6, 4,  NULL,                    1, 4, 1),
(5,  '姓名脱敏',           'MASK_NAME',           '张*，保留姓隐藏名',                  'STRING',  'MASK', 'CENTER', '*', 1, 1,  NULL,                    1, 5, 1),
(6,  '银行卡号脱敏',        'MASK_BANKCARD',       '622202******1234，保留前6后4位',      'STRING',  'MASK', 'CENTER', '*', 6, 4,  NULL,                    1, 6, 1),
(7,  '金额范围脱敏',        'MASK_AMOUNT',         '显示金额所属范围区间，如 1000-5000',  'NUMBER',  'MASK', 'FULL',   NULL, NULL, NULL, '{RANGE}',            1, 7, 1),
(8,  '完全隐藏',           'MASK_HIDE',            '完全隐藏字段值，显示为 ***',         'ALL',     'HIDE', 'FULL',   '*', 0, 0,  NULL,                    1, 8, 1),
(9,  '密码完全隐藏',        'MASK_PASSWORD',        '密码字段显示为空或星号',             'STRING',  'HIDE', 'FULL',   '*', 0, 0,  NULL,                    1, 9, 1);

-- 7.5 字段敏感等级记录
TRUNCATE TABLE `sec_column_sensitivity`;
INSERT INTO `sec_column_sensitivity` (`id`, `ds_id`, `metadata_id`, `table_name`, `column_name`, `column_comment`, `data_type`, `class_id`, `level_id`, `match_rule_id`, `mask_type`, `mask_pattern`, `mask_position`, `confidence`, `scan_batch_no`, `scan_time`, `review_status`, `approved_by`) VALUES
(1, 2, 2, 'dim_users',  'user_phone',  '手机号码',         'VARCHAR', 1, 3, 1,  'MASK', '138****{TAIL4}',         'CENTER', 95.00, 'SCAN_BATCH_001', '2024-01-15 00:00:00', 'APPROVED', 2),
(2, 2, 2, 'dim_users',  'email',       '邮箱地址',         'VARCHAR', 1, 3, 2,  'MASK', '{HEAD1}***@***.{TAIL3}','CENTER', 92.00, 'SCAN_BATCH_001', '2024-01-15 00:00:00', 'APPROVED', 2),
(3, 2, 2, 'dim_users',  'id_card',     '身份证号',         'VARCHAR', 1, 4, 3,  'MASK', '{HEAD6}********{TAIL4}', 'CENTER', 99.00, 'SCAN_BATCH_001', '2024-01-15 00:00:00', 'APPROVED', 2),
(4, 2, 2, 'dim_users',  'user_name',   '用户姓名',         'VARCHAR', 1, 3, 4,  'MASK', '{HEAD1}*{TAIL1}',         'CENTER', 88.00, 'SCAN_BATCH_001', '2024-01-15 00:00:00', 'APPROVED', 2),
(5, 2, 3, 'fact_orders','order_amount', '订单金额',        'DECIMAL', 2, 2, NULL,'MASK', '{RANGE}',                'FULL',   80.00, 'SCAN_BATCH_001', '2024-01-15 00:00:00', 'PENDING',  NULL);

-- 7.6 敏感字段访问申请
TRUNCATE TABLE `sec_access_application`;
INSERT INTO `sec_access_application` (`id`, `application_no`, `applicant_id`, `applicant_name`, `dept_id`, `dept_name`, `target_ds_id`, `target_ds_name`, `target_table`, `target_columns`, `target_column_names`, `duration_type`, `duration_hours`, `start_time`, `end_time`, `apply_reason`, `status`, `approver_id`, `approver_name`, `approval_comment`, `approval_time`) VALUES
(1, 'APP20240115001', 3, '李四', 3, '数据质量组', 2, 'MySQL数仓(DWD)', 'dim_users', '["user_phone","email","id_card"]', 'user_phone, email, id_card', 'WEEK', 168, '2024-01-15 00:00:00', '2024-01-22 00:00:00', '数据质量检测需要验证用户手机号和身份证号的准确性，需要临时访问敏感字段进行数据比对', 'APPROVED', 1, 'admin', '同意，仅限数据质量检测用途，注意数据保密', '2024-01-15 10:30:00'),
(2, 'APP20240116001', 4, '王五', 4, '数据开发组', 2, 'MySQL数仓(DWD)', 'fact_orders', '["order_amount"]', 'order_amount', 'DAY', 24, '2024-01-16 00:00:00', '2024-01-17 00:00:00', '数据分析需要查看订单金额分布，用于制作财务报表', 'PENDING', NULL, NULL, NULL, NULL);

-- 7.7 敏感字段访问审批记录
TRUNCATE TABLE `sec_access_audit`;
INSERT INTO `sec_access_audit` (`id`, `application_id`, `application_no`, `approver_id`, `approver_name`, `approver_dept`, `action`, `result`, `comment`, `audit_node`, `audit_time`, `audit_source`) VALUES
(1, 1, 'APP20240115001', 1, 'admin', '数据中台部', 'APPROVE', 'AGREE', '同意，仅限数据质量检测用途，注意数据保密', 'FIRST', '2024-01-15 10:30:00', 'MANUAL');

-- 7.8 敏感字段访问审计日志
TRUNCATE TABLE `sec_access_log`;
INSERT INTO `sec_access_log` (`id`, `application_id`, `application_no`, `operator_id`, `operator_name`, `operator_dept`, `operation_type`, `operation_content`, `target_ds_id`, `target_table`, `target_column`, `ip_address`, `status`) VALUES
(1, 1, 'APP20240115001', 1, 'admin', '数据中台部', 'APPROVE',  '审批通过敏感字段访问申请 APP20240115001',            2, 'dim_users',     'user_phone,email,id_card', '192.168.1.50',  'APPROVED'),
(2, 1, 'APP20240115001', 3, '李四',  '数据质量组', 'ACCESS',   '查询敏感字段 user_phone, email, id_card 用于数据质量检测', 2, 'dim_users',     'user_phone,email,id_card', '192.168.1.100', 'SUCCESS'),
(3, 2, 'APP20240116001', 4, '王五',  '数据开发组', 'APPLY',    '提交敏感字段访问申请 APP20240116001',                  2, 'fact_orders',   'order_amount',             '192.168.1.101', 'PENDING');
