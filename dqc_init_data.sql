-- ============================================================
-- 数据质量（DQC）初始化数据
-- 适用版本: ruoyi-metadata 模块（基于 ry_bigdata_v1_data.sql 表结构）
--
-- 包含:
--   1. 规则模板（dqc_rule_template）8 条内置模板
--   2. 规则定义（dqc_rule_def）10 条示例规则
--   3. 质检方案（dqc_plan）3 个示例方案
--   4. 质检执行记录（dqc_execution）5 条模拟记录
--   5. 执行明细（dqc_execution_detail）15 条明细数据
--
-- 使用方式: 在已有表结构基础上执行
--   - TRUNCATE 语句确保可重复执行时数据干净
--   - 字段与 ry_bigdata_v1_data.sql 实际表结构完全对齐
-- ============================================================

-- ============================================================
-- 1. 规则模板（dqc_rule_template）
-- 实际字段: id, tenant_id, template_code, template_name,
--           template_desc, rule_type, apply_level, default_expr,
--           threshold_json, param_spec, dimension, builtin,
--           enabled, del_flag, create_dept, create_by, create_time,
--           update_by, update_time
-- ============================================================

TRUNCATE TABLE `dqc_rule_template`;
INSERT INTO `dqc_rule_template`
  (`id`, `tenant_id`, `template_code`, `template_name`, `template_desc`, `rule_type`, `apply_level`,
   `default_expr`, `threshold_json`, `param_spec`, `dimension`, `builtin`, `enabled`, `del_flag`,
   `create_by`, `create_time`)
VALUES
-- T01: 空值检查（完整性）
(1, '000000', 'T01_NULL_CHECK', '空值检查',
 '检测指定字段的空值比例是否超过阈值，超过则判定为不合格',
 'NULL_CHECK', 'COLUMN',
 'SELECT COUNT(*) FROM `${TABLE_NAME}` WHERE `${COLUMN_NAME}` IS NULL OR `${COLUMN_NAME}` = '''' AS null_count, (SELECT COUNT(*) FROM `${TABLE_NAME}`) AS total_count',
 NULL,
 '[]',
 'COMPLETENESS', '1', '0', '0', 'admin', NOW()),

-- T02: 唯一性检查
(2, '000000', 'T02_UNIQUE_CHECK', '唯一性检查',
 '检测指定字段的值是否唯一（主键或候选键），重复值超过阈值则不合格',
 'UNIQUE', 'COLUMN',
 'SELECT COUNT(*) - COUNT(DISTINCT `${COLUMN_NAME}`) AS duplicate_count FROM `${TABLE_NAME}` WHERE `${COLUMN_NAME}` IS NOT NULL AND `${COLUMN_NAME}` <> '''' ',
 NULL,
 '[]',
 'UNIQUENESS', '1', '0', '0', 'admin', NOW()),

-- T03: 数值范围检查（准确性）
(3, '000000', 'T03_THRESHOLD_CHECK', '数值范围检查',
 '检测数值字段的值是否在合理范围内（min/max），超出阈值为不合格',
 'THRESHOLD', 'COLUMN',
 'SELECT COUNT(*) FROM `${TABLE_NAME}` WHERE `${COLUMN_NAME}` < ${param.minValue} OR `${COLUMN_NAME}` > ${param.maxValue} AS out_of_range_count, (SELECT COUNT(*) FROM `${TABLE_NAME}` WHERE `${COLUMN_NAME}` IS NOT NULL) AS total_count',
 NULL,
 '[{"name":"minValue","type":"DECIMAL","defaultValue":"0","description":"最小值","required":true},{"name":"maxValue","type":"DECIMAL","defaultValue":"999999","description":"最大值","required":true}]',
 'ACCURACY', '1', '0', '0', 'admin', NOW()),

