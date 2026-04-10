-- ============================================================
-- 数据资产平台基础数据初始化脚本
-- 适用数据库：MySQL 8.0+
-- 执行顺序：按依赖关系自动排序
-- ============================================================

USE ry_bigdata_v1;

-- ============================================================
-- 第1部分：敏感等级 (sec_level)
-- 建议等级值：1-4（1最低，4最高）
-- ============================================================
INSERT INTO sec_level (tenant_id, level_code, level_name, level_value, level_desc, color, sort_order, enabled, del_flag, create_dept, create_by, create_time, update_by, update_time) VALUES
-- 低敏感等级
('000000', 'NORMAL',    '一般数据',    1, '公开可访问的数据，不涉及个人隐私或商业秘密',  '#909399', 1, '1', '0', 100, '1', NOW(), 0, NULL),
-- 中敏感等级
('000000', 'INNER',     '内部数据',    2, '仅限企业内部使用，不可对外公开',             '#409EFF', 2, '1', '0', 100, '1', NOW(), 0, NULL),
-- 高敏感等级
('000000', 'SENSITIVE', '敏感数据',    3, '涉及个人隐私或商业秘密，需严格管控访问',     '#E6A23C', 3, '1', '0', 100, '1', NOW(), 0, NULL),
-- 极高敏感等级
('000000', 'HIGHLY',    '高度敏感',    4, '涉及核心机密，必须加密存储，访问需审批',     '#F56C6C', 4, '1', '0', 100, '1', NOW(), 0, NULL);

-- ============================================================
-- 第2部分：数据分类 (sec_classification)
-- 业务分类与默认敏感等级关联
-- ============================================================
INSERT INTO sec_classification (tenant_id, class_code, class_name, class_desc, sort_order, default_level_code, enabled, del_flag, create_dept, create_by, create_time, update_by, update_time) VALUES
-- 个人信息类
('000000', 'PERSONAL',      '个人信息',       '姓名、身份证号、手机号、邮箱等个人身份信息',     1,  'SENSITIVE', '1', '0', 100, '1', NOW(), 0, NULL),
-- 财务类
('000000', 'FINANCIAL',     '财务数据',       '收入、支出、利润、资产负债表等财务相关数据',     2,  'SENSITIVE', '1', '0', 100, '1', NOW(), 0, NULL),
-- 交易类
('000000', 'TRANSACTION',   '交易数据',       '订单、支付、交易记录等业务交易数据',             3,  'INNER',     '1', '0', 100, '1', NOW(), 0, NULL),
-- 运营类
('000000', 'OPERATION',     '运营数据',       '业务运营指标、日志、报表等运营数据',               4,  'INNER',     '1', '0', 100, '1', NOW(), 0, NULL),
-- 客户类
('000000', 'CUSTOMER',     '客户数据',       '客户信息、客户行为、客户画像等',                   5,  'SENSITIVE', '1', '0', 100, '1', NOW(), 0, NULL),
-- 产品类
('000000', 'PRODUCT',      '产品数据',       '商品信息、库存、价格等产品和库存数据',             6,  'NORMAL',    '1', '0', 100, '1', NOW(), 0, NULL),
-- 供应商类
('000000', 'SUPPLIER',     '供应商数据',     '供应商信息、合同、采购数据',                     7,  'INNER',     '1', '0', 100, '1', NOW(), 0, NULL),
-- 内部管理类
('000000', 'INTERNAL',      '内部管理数据',   '员工信息、组织架构、权限配置等内部管理数据',       8,  'INNER',     '1', '0', 100, '1', NOW(), 0, NULL),
-- 公共类
('000000', 'PUBLIC',       '公共数据',       '公开信息、参考数据、字典数据等',                  9,  'NORMAL',    '1', '0', 100, '1', NOW(), 0, NULL),
-- 日志审计类
('000000', 'AUDIT',        '日志审计数据',   '系统日志、操作审计、安全日志等',                   10, 'INNER',     '1', '0', 100, '1', NOW(), 0, NULL);

