-- 数据质量规则定义表
CREATE TABLE IF NOT EXISTS dqc_rule_def (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    template_id BIGINT,
    rule_type VARCHAR(50) NOT NULL,
    apply_level VARCHAR(20),
    dimensions VARCHAR(500),
    rule_expr TEXT,
    target_ds_id BIGINT,
    target_table VARCHAR(200),
    target_column VARCHAR(100),
    compare_ds_id BIGINT,
    compare_table VARCHAR(200),
    compare_column VARCHAR(100),
    threshold_min DECIMAL(20,4),
    threshold_max DECIMAL(20,4),
    fluctuation_threshold DECIMAL(5,2),
    regex_pattern VARCHAR(500),
    error_level VARCHAR(20) DEFAULT 'MEDIUM',
    rule_strength VARCHAR(10) DEFAULT 'WEAK',
    alert_receivers VARCHAR(500),
    sort_order INT DEFAULT 0,
    enabled TINYINT DEFAULT 1,
    dept_id BIGINT,
    deleted INT DEFAULT 0,
    create_user BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_user BIGINT,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    custom_function_class VARCHAR(500),
    custom_function_params TEXT
);

-- 数据源表
CREATE TABLE IF NOT EXISTS dq_datasource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ds_name VARCHAR(100) NOT NULL,
    ds_code VARCHAR(50) NOT NULL UNIQUE,
    ds_type VARCHAR(30) NOT NULL,
    host VARCHAR(255),
    port INT,
    database_name VARCHAR(100),
    username VARCHAR(100),
    password VARCHAR(255),
    connection_params TEXT,
    data_layer VARCHAR(20),
    dept_id BIGINT,
    owner_id BIGINT,
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 数据层配置表
CREATE TABLE IF NOT EXISTS dq_data_layer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    layer_code VARCHAR(20) NOT NULL UNIQUE,
    layer_name VARCHAR(50) NOT NULL,
    layer_desc VARCHAR(200),
    sort_order INT DEFAULT 0
);

-- 插入测试数据
INSERT INTO dq_data_layer (layer_code, layer_name, layer_desc, sort_order) VALUES
('ODS', 'ODS层', '原始数据层', 1),
('DWD', 'DWD层', '明细数据层', 2),
('DWS', 'DWS层', '汇总数据层', 3),
('ADS', 'ADS层', '应用数据层', 4);

-- 插入测试数据源
INSERT INTO dq_datasource (ds_name, ds_code, ds_type, host, port, database_name, username, password, data_layer, status) VALUES
('测试MySQL', 'TEST_MYSQL', 'MYSQL', 'localhost', 3306, 'test_db', 'root', 'password', 'DWD', 1),
('测试PostgreSQL', 'TEST_POSTGRESQL', 'POSTGRESQL', 'localhost', 5432, 'test_db', 'postgres', 'password', 'DWS', 1);

-- 数据质量规则模板表
CREATE TABLE IF NOT EXISTS dqc_rule_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(50) NOT NULL UNIQUE,
    template_name VARCHAR(100) NOT NULL,
    template_desc VARCHAR(500),
    rule_type VARCHAR(50) NOT NULL,
    apply_level VARCHAR(20),
    default_expr TEXT,
    default_threshold TEXT,
    param_spec TEXT,
    builtin TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入内置规则模板
INSERT INTO dqc_rule_template (template_code, template_name, template_desc, rule_type, apply_level, builtin) VALUES
('NULL_CHECK', '空值检查', '检测字段是否为空', 'NULL_CHECK', 'COLUMN', 1),
('UNIQUE_CHECK', '唯一性检查', '检测字段值是否唯一', 'UNIQUE_CHECK', 'COLUMN', 1),
('REGEX_PHONE', '手机号格式检查', '检测手机号格式', 'REGEX_PHONE', 'COLUMN', 1),
('ROW_COUNT_NOT_ZERO', '行数非0检查', '检测表是否有数据', 'ROW_COUNT_NOT_ZERO', 'TABLE', 1),
('ROW_COUNT_FLUCTUATION', '行数波动检查', '检测表行数波动', 'ROW_COUNT_FLUCTUATION', 'TABLE', 1),
('THRESHOLD_RANGE', '值域范围检查', '检测字段值是否在范围内', 'THRESHOLD_RANGE', 'COLUMN', 1);

-- 质检方案表
CREATE TABLE IF NOT EXISTS dqc_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_name VARCHAR(100) NOT NULL,
    plan_code VARCHAR(50) NOT NULL UNIQUE,
    plan_desc VARCHAR(500),
    bind_type VARCHAR(20),
    bind_value TEXT,
    layer_code VARCHAR(20),
    dept_id BIGINT,
    trigger_type VARCHAR(20) DEFAULT 'MANUAL',
    trigger_cron VARCHAR(100),
    alert_on_success TINYINT DEFAULT 0,
    alert_on_failure TINYINT DEFAULT 1,
    auto_block TINYINT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'DRAFT',
    create_user BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 方案-规则关联表