-- T04: 枚举值检查（有效性）
(4, '000000', 'T04_ENUM_CHECK', '枚举值检查',
 '检测字段值是否在预定义的枚举列表内，不在枚举范围内的判定为无效数据',
 'REGEX', 'COLUMN',
 'SELECT COUNT(*) FROM `${TABLE_NAME}` WHERE `${COLUMN_NAME}` IS NOT NULL AND `${COLUMN_NAME}` NOT IN (''${param.enumValues}'') AS invalid_count, (SELECT COUNT(*) FROM `${TABLE_NAME}` WHERE `${COLUMN_NAME}` IS NOT NULL) AS total_count',
 NULL,
 '[{"name":"enumValues","type":"STRING","defaultValue":"A,B,C","description":"逗号分隔的枚举值","required":true}]',
 'VALIDITY', '1', '0', '0', 'admin', NOW()),

-- T05: 跨表一致性检查
(5, '000000', 'T05_CONSISTENCY_CHECK', '跨表一致性检查',
 '检测两张关联表之间的关键字段值是否一致（外键一致性），不一致超过阈值则不合格',
 'CUSTOM_SQL', 'CROSS_TABLE',
 'SELECT COUNT(*) FROM `${TABLE_NAME}` t1 LEFT JOIN `${param.compareTable}` t2 ON t1.${param.joinColumn} = t2.${param.compareColumn} WHERE t2.${param.compareColumn} IS NULL AND t1.${param.joinColumn} IS NOT NULL AS orphan_count, (SELECT COUNT(*) FROM `${TABLE_NAME}` WHERE ${param.joinColumn} IS NOT NULL) AS total_count',
 NULL,
 '[{"name":"compareTable","type":"STRING","defaultValue":"","description":"对比表名","required":true},{"name":"joinColumn","type":"STRING","defaultValue":"","description":"本表关联列","required":true},{"name":"compareColumn","type":"STRING","defaultValue":"","description":"对比表关联列","required":true}]',
 'CONSISTENCY', '1', '0', '0', 'admin', NOW()),

-- T06: 波动率检查（时效性）
(6, '000000', 'T06_FLUCTUATION_CHECK', '数据波动检查',
 '检测关键指标的日波动率是否超过设定阈值，异常波动可能意味着数据源问题或ETL异常',
 'FLUCTUATION', 'TABLE',
 'SELECT ABS((SELECT COUNT(*) FROM `${TABLE_NAME}` WHERE DATE(create_time) = CURDATE()) - (SELECT COUNT(*) FROM `${TABLE_NAME}` WHERE DATE(create_time) = DATE_SUB(CURDATE(), INTERVAL 1 DAY))) / NULLIF((SELECT COUNT(*) FROM `${TABLE_NAME}` WHERE DATE(create_time) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)), 0) * 100 AS fluctuation_pct',
 NULL,
 '[{"name":"maxFluctuationPct","type":"DECIMAL","defaultValue":"20","description":"最大允许波动百分比","required":false}]',
 'TIMELINESS', '1', '0', '0', 'admin', NOW()),

-- T07: 重复记录检查
(7, '000000', 'T07_DUPLICATE_CHECK', '重复记录检查',
 '检测表中完全重复或关键字段组合重复的记录数，超过阈值则数据质量不合格',
 'CUSTOM_SQL', 'TABLE',
 'SELECT COUNT(*) - COUNT(DISTINCT CONCAT(${param.keyColumns})) AS duplicate_count FROM `${TABLE_NAME}`',
 NULL,
 '[{"name":"keyColumns","type":"STRING","defaultValue":"id,name","description":"用于判断重复的关键字段（逗号分隔）","required":true}]',
 'UNIQUENESS', '1', '0', '0', 'admin', NOW()),

