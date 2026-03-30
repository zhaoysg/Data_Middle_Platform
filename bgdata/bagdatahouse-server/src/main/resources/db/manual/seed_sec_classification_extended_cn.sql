-- =============================================================================
-- 可选：扩展「数据分类」与「分类-等级推荐绑定」示例数据（偏国标/行业实践表述）
-- 使用方式：在 MySQL 客户端执行，或：mysql ... < seed_sec_classification_extended_cn.sql
-- 说明：不通过 Flyway 自动执行；id 从 5 起，避免与内置 1～4 冲突。
-- =============================================================================

-- ---------- 扩展分类 ----------
INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 5, 'USER_IDENTITY', '用户身份信息', '可直接或结合其他信息识别特定自然人的数据（姓名、证件号、住址、生物识别等）', 5, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sec_classification` WHERE `id` = 5);

INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 6, 'USER_LOCATION', '位置轨迹数据', '精确或大致地理位置、轨迹、常驻地等与位置相关的数据', 6, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sec_classification` WHERE `id` = 6);

INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 7, 'COMMUNICATION', '通信与内容数据', '通信记录、邮件正文、即时消息内容、通话录音等', 7, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sec_classification` WHERE `id` = 7);

INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 8, 'DEVICE_NETWORK', '设备与网络标识', '设备唯一标识、IMEI、MAC、Cookie、广告标识符等', 8, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sec_classification` WHERE `id` = 8);

INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 9, 'HEALTH_MEDICAL', '医疗健康数据', '诊疗、检验、用药、基因、健康档案等医疗相关数据', 9, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sec_classification` WHERE `id` = 9);

INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 10, 'MINOR_PROTECT', '未成年人信息', '不满十四周岁未成年人个人信息，需更严格保护', 10, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sec_classification` WHERE `id` = 10);

INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 11, 'TRANSACTION', '交易与订单数据', '订单号、支付流水、交易对手、金额、合约条款等', 11, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sec_classification` WHERE `id` = 11);

INSERT INTO `sec_classification` (`id`, `class_code`, `class_name`, `class_desc`, `class_order`, `status`, `deleted`, `create_time`)
SELECT 12, 'SYSTEM_SECURITY', '系统与安全运维', '账号凭证、密钥、审计日志、漏洞与配置等安全相关数据', 12, 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sec_classification` WHERE `id` = 12);

-- ---------- 推荐绑定（分类 → 等级；等级 id 与 V14 种子一致：L1=1 L2=2 L3=3 L4=4）----------
INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 5, 4, 1, '用户身份信息常见最高风险：推荐机密'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 5 AND `level_id` = 4 AND `deleted` = 0);

INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 5, 3, 0, '一般业务场景可为敏感'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 5 AND `level_id` = 3 AND `deleted` = 0);

INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 6, 3, 1, '位置轨迹：多为敏感'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 6 AND `level_id` = 3 AND `deleted` = 0);

INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 7, 3, 1, '通信内容：多为敏感及以上'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 7 AND `level_id` = 3 AND `deleted` = 0);

INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 8, 2, 1, '设备标识：常见为内部'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 8 AND `level_id` = 2 AND `deleted` = 0);

INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 9, 4, 1, '医疗健康：推荐机密'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 9 AND `level_id` = 4 AND `deleted` = 0);

INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 10, 4, 1, '未成年人信息：从严，推荐机密'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 10 AND `level_id` = 4 AND `deleted` = 0);

INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 11, 4, 0, '交易数据：高敏感场景可为机密'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 11 AND `level_id` = 4 AND `deleted` = 0);

INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 11, 3, 1, '交易数据：常见为敏感'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 11 AND `level_id` = 3 AND `deleted` = 0);

INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 12, 4, 1, '密钥与审计：推荐机密'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 12 AND `level_id` = 4 AND `deleted` = 0);

INSERT INTO `sec_class_level_binding` (`class_id`, `level_id`, `is_recommended`, `binding_desc`)
SELECT 12, 3, 0, '部分日志可为敏感'
WHERE NOT EXISTS (SELECT 1 FROM `sec_class_level_binding` WHERE `class_id` = 12 AND `level_id` = 3 AND `deleted` = 0);
