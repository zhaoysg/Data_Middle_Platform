-- 数据分类：默认关联敏感等级（与 bagdatahouse 分类管理对齐，供列表「关联等级」与表单选择）
ALTER TABLE `sec_classification`
  ADD COLUMN `default_level_code` varchar(50) DEFAULT NULL COMMENT '默认关联敏感等级编码（关联 sec_level.level_code）' AFTER `sort_order`;