-- T08: 引用完整性检查
(8, '000000', 'T08_REFERENCE_CHECK', '引用完整性检查',
 '检测外键字段的值是否都在主表/字典表中存在，孤立记录超过阈值则不合格',
 'CUSTOM_SQL', 'CROSS_TABLE',
 'SELECT COUNT(*) FROM `${TABLE_NAME}` t1 WHERE t1.${param.fkColumn} IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `${param.refTable}` t2 WHERE t2.${param.refColumn} = t1.${param.fkColumn}) AS orphan_count, (SELECT COUNT(*) FROM `${TABLE_NAME}` WHERE ${param.fkColumn} IS NOT NULL) AS total_count',
 NULL,
 '[{"name":"refTable","type":"STRING","defaultValue":"","description":"被引用表名","required":true},{"name":"fkColumn","type":"STRING","defaultValue":"","description":"本表外键列","required":true},{"name":"refColumn","type":"STRING","defaultValue":"id","description":"被引用表主键列","required":false}]',
 'CONSISTENCY', '1', '0', '0', 'admin', NOW());


-- ============================================================
-- 2. 规则定义（dqc_rule_def）
-- 实际字段: id, tenant_id, rule_name, rule_code, template_id,
--           rule_type, apply_level, dimensions, rule_expr,
--           target_ds_id, target_table, target_column,
--           compare_ds_id, compare_table, compare_column,
--           threshold_min, threshold_max, fluctuation_threshold,
--           regex_pattern, error_level, alert_receivers,
--           sort_order, enabled, del_flag,
--           create_dept, create_by, create_time, update_by, update_time
-- 注意: 无 builtin, rule_strength 字段
-- ============================================================

TRUNCATE TABLE `dqc_rule_def`;
INSERT INTO `dqc_rule_def`
  (`id`, `tenant_id`, `rule_name`, `rule_code`, `template_id`, `rule_type`, `apply_level`,
   `dimensions`, `rule_expr`, `target_ds_id`, `target_table`, `target_column`,
   `threshold_min`, `threshold_max`, `error_level`, `enabled`, `del_flag`,
   `create_by`, `create_time`)
VALUES
-- R01: ODS层用户表手机号空值检查
(1, '000000', 'ODS层用户表手机号空值检查', 'R01_USER_PHONE_NULL',
 1, 'NULL_CHECK', 'COLUMN', 'COMPLETENESS',
 'SELECT COUNT(*) FROM ods_user WHERE phone IS NULL OR phone = '''' AS null_count, (SELECT COUNT(*) FROM ods_user) AS total_count',
 1, 'ods_user', 'phone',
 0, 5, 'MEDIUM', '0', '0', 'admin', NOW()),

-- R02: ODS层用户表手机号唯一性检查
(2, '000000', 'ODS层用户表手机号唯一性检查', 'R02_USER_PHONE_UNIQUE',
 2, 'UNIQUE', 'COLUMN', 'UNIQUENESS',
 'SELECT COUNT(*) - COUNT(DISTINCT phone) AS dup_count FROM ods_user WHERE phone IS NOT NULL AND phone <> '''' ',
 1, 'ods_user', 'phone',
 0, 1, 'HIGH', '0', '0', 'admin', NOW()),

-- R03: DWD层订单表订单金额合理性
(3, '000000', 'DWD层订单表订单金额合理性', 'R03_ORDER_AMOUNT_RANGE',
 3, 'THRESHOLD', 'COLUMN', 'ACCURACY',
 'SELECT COUNT(*) FROM dwd_order WHERE order_amount < ${param.minValue} OR order_amount > ${param.maxValue} AS bad_count, (SELECT COUNT(*) FROM dwd_order) AS total_count',
 1, 'dwd_order', 'order_amount',
 0, 999999, 'HIGH', '0', '0', 'admin', NOW()),

-- R04: ODS层用户表邮箱有效性
(4, '000000', 'ODS层用户表邮箱有效性', 'R04_USER_EMAIL_VALID',
 4, 'REGEX', 'COLUMN', 'VALIDITY',
 'SELECT COUNT(*) FROM ods_user WHERE email IS NOT NULL AND email NOT REGEXP ''^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'' AS invalid_count, (SELECT COUNT(*) FROM ods_user WHERE email IS NOT NULL) AS total_count',
 1, 'ods_user', 'email',
 0, 5, 'MEDIUM', '0', '0', 'admin', NOW()),