CREATE TABLE IF NOT EXISTS dqc_plan_rule (
    plan_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    rule_order INT DEFAULT 0,
    custom_threshold TEXT,
    skip_on_failure TINYINT DEFAULT 0,
    PRIMARY KEY (plan_id, rule_id)
);

-- 质检执行记录表
CREATE TABLE IF NOT EXISTS dqc_execution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_no VARCHAR(64) NOT NULL UNIQUE,
    plan_id BIGINT,
    plan_name VARCHAR(100),
    rule_id BIGINT,
    rule_name VARCHAR(100),
    target_table VARCHAR(200),
    trigger_type VARCHAR(20),
    trigger_user BIGINT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    elapsed_ms BIGINT,
    status VARCHAR(20),
    total_count BIGINT,
    error_count BIGINT,
    quality_score DECIMAL(5,2),
    score_breakdown TEXT,
    error_detail TEXT,
    log_path VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 质量评分历史表
CREATE TABLE IF NOT EXISTS dqc_quality_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_date DATE NOT NULL,
    target_ds_id BIGINT,
    target_table VARCHAR(200),
    layer_code VARCHAR(20),
    dept_id BIGINT,
    completeness_score DECIMAL(5,2),
    uniqueness_score DECIMAL(5,2),
    accuracy_score DECIMAL(5,2),
    consistency_score DECIMAL(5,2),
    timeliness_score DECIMAL(5,2),
    validity_score DECIMAL(5,2),
    overall_score DECIMAL(5,2),
    rule_pass_rate DECIMAL(5,2),
    rule_total_count INT,
    rule_pass_count INT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_check_date_table (check_date, target_ds_id, target_table)
);

-- 元数据表
CREATE TABLE IF NOT EXISTS gov_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ds_id BIGINT NOT NULL,
    table_name VARCHAR(200) NOT NULL,
    table_alias VARCHAR(200),
    table_comment VARCHAR(1000),
    table_type VARCHAR(30) DEFAULT 'TABLE',
    data_layer VARCHAR(20),
    data_domain VARCHAR(50),
    biz_domain VARCHAR(50),
    lifecycle_days INT,
    is_partitioned TINYINT DEFAULT 0,
    partition_column VARCHAR(100),
    storage_bytes BIGINT,
    row_count BIGINT,
    access_freq INT DEFAULT 0,
    sensitivity_level VARCHAR(20) DEFAULT 'NORMAL',
    owner_id BIGINT,
    dept_id BIGINT,
    tags VARCHAR(500),
    last_profiled_at TIMESTAMP,
    last_modified_at TIMESTAMP,
    etl_source VARCHAR(200),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ds_table (ds_id, table_name)
);

-- 血缘表
CREATE TABLE IF NOT EXISTS gov_lineage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lineage_type VARCHAR(20) NOT NULL,
    source_ds_id BIGINT NOT NULL,
    source_table VARCHAR(200) NOT NULL,
    source_column VARCHAR(100),
    source_column_alias VARCHAR(200),
    target_ds_id BIGINT NOT NULL,
    target_table VARCHAR(200) NOT NULL,
    target_column VARCHAR(100),
    target_column_alias VARCHAR(200),
    transform_type VARCHAR(50),
    transform_expr TEXT,
    job_id BIGINT,
    job_name VARCHAR(200),
    lineage_source VARCHAR(20) DEFAULT 'MANUAL',
    dept_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    create_user BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 告警规则表
CREATE TABLE IF NOT EXISTS monitor_alert_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    rule_code VARCHAR(50) NOT NULL UNIQUE,
    rule_type VARCHAR(30) NOT NULL,
    target_type VARCHAR(30),
    target_id VARCHAR(100),
    condition_type VARCHAR(20),
    threshold_value DECIMAL(20,6),
    fluctuation_pct DECIMAL(5,2),
    comparison_type VARCHAR(20),
    alert_level VARCHAR(20) DEFAULT 'WARN',
    alert_receivers VARCHAR(500),
    mute_until TIMESTAMP,
    enabled TINYINT DEFAULT 1,
    dept_id BIGINT,
    create_user BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 告警记录表
CREATE TABLE IF NOT EXISTS monitor_alert_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_no VARCHAR(64) NOT NULL UNIQUE,
    rule_id BIGINT,
    rule_name VARCHAR(100),
    alert_level VARCHAR(20),
    alert_title VARCHAR(200) NOT NULL,
    alert_content TEXT,
    target_type VARCHAR(30),
    target_id VARCHAR(100),
    trigger_value DECIMAL(20,6),
    threshold_value DECIMAL(20,6),
    status VARCHAR(20) DEFAULT 'PENDING',
    sent_channels VARCHAR(200),
    sent_time TIMESTAMP,
    read_time TIMESTAMP,
    resolved_time TIMESTAMP,
    resolve_user BIGINT,
    resolve_comment VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