-- ============================================================
-- 第3部分：敏感识别规则 (sec_sensitivity_rule)
-- 规则类型：COLUMN_NAME(列名匹配) / DATA_TYPE(数据类型匹配) / REGEX(正则匹配) / CUSTOM(自定义)
-- ============================================================
INSERT INTO sec_sensitivity_rule (tenant_id, rule_code, rule_name, rule_type, rule_expr, target_level_code, target_class_code, enabled, builtin, del_flag, create_dept, create_by, create_time, update_by, update_time) VALUES
-- ===== COLUMN_NAME 规则：按列名关键字识别 =====
-- 身份证号
('000000', 'COL_ID_CARD',          '身份证号识别',        'COLUMN_NAME', '["id_card","idcard","sfz","identity_card"]',                     'HIGHLY',    'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 手机号
('000000', 'COL_PHONE',            '手机号识别',           'COLUMN_NAME', '["phone","mobile","tel","telephone","sjhm","shouji"]',           'SENSITIVE', 'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 邮箱
('000000', 'COL_EMAIL',            '邮箱识别',             'COLUMN_NAME', '["email","mail","yx","youxiang"]',                             'SENSITIVE', 'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 姓名
('000000', 'COL_NAME',             '姓名识别',             'COLUMN_NAME', '["name","username","xm","xingming","real_name","nickname"]',   'SENSITIVE', 'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 银行卡号
('000000', 'COL_BANK_CARD',        '银行卡号识别',         'COLUMN_NAME', '["bank_card","card_no","card_number","yhkzh","bank_account"]','HIGHLY',    'FINANCIAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 密码
('000000', 'COL_PASSWORD',         '密码识别',             'COLUMN_NAME', '["password","pwd","passwd","secret","mima","miyao"]',         'HIGHLY',    'INTERNAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 社保号
('000000', 'COL_SOCIAL_CARD',      '社保号识别',           'COLUMN_NAME', '["social_card","social_security","sbh","shebao"]',          'HIGHLY',    'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 地址
('000000', 'COL_ADDRESS',          '地址识别',             'COLUMN_NAME', '["address","addr","dizhi","location","guapai"]',              'INNER',     'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 出生日期
('000000', 'COL_BIRTHDAY',         '出生日期识别',          'COLUMN_NAME', '["birthday","birth","birthday","csrq","csny","birth_date"]', 'INNER',     'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 薪酬/工资
('000000', 'COL_SALARY',           '薪酬工资识别',          'COLUMN_NAME', '["salary","wage","pay","gongzi","salary_amount","salary_amt"]','SENSITIVE', 'FINANCIAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 订单金额
('000000', 'COL_ORDER_AMOUNT',     '订单金额识别',          'COLUMN_NAME', '["amount","total_amount","order_amount","je","dingdanje"]',  'INNER',     'TRANSACTION', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 账户余额
('000000', 'COL_BALANCE',          '账户余额识别',          'COLUMN_NAME', '["balance","account_balance","yue","zhanghuye"]',             'SENSITIVE', 'FINANCIAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- IP地址
('000000', 'COL_IP_ADDRESS',       'IP地址识别',            'COLUMN_NAME', '["ip","ip_address","ip_addr","w_ip","dizhiip"]',             'INNER',     'AUDIT',      '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 护照号
('000000', 'COL_PASSPORT',         '护照号识别',            'COLUMN_NAME', '["passport","huzhao","hz","passport_no"]',                 'HIGHLY',    'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 车牌号
('000000', 'COL_LICENSE_PLATE',    '车牌号识别',            'COLUMN_NAME', '["license_plate","plate","chepai","cph","car_plate"]',      'INNER',     'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 驾驶证号
('000000', 'COL_DRIVE_LICENSE',    '驾驶证号识别',          'COLUMN_NAME', '["drive_license","driver_license","jsz","jiashizheng"]',    'HIGHLY',    'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 婚姻状态
('000000', 'COL_MARITAL',          '婚姻状态识别',          'COLUMN_NAME', '["marital","marriage","hyzk","hunyin"]',                   'SENSITIVE', 'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 指纹/生物特征
('000000', 'COL_BIOMETRIC',        '生物特征识别',          'COLUMN_NAME', '["fingerprint","face","biometric","biometric_data","swtz"]', 'HIGHLY',    'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 宗教信仰
('000000', 'COL_RELIGION',         '宗教信仰识别',          'COLUMN_NAME', '["religion","faith","zjx","religious"]',                 'HIGHLY',    'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 政治面貌
('000000', 'COL_POLITICAL',        '政治面貌识别',          'COLUMN_NAME', '["political","political_status","zzmm","zhengzhi"]',        'SENSITIVE', 'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 血型
('000000', 'COL_BLOOD_TYPE',       '血型识别',              'COLUMN_NAME', '["blood_type","blood","xx","xuexing","xue"]',               'SENSITIVE', 'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 信用评分
('000000', 'COL_CREDIT_SCORE',     '信用评分识别',          'COLUMN_NAME', '["credit_score","credit","xydf","xinyong"]',               'SENSITIVE', 'FINANCIAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 疾病史
('000000', 'COL_MEDICAL_HISTORY',  '疾病史识别',            'COLUMN_NAME', '["medical_history","diagnosis","illness","jb","jibing"]',   'HIGHLY',    'PERSONAL',   '1', '1', '0', 100, '1', NOW(), 0, NULL),

-- ===== DATA_TYPE 规则：按数据类型识别 =====
-- 身份证号（18位数字+字母）
('000000', 'DT_ID_CARD',           '数据类型-身份证号',     'REGEX', '^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$', 'HIGHLY', 'PERSONAL', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 手机号（中国大陆11位）
('000000', 'DT_PHONE_MOBILE',      '数据类型-手机号',       'REGEX', '^(1[3-9]\\d{9})$',                                                          'SENSITIVE', 'PERSONAL', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 邮箱
('000000', 'DT_EMAIL',            '数据类型-邮箱',         'REGEX', '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',                       'SENSITIVE', 'PERSONAL', '1', '1', '0', 100, '1', NOW(), 0, NULL);

-- ============================================================
-- 第4部分：脱敏模板 (sec_mask_template)
-- 脱敏类型：ENCRYPT(加密) / MASK(掩码) / HIDE(隐藏) / DELETE(删除) / SHUFFLE(打乱) / CUSTOM(自定义)
-- 遮蔽位置：ALL(全部) / HEAD(头部) / TAIL(尾部) / CENTER(中间)
-- ============================================================
INSERT INTO sec_mask_template (tenant_id, template_code, template_name, template_type, mask_expr, mask_char, mask_position, mask_head_keep, mask_tail_keep, mask_pattern, template_desc, builtin, enabled, del_flag, create_dept, create_by, create_time, update_by, update_time) VALUES
-- 保留前3后4（手机号脱敏）
('000000', 'PHONE_KEEP_HEAD3_TAIL4',  '手机号-保留前3后4',    'MASK',   '', '*', 'CENTER', 3, 4, NULL,
 '手机号138****5678，保留前3位和后4位，中间用*号遮蔽。适用于中国大陆11位手机号。', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 邮箱脱敏
('000000', 'EMAIL_MASK',             '邮箱-部分遮蔽',        'MASK',   '', '*', 'CENTER', 2, NULL, NULL,
 '邮箱脱敏，保留@前2个字符和域名，保留@后全部。如ab****@163.com', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 身份证号脱敏（保留前3后4）
('000000', 'ID_CARD_MASK',           '身份证号-保留前3后4',  'MASK',   '', '*', 'CENTER', 3, 4, NULL,
 '身份证号脱敏，保留前3位和后4位，中间用*号遮蔽，如410***********1234', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 姓名脱敏（只保留姓氏）
('000000', 'NAME_MASK',              '姓名-只保留姓氏',      'MASK',   '', '*', 'HEAD',   1, NULL, NULL,
 '姓名脱敏，只保留第一个字符，其余用*遮蔽，如张*', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 银行卡号脱敏（保留后4位）
('000000', 'BANK_CARD_MASK',         '银行卡号-保留后4位',   'MASK',   '', '*', 'HEAD',   0, 4, NULL,
 '银行卡号脱敏，只保留后4位，其余用*遮蔽，如**** **** **** 1234', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 密码全脱敏
('000000', 'PASSWORD_DELETE',        '密码-完全隐藏',        'DELETE', '', NULL, NULL, NULL, NULL, NULL,
 '密码类字段直接删除，不返回任何内容', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 金额四舍五入
('000000', 'AMOUNT_ROUND',           '金额-四舍五入取整',    'MASK',   'ROUND(${column}, 0)', NULL, NULL, NULL, NULL, NULL,
 '金额字段四舍五入取整数，适用于金额统计展示场景', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 金额保留2位小数
('000000', 'AMOUNT_KEEP_2_DECIMAL',  '金额-保留2位小数',     'MASK',   'ROUND(${column}, 2)', NULL, NULL, NULL, NULL, NULL,
 '金额保留2位小数，适用于精确金额展示', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- IP地址脱敏
('000000', 'IP_MASK',                'IP地址-部分遮蔽',      'MASK',   '', '*', 'TAIL',   NULL, 0, NULL,
 'IP地址脱敏，只保留前两位，如192.***.***.***', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 家庭地址脱敏
('000000', 'ADDRESS_MASK',           '地址-保留省市',        'MASK',   '', '*', 'TAIL',   NULL, 0, NULL,
 '详细地址脱敏，只保留省市区信息，街道门牌号用*遮蔽', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 邮箱域名前缀打乱
('000000', 'EMAIL_SHUFFLE',          '邮箱-前缀打乱',        'SHUFFLE', '', NULL, NULL, NULL, NULL, NULL,
 '邮箱前缀字符打乱重排，保护用户标识同时保留格式', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- AES加密
('000000', 'AES_ENCRYPT',            'AES加密',              'ENCRYPT', 'AES_ENCRYPT(${column}, SHA2(''dataplatform_secret_key_2024'', 256))', NULL, NULL, NULL, NULL, NULL,
 '使用AES算法对字段进行加密存储，适用于高敏感数据的加密存储场景', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- MD5哈希（不可逆）
('000000', 'MD5_HASH',               'MD5哈希',              'ENCRYPT', 'MD5(${column})', NULL, NULL, NULL, NULL, NULL,
 '使用MD5对字段进行不可逆哈希，适用于需要对比但不需要还原的场景（如密码比对）', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 脱敏后直接返回固定值
('000000', 'CONSTANT_MASK',          '返回固定占位符',        'MASK',   '''[已脱敏]''', NULL, NULL, NULL, NULL, NULL,
 '直接返回脱敏占位符，不返回原始数据，适用于完全不可对外展示的字段', '1', '1', '0', 100, '1', NOW(), 0, NULL);

-- ============================================================
-- 第5部分：DQC数据质量规则模板（补充）
-- 质量维度：COMPLETENESS(完整性) / UNIQUENESS(唯一性) / ACCURACY(准确性) /
--          CONSISTENCY(一致性) / VALIDITY(有效性) / TIMELINESS(时效性) / CUSTOM(自定义)
-- ============================================================
INSERT INTO dqc_rule_template (tenant_id, template_code, template_name, template_desc, rule_type, apply_level, default_expr, threshold_json, param_spec, dimension, builtin, enabled, del_flag, create_dept, create_by, create_time, update_by, update_time) VALUES
-- ===== 表级规则 =====
-- 表行数下限检测
('000000', 'TBL_ROW_COUNT_MIN',    '表行数下限检测',         '检测表行数是否低于最小阈值',
 'THRESHOLD', 'TABLE',
 'SELECT COUNT(*) AS cnt FROM ${table}',
 '{"threshold_min": 1}',
 '{"remark": "table_name=表名", "table_name": ""}',
 'COMPLETENESS', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 表行数上限检测
('000000', 'TBL_ROW_COUNT_MAX',    '表行数上限检测',         '检测表行数是否超过最大阈值（防数据爆炸）',
 'THRESHOLD', 'TABLE',
 'SELECT COUNT(*) AS cnt FROM ${table}',
 '{"threshold_max": 10000000}',
 '{"table_name": ""}',
 'COMPLETENESS', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 表大小检测
('000000', 'TBL_SIZE_CHECK',       '表存储大小检测',          '检测表存储大小是否超过阈值',
 'THRESHOLD', 'TABLE',
 'SELECT COALESCE(SUM(data_length + index_length), 0) AS size_bytes FROM information_schema.tables WHERE table_name = ''${table}''',
 '{"threshold_max": 1073741824}',
 '{"table_name": ""}',
 'COMPLETENESS', '1', '1', '0', 100, '1', NOW(), 0, NULL),

-- ===== 字段级规则 =====
-- 字段值域枚举白名单
('000000', 'COL_ENUM_WHITELIST',   '字段枚举白名单检测',      '检测字段值是否在允许的枚举值列表内',
 'REGEX', 'COLUMN',
 'SELECT COUNT(*) AS enum_violation FROM ${table} WHERE ${column} NOT IN (${enum_values}) AND ${column} IS NOT NULL',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": "", "enum_values": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段长度检测
('000000', 'COL_LENGTH_RANGE',     '字段字符串长度范围',      '检测字段字符串长度是否在指定范围内',
 'THRESHOLD', 'COLUMN',
 'SELECT COUNT(*) AS length_violation FROM ${table} WHERE LENGTH(CAST(${column} AS CHAR)) NOT BETWEEN ${min_len} AND ${max_len}',
 '{"threshold_min": 0}',
 '{"max_len": "", "min_len": "", "table_name": "", "column_name": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段小数位数检测
('000000', 'COL_DECIMAL_PLACES',   '字段小数位数检测',        '检测DECIMAL字段的小数位数是否超限',
 'THRESHOLD', 'COLUMN',
 'SELECT COUNT(*) AS decimal_violation FROM ${table} WHERE ${column} IS NOT NULL AND LENGTH(TRIM(TRAILING ''0'' FROM TRIM(TRAILING ''.'' FROM CAST(${column} AS CHAR)))) - LOCATE(''.'', TRIM(TRAILING ''0'' FROM TRIM(TRAILING ''.'' FROM CAST(${column} AS CHAR)))) > ${max_decimal}',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": "", "max_decimal": ""}',
 'ACCURACY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段JSON格式检测
('000000', 'COL_JSON_FORMAT',      '字段JSON格式检测',        '检测字段是否为合法的JSON格式',
 'THRESHOLD', 'COLUMN',
 'SELECT COUNT(*) AS json_violation FROM ${table} WHERE ${column} IS NOT NULL AND JSON_VALID(${column}) = 0',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段URL格式检测
('000000', 'COL_URL_FORMAT',        '字段URL格式检测',         '检测字段是否为合法的URL格式',
 'REGEX', 'COLUMN',
 'SELECT COUNT(*) AS url_violation FROM ${table} WHERE ${column} IS NOT NULL AND ${column} NOT REGEXP ''^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$''',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段邮箱格式检测
('000000', 'COL_EMAIL_FORMAT',     '字段邮箱格式检测',         '检测字段是否为合法的邮箱格式',
 'REGEX', 'COLUMN',
 'SELECT COUNT(*) AS email_violation FROM ${table} WHERE ${column} IS NOT NULL AND ${column} NOT REGEXP ''^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$''',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段手机号格式检测
('000000', 'COL_MOBILE_FORMAT',    '字段手机号格式检测',       '检测字段是否为中国大陆手机号格式',
 'REGEX', 'COLUMN',
 'SELECT COUNT(*) AS mobile_violation FROM ${table} WHERE ${column} IS NOT NULL AND ${column} NOT REGEXP ''^1[3-9]\\d{9}$''',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段身份证号格式检测
('000000', 'COL_IDCARD_FORMAT',    '字段身份证号格式检测',     '检测字段是否为合法身份证号格式（18位）',
 'REGEX', 'COLUMN',
 'SELECT COUNT(*) AS idcard_violation FROM ${table} WHERE ${column} IS NOT NULL AND ${column} NOT REGEXP ''^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$''',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段空字符串检测
('000000', 'COL_EMPTY_STRING',      '字段空字符串检测',         '检测字段是否存在空字符串（不同于NULL）',
 'THRESHOLD', 'COLUMN',
 'SELECT COUNT(*) AS empty_count FROM ${table} WHERE ${column} = ''''', '{"threshold_min": 0}',
 '{"table_name": "", "column_name": ""}',
 'COMPLETENESS', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段纯数字检测
('000000', 'COL_PURE_DIGIT',        '字段纯数字检测',           '检测字段值是否全为数字',
 'THRESHOLD', 'COLUMN',
 'SELECT COUNT(*) AS non_digit_count FROM ${table} WHERE ${column} IS NOT NULL AND ${column} NOT REGEXP ''^[0-9]+$''',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段纯字母检测
('000000', 'COL_PURE_LETTER',       '字段纯字母检测',           '检测字段值是否全为字母',
 'THRESHOLD', 'COLUMN',
 'SELECT COUNT(*) AS non_letter_count FROM ${table} WHERE ${column} IS NOT NULL AND ${column} NOT REGEXP ''^[a-zA-Z]+$''',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段时间格式检测
('000000', 'COL_DATE_FORMAT',       '字段日期格式检测',          '检测字段是否为有效日期格式',
 'THRESHOLD', 'COLUMN',
 'SELECT COUNT(*) AS date_violation FROM ${table} WHERE ${column} IS NOT NULL AND STR_TO_DATE(${column}, ''%Y-%m-%d'') IS NULL AND ${column} NOT REGEXP ''^\\d{4}-\\d{2}-\\d{2}$''',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段SUM值检测
('000000', 'COL_SUM_CHECK',         '字段汇总值检测',            '检测字段SUM值是否超过阈值',
 'THRESHOLD', 'COLUMN',
 'SELECT COALESCE(SUM(${column}), 0) AS sum_val FROM ${table}',
 '{"threshold_min": 0}',
 '{"table_name": "", "column_name": "", "threshold_sum": ""}',
 'ACCURACY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段NULL占比检测
('000000', 'COL_NULL_RATIO',        '字段NULL占比检测',          '检测字段空值比例是否超过阈值',
 'NULL_CHECK', 'COLUMN',
 'SELECT (COUNT(*) - COUNT(${column})) * 100.0 / NULLIF(COUNT(*), 0) AS null_pct FROM ${table}',
 '{"threshold_pct": 5}',
 '{"table_name": "", "column_name": "", "threshold_pct": ""}',
 'COMPLETENESS', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段离散度检测（标准差）
('000000', 'COL_STDDEV',            '字段离散度检测',            '检测字段值标准差是否在合理范围',
 'THRESHOLD', 'COLUMN',
 'SELECT COALESCE(STDDEV(${column}), 0) AS stddev_val FROM ${table} WHERE ${column} IS NOT NULL',
 '{"threshold_max": 10000}',
 '{"table_name": "", "column_name": "", "threshold_max": ""}',
 'ACCURACY', '1', '1', '0', 100, '1', NOW(), 0, NULL),

-- ===== 跨字段规则 =====
-- 字段A<字段B一致性
('000000', 'CROSS_A_LESS_B',        '字段A<字段B一致性',         '检测字段A是否始终小于字段B',
 'THRESHOLD', 'CROSS_FIELD',
 'SELECT COUNT(*) AS violation_count FROM ${table} WHERE NOT (${column_a} < ${column_b})',
 '{"threshold_min": 0}',
 '{"column_a": "", "column_b": "", "table_name": ""}',
 'CONSISTENCY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段A<=字段B一致性
('000000', 'CROSS_A_LE_B',          '字段A<=字段B一致性',        '检测字段A是否始终小于等于字段B',
 'THRESHOLD', 'CROSS_FIELD',
 'SELECT COUNT(*) AS violation_count FROM ${table} WHERE NOT (${column_a} <= ${column_b})',
 '{"threshold_min": 0}',
 '{"column_a": "", "column_b": "", "table_name": ""}',
 'CONSISTENCY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段日期前后一致性（开始<=结束）
('000000', 'CROSS_DATE_RANGE',      '日期范围一致性',             '检测开始日期是否在结束日期之前',
 'THRESHOLD', 'CROSS_FIELD',
 'SELECT COUNT(*) AS violation_count FROM ${table} WHERE NOT (${start_date} <= ${end_date})',
 '{"threshold_min": 0}',
 '{"start_date": "", "end_date": "", "table_name": ""}',
 'CONSISTENCY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 字段A!=字段B一致性（非等值检测）
('000000', 'CROSS_A_NE_B',          '字段A!=字段B一致性',        '检测两个字段值是否不相等',
 'THRESHOLD', 'CROSS_FIELD',
 'SELECT COUNT(*) AS violation_count FROM ${table} WHERE ${column_a} = ${column_b}',
 '{"threshold_min": 0}',
 '{"column_a": "", "column_b": "", "table_name": ""}',
 'CONSISTENCY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 金额=数量*单价一致性
('000000', 'CROSS_AMOUNT_QTY_PRICE','金额=数量*单价一致性',       '检测金额是否等于数量乘以单价',
 'THRESHOLD', 'CROSS_FIELD',
 'SELECT COUNT(*) AS violation_count FROM ${table} WHERE NOT (ABS(${amount} - ${quantity} * ${unit_price}) < 0.01)',
 '{"threshold_min": 0}',
 '{"amount": "", "quantity": "", "unit_price": "", "table_name": ""}',
 'CONSISTENCY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 折扣率一致性（0-1之间）
('000000', 'CROSS_DISCOUNT_RANGE',  '折扣率范围一致性',          '检测折扣率是否在0-1之间',
 'THRESHOLD', 'CROSS_FIELD',
 'SELECT COUNT(*) AS violation_count FROM ${table} WHERE ${discount_rate} < 0 OR ${discount_rate} > 1',
 '{"threshold_min": 0}',
 '{"discount_rate": "", "table_name": ""}',
 'VALIDITY', '1', '1', '0', 100, '1', NOW(), 0, NULL),

-- ===== 跨表规则 =====
-- 跨表字段值一致性（lookup）
('000000', 'CROSS_TBL_LOOKUP',       '跨表Lookup一致性',         '检测字段值是否在另一张表中存在',
 'THRESHOLD', 'CROSS_TABLE',
 'SELECT COUNT(*) AS lookup_violation FROM ${table} t WHERE NOT EXISTS (SELECT 1 FROM ${lookup_table} l WHERE l.${lookup_column} = t.${column})',
 '{"threshold_min": 0}',
 '{"column": "", "table_name": "", "lookup_table": "", "lookup_column": ""}',
 'CONSISTENCY', '1', '1', '0', 100, '1', NOW(), 0, NULL),
-- 跨表汇总值一致性
('000000', 'CROSS_TBL_SUM_MATCH',   '跨表汇总一致性',             '检测明细表汇总值是否等于汇总表值',
 'THRESHOLD', 'CROSS_TABLE',
 'SELECT ABS((SELECT COALESCE(SUM(${detail_column}), 0) FROM ${detail_table}) - (SELECT COALESCE(SUM(${summary_column}), 0) FROM ${summary_table})) AS sum_diff',
 '{"threshold_min": 0}',
 '{"detail_table": "", "detail_column": "", "summary_table": "", "summary_column": ""}',
 'CONSISTENCY', '1', '1', '0', 100, '1', NOW(), 0, NULL),

-- ===== 自定义SQL规则 =====
-- 自定义SQL（用户自行编写）
('000000', 'CUSTOM_SQL',             '自定义SQL规则',              '用户自行编写SQL，规则引擎执行后比较返回单值',
 'CUSTOM', 'TABLE',
 '${custom_sql}',
 '{}',
 '{"custom_sql": ""}',
 'CUSTOM', '1', '1', '0', 100, '1', NOW(), 0, NULL);

-- ============================================================
-- 第6部分：数仓分层补充 (data_layer)
-- 标准数仓分层结构：ODS -> DWD -> DWS -> ADS
-- 注意：ODS/DWD/DWS/ADS 在 ry_bigdata_v1.sql 中已存在，此处用 INSERT IGNORE 跳过
-- ============================================================
INSERT IGNORE INTO data_layer (tenant_id, layer_code, layer_name, layer_desc, layer_color, sort_order, status, remark, create_dept, create_by, create_time, update_by, update_time) VALUES
-- 维度层（可选DIM）
('000000', 'DIM', '维度数据层', 'Dimension Data Store，存储所有维度表，包括缓慢变化维度，支持拉链表设计', '#9B59B6', 5, '0', '维度建模可选层，存放所有维度信息', 100, '1', NOW(), 0, NULL),
-- 临时层（可选TMP）
('000000', 'TMP', '临时数据层', 'Temporary Data Store，ETL过程中的临时中间结果，加工完成后清理', '#95A5A6', 6, '0', '临时存放中间加工结果，不保留历史', 100, '1', NOW(), 0, NULL);

-- ============================================================
-- 第7部分：数据域补充 (data_domain)
-- 数据域划分原则：按业务主题/部门划分
-- ============================================================
INSERT INTO data_domain (tenant_id, domain_name, domain_code, domain_desc, owner_id, dept_id, status, remark, del_flag, create_dept, create_by, create_time, update_by, update_time) VALUES
-- 用户域
('000000', '用户域',   'USER',        '用户注册、登录、认证、会员体系等用户相关数据',     NULL, 100, '0', '涵盖用户全生命周期数据',  '0', 100, '1', NOW(), 0, NULL),
-- 会员域
('000000', '会员域',   'MEMBER',      '会员等级、积分、权益、成长值等会员相关数据',       NULL, 100, '0', '会员运营相关数据',         '0', 100, '1', NOW(), 0, NULL),
-- 商品域
('000000', '商品域',   'PRODUCT',     '商品信息、SKU、SPU、类目、属性、价格等商品数据',   NULL, 100, '0', '商品主数据',               '0', 100, '1', NOW(), 0, NULL),
-- 交易域
('000000', '交易域',   'TRADE',       '订单、支付、退款、优惠券、促销活动等交易相关数据',  NULL, 100, '0', '核心交易业务域',           '0', 100, '1', NOW(), 0, NULL),
-- 库存域
('000000', '库存域',   'INVENTORY',   '采购、库存、出入库、仓储、物流等供应链数据',         NULL, 100, '0', '供应链库存管理',           '0', 100, '1', NOW(), 0, NULL),
-- 营销域
('000000', '营销域',   'MARKETING',   '促销活动、广告投放、内容运营、用户触达等营销数据',  NULL, 100, '0', '营销活动相关数据',         '0', 100, '1', NOW(), 0, NULL),
-- 内容域
('000000', '内容域',   'CONTENT',     '内容发布、评论、点赞、收藏、内容推荐等数据',         NULL, 100, '0', '社区/内容平台数据',         '0', 100, '1', NOW(), 0, NULL),
-- 财务域
('000000', '财务域',   'FINANCE',     '收入、支出、利润、应收账款、发票等财务数据',         NULL, 100, '0', '财务核算相关数据',         '0', 100, '1', NOW(), 0, NULL),
-- 客服域
('000000', '客服域',   'CUSTOMER_SERVICE', '工单、客诉、满意度调查、在线客服等客服数据',   NULL, 100, '0', '客户服务与支持',           '0', 100, '1', NOW(), 0, NULL),
-- 行为域
('000000', '行为域',   'BEHAVIOR',    '用户浏览、点击、搜索、停留时长等用户行为埋点数据',   NULL, 100, '0', '用户行为分析数据',         '0', 100, '1', NOW(), 0, NULL),
-- 风控域
('000000', '风控域',   'RISK',        '欺诈检测、信用评估、风险预警、安全策略等风控数据',   NULL, 100, '0', '风险控制与安全',           '0', 100, '1', NOW(), 0, NULL),
-- 公共域
('000000', '公共域',   'COMMON',      '地区字典、机构字典、行业分类等公共基础参考数据',     NULL, 100, '0', '跨业务可复用的公共数据',   '0', 100, '1', NOW(), 0, NULL),
-- 日志域
('000000', '日志域',   'LOG',         '系统日志、应用日志、审计日志、安全日志等日志数据',   NULL, 100, '0', '可观测性和审计数据',       '0', 100, '1', NOW(), 0, NULL),
-- 标签域
('000000', '标签域',   'TAG',         '用户标签、商品标签、行为标签等各类标签数据',         NULL, 100, '0', '标签体系与画像数据',       '0', 100, '1', NOW(), 0, NULL),
-- 实时域
('000000', '实时域',   'RT',          '实时计算、实时大屏、流式处理等实时数据',             NULL, 100, '0', '实时数据处理与分析',       '0', 100, '1', NOW(), 0, NULL);

-- ============================================================
-- 执行完成
-- ============================================================
SELECT '基础数据初始化完成！' AS message;
SELECT CONCAT('敏感等级: ', COUNT(*), ' 条') AS stat FROM sec_level;
SELECT CONCAT('数据分类: ', COUNT(*), ' 条') AS stat FROM sec_classification;
SELECT CONCAT('敏感识别规则: ', COUNT(*), ' 条') AS stat FROM sec_sensitivity_rule;
SELECT CONCAT('脱敏模板: ', COUNT(*), ' 条') AS stat FROM sec_mask_template;
SELECT CONCAT('DQC规则模板: ', COUNT(*), ' 条') AS stat FROM dqc_rule_template;
SELECT CONCAT('数仓分层: ', COUNT(*), ' 条') AS stat FROM data_layer;
SELECT CONCAT('数据域: ', COUNT(*), ' 条') AS stat FROM data_domain;