-- R05: DWD层订单表用户ID引用完整性
(5, '000000', 'DWD层订单表用户ID引用完整性', 'R05_ORDER_USER_FK',
 8, 'CUSTOM_SQL', 'CROSS_TABLE', 'CONSISTENCY',
 'SELECT COUNT(*) FROM dwd_order o WHERE o.user_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dwd_user u WHERE u.user_id = o.user_id) AS orphan_count, (SELECT COUNT(*) FROM dwd_order WHERE user_id IS NOT NULL) AS total_count',
 1, 'dwd_order', 'user_id',
 0, 0, 'HIGH', '0', '0', 'admin', NOW()),

-- R06: DWD层用户表身份证号唯一性
(6, '000000', 'DWD层用户表身份证号唯一性', 'R06_USER_IDCARD_UNIQUE',
 2, 'UNIQUE', 'COLUMN', 'UNIQUENESS',
 'SELECT COUNT(*) - COUNT(DISTINCT idcard) AS dup_count FROM dwd_user WHERE idcard IS NOT NULL AND idcard <> '''' ',
 1, 'dwd_user', 'idcard',
 0, 1, 'HIGH', '0', '0', 'admin', NOW()),

-- R07: DWD层商品表状态枚举有效性
(7, '000000', 'DWD层商品表状态枚举有效性', 'R07_PRODUCT_STATUS_ENUM',
 4, 'REGEX', 'COLUMN', 'VALIDITY',
 'SELECT COUNT(*) FROM dwd_product WHERE status IS NOT NULL AND status NOT IN (''ACTIVE'',''INACTIVE'',''DELETED'') AS invalid_count, (SELECT COUNT(*) FROM dwd_product WHERE status IS NOT NULL) AS total_count',
 1, 'dwd_product', 'status',
 0, 5, 'LOW', '0', '0', 'admin', NOW()),

-- R08: DWD层订单表日订单量波动监控
(8, '000000', 'DWD层订单表日订单量波动监控', 'R08_ORDER_DAILY_FLUCTUATION',
 6, 'FLUCTUATION', 'TABLE', 'TIMELINESS',
 'SELECT ABS((SELECT COUNT(*) FROM dwd_order WHERE DATE(create_time) = CURDATE()) - (SELECT COUNT(*) FROM dwd_order WHERE DATE(create_time) = DATE_SUB(CURDATE(), INTERVAL 1 DAY))) / NULLIF((SELECT COUNT(*) FROM dwd_order WHERE DATE(create_time) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)), 0) * 100 AS fluctuation_pct',
 1, 'dwd_order', NULL,
 0, 20, 'MEDIUM', '0', '0', 'admin', NOW()),

-- R09: ODS层用户表重复记录检查
(9, '000000', 'ODS层用户表重复记录检查', 'R09_USER_DUPLICATE',
 7, 'CUSTOM_SQL', 'TABLE', 'UNIQUENESS',
 'SELECT COUNT(*) - COUNT(DISTINCT CONCAT(username, phone)) AS duplicate_count FROM ods_user',
 1, 'ods_user', NULL,
 0, 1, 'MEDIUM', '0', '0', 'admin', NOW()),

-- R10: DWD层商品表商品ID唯一性
(10, '000000', 'DWD层商品表商品ID唯一性', 'R10_PRODUCT_ID_UNIQUE',
 2, 'UNIQUE', 'COLUMN', 'UNIQUENESS',
 'SELECT COUNT(*) - COUNT(DISTINCT product_id) AS dup_count FROM dwd_product WHERE product_id IS NOT NULL',
 1, 'dwd_product', 'product_id',
 0, 1, 'HIGH', '0', '0', 'admin', NOW());


-- ============================================================
-- 3. 质检方案（dqc_plan）
-- 实际字段: id, tenant_id, plan_name, plan_code, plan_desc,
--           layer_code, trigger_type, enabled, del_flag,
--           create_dept, create_by, create_time, update_by, update_time
-- 注意: 无 trigger_cron, alert_on_failure, auto_block, bind_type,
--       bind_value, rule_count, table_count, status, last_execution_id 等
-- ============================================================

TRUNCATE TABLE `dqc_plan`;
INSERT INTO `dqc_plan`
  (`id`, `tenant_id`, `plan_name`, `plan_code`, `plan_desc`, `bind_type`, `bind_value`,
   `layer_code`, `trigger_type`, `trigger_cron`, `alert_on_failure`, `auto_block`,
   `status`, `rule_count`, `table_count`, `del_flag`, `create_by`, `create_time`)
VALUES
-- P01: ODS层数据质量日常监控
(1, '000000', 'ODS层数据质量日常监控', 'PLAN_ODS_DAILY',
 '覆盖 ODS 层的用户、产品等基础表的空值、唯一性、有效性检查，每日凌晨执行',
 'LAYER', '{"layerCode":"ODS"}', 'ODS', 'SCHEDULE', '0 30 2 * * ?', '1', '0',
 'PUBLISHED', 5, 3, '0', 'admin', NOW()),

-- P02: DWD层数据质量专项检查
(2, '000000', 'DWD层数据质量专项检查', 'PLAN_DWD_SPECIAL',
 '覆盖 DWD 层的订单、商品、用户宽表的准确性、一致性、时效性检查，工作日上午执行',
 'LAYER', '{"layerCode":"DWD"}', 'DWD', 'SCHEDULE', '0 0 9 ? * MON-FRI', '1', '1',
 'PUBLISHED', 5, 4, '0', 'admin', NOW()),

-- P03: 全表数据质量摸底
(3, '000000', '全表数据质量摸底', 'PLAN_FULL_SURVEY',
 '对所有已注册表执行通用空值和唯一性检查，快速发现数据质量问题',
 'PATTERN', '{"pattern":"*"}', NULL, 'MANUAL', NULL, '1', '0',
 'DRAFT', 0, 0, '0', 'admin', NOW());


-- ============================================================
-- 4. 质检执行记录（dqc_execution）
-- 实际字段: id, tenant_id, execution_no, plan_id, plan_name,
--           layer_code, trigger_type, trigger_user,
--           start_time, end_time, elapsed_ms,
--           total_rules, passed_count, failed_count,
--           blocked_count, overall_score, status,
--           del_flag, create_dept, create_by, create_time,
--           update_by, update_time
-- ============================================================

TRUNCATE TABLE `dqc_execution`;
INSERT INTO `dqc_execution`
  (`id`, `tenant_id`, `execution_no`, `plan_id`, `plan_name`, `layer_code`,
   `trigger_type`, `trigger_user`, `start_time`, `end_time`, `elapsed_ms`,
   `total_rules`, `passed_count`, `failed_count`, `blocked_count`,
   `overall_score`, `status`, `del_flag`, `create_by`, `create_time`)
VALUES
-- EX01: ODS日常监控（昨日，5条规则全部通过，得分98.5）
(1, '000000', 'EX2026040501', 1, 'ODS层数据质量日常监控', 'ODS',
 'SCHEDULE', 1,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 5 MINUTE, 323000,
 5, 5, 0, 0, 98.50, 'SUCCESS', '0', 'admin', DATE_SUB(NOW(), INTERVAL 1 DAY)),

-- EX02: ODS日常监控（前日，1条失败，得分85.2）
(2, '000000', 'EX2026040401', 1, 'ODS层数据质量日常监控', 'ODS',
 'SCHEDULE', 1,
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 8 MINUTE, 471000,
 5, 4, 1, 0, 85.20, 'FAILED', '0', 'admin', DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- EX03: DWD专项检查（3天前，部分通过，得分72.0）
(3, '000000', 'EX2026040301', 2, 'DWD层数据质量专项检查', 'DWD',
 'SCHEDULE', 1,
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 12 MINUTE, 721000,
 5, 3, 2, 1, 72.00, 'PARTIAL', '0', 'admin', DATE_SUB(NOW(), INTERVAL 3 DAY)),

-- EX04: 手动摸底（今日，得分75.0）
(4, '000000', 'EX2026040502', 3, '全表数据质量摸底', NULL,
 'MANUAL', 1,
 NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR, 3600000,
 10, 7, 2, 1, 75.00, 'SUCCESS', '0', 'admin', NOW() - INTERVAL 2 HOUR),

-- EX05: 仍在运行中的执行
(5, '000000', 'EX2026040503', 1, 'ODS层数据质量日常监控', 'ODS',
 'SCHEDULE', 1,
 NOW() - INTERVAL 1 MINUTE, NULL, NULL,
 5, 2, 0, 0, NULL, 'RUNNING', '0', 'admin', NOW() - INTERVAL 1 MINUTE);


-- ============================================================
-- 5. 执行明细（dqc_execution_detail）
-- 实际字段: id, tenant_id, execution_id, rule_id, rule_name,
--           rule_code, rule_type, dimension, target_ds_id,
--           target_table, target_column, actual_value, threshold_value,
--           result_value, pass_flag, error_level, error_msg,
--           execute_sql, execute_time, elapsed_ms, del_flag
-- 注意: 无 execution_no, rule_strength, compare_ds_id 等
-- ============================================================

TRUNCATE TABLE `dqc_execution_detail`;
INSERT INTO `dqc_execution_detail`
  (`id`, `tenant_id`, `execution_id`, `rule_id`, `rule_name`, `rule_code`, `rule_type`,
   `dimension`, `target_ds_id`, `target_table`, `target_column`,
   `actual_value`, `threshold_value`, `result_value`, `pass_flag`, `error_level`,
   `error_msg`, `execute_sql`, `execute_time`, `elapsed_ms`, `del_flag`)
VALUES
-- EX01 明细（全部通过）
(1,  '000000', 1, 1, 'ODS层用户表手机号空值检查',   'R01_USER_PHONE_NULL',     'NULL_CHECK',    'COMPLETENESS', 1, 'ods_user',   'phone',      '1.5%',  '≤5%',    1.50, '1', 'MEDIUM', NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 1 DAY), 3200,  '0'),
(2,  '000000', 1, 2, 'ODS层用户表手机号唯一性检查', 'R02_USER_PHONE_UNIQUE',   'UNIQUE',        'UNIQUENESS',   1, 'ods_user',   'phone',      '0.02%', '≤1%',    0.02, '1', 'HIGH',   NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 1 DAY), 4500,  '0'),
(3,  '000000', 1, 4, 'ODS层用户表邮箱有效性',      'R04_USER_EMAIL_VALID',    'REGEX',         'VALIDITY',     1, 'ods_user',   'email',      '0.8%',  '≤5%',    0.80, '1', 'MEDIUM', NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 1 DAY), 2800,  '0'),
(4,  '000000', 1, 9, 'ODS层用户表重复记录检查',   'R09_USER_DUPLICATE',     'CUSTOM_SQL',   'UNIQUENESS',   1, 'ods_user',   NULL,        '0.0%',  '≤1%',    0.00, '1', 'MEDIUM', NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 1 DAY), 5200,  '0'),
(5,  '000000', 1, 7, 'DWD层商品表状态枚举有效性', 'R07_PRODUCT_STATUS_ENUM', 'REGEX',         'VALIDITY',     1, 'dwd_product','status',     '0.0%',  '≤5%',    0.00, '1', 'LOW',    NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 1 DAY), 2100,  '0'),

-- EX02 明细（1条失败）
(6,  '000000', 2, 1, 'ODS层用户表手机号空值检查',   'R01_USER_PHONE_NULL',     'NULL_CHECK',    'COMPLETENESS', 1, 'ods_user',   'phone',      '10.3%', '≤5%',    10.30, '0', 'MEDIUM', '手机号空值比例 10.3% 超过阈值 5%',              NULL,                       DATE_SUB(NOW(), INTERVAL 2 DAY), 3100,  '0'),
(7,  '000000', 2, 2, 'ODS层用户表手机号唯一性检查', 'R02_USER_PHONE_UNIQUE',   'UNIQUE',        'UNIQUENESS',   1, 'ods_user',   'phone',      '0.5%',  '≤1%',    0.50,  '1', 'HIGH',   NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 2 DAY), 4800,  '0'),
(8,  '000000', 2, 4, 'ODS层用户表邮箱有效性',      'R04_USER_EMAIL_VALID',    'REGEX',         'VALIDITY',     1, 'ods_user',   'email',      '2.1%',  '≤5%',    2.10,  '1', 'MEDIUM', NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 2 DAY), 2700,  '0'),
(9,  '000000', 2, 9, 'ODS层用户表重复记录检查',   'R09_USER_DUPLICATE',     'CUSTOM_SQL',   'UNIQUENESS',   1, 'ods_user',   NULL,        '0.3%',  '≤1%',    0.30,  '1', 'MEDIUM', NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 2 DAY), 5100,  '0'),
(10, '000000', 2, 7, 'DWD层商品表状态枚举有效性', 'R07_PRODUCT_STATUS_ENUM', 'REGEX',         'VALIDITY',     1, 'dwd_product','status',     '0.0%',  '≤5%',    0.00,  '1', 'LOW',    NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 2 DAY), 2000,  '0'),

-- EX03 明细（部分通过）
(11, '000000', 3, 3, 'DWD层订单表订单金额合理性',   'R03_ORDER_AMOUNT_RANGE', 'THRESHOLD',     'ACCURACY',     1, 'dwd_order', 'order_amount','2.1%',  '≤1%',    2.10,  '0', 'HIGH',   '订单金额超出合理范围[0,999999]的记录占 2.1%，超过阈值 1%', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), 8900,  '0'),
(12, '000000', 3, 5, 'DWD层订单表用户ID引用完整性', 'R05_ORDER_USER_FK',      'CUSTOM_SQL',   'CONSISTENCY',  1, 'dwd_order', 'user_id',    '0.5%',  '≤0%',    0.50,  '0', 'HIGH',   '发现 126 条订单记录关联的用户ID在用户表中不存在（孤儿记录）', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), 12000, '0'),
(13, '000000', 3, 6, 'DWD层用户表身份证号唯一性', 'R06_USER_IDCARD_UNIQUE',  'UNIQUE',        'UNIQUENESS',   1, 'dwd_user',  'idcard',     '0.0%',  '≤1%',    0.00,  '1', 'HIGH',   NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 3 DAY), 7600,  '0'),
(14, '000000', 3, 7, 'DWD层商品表状态枚举有效性', 'R07_PRODUCT_STATUS_ENUM', 'REGEX',         'VALIDITY',     1, 'dwd_product','status',     '0.0%',  '≤5%',    0.00,  '1', 'LOW',    NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 3 DAY), 2300,  '0'),
(15, '000000', 3, 8, 'DWD层订单表日订单量波动监控', 'R08_ORDER_DAILY_FLUCTUATION','FLUCTUATION','TIMELINESS',  1, 'dwd_order', NULL,        '18.5%', '≤20%',   18.50, '1', 'MEDIUM', NULL,                       NULL,                       DATE_SUB(NOW(), INTERVAL 3 DAY), 15000, '0');

-- ============================================================
-- DQC 初始化数据完成
-- ============================================================
