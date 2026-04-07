-- 脱敏模板：对齐治理端字段（掩码字符、位置、保留位数、掩码正则/高级规则）
ALTER TABLE `sec_mask_template`
  MODIFY COLUMN `mask_expr` varchar(500) DEFAULT NULL COMMENT '脱敏表达式/SQL片段（执行层，可选）',
  ADD COLUMN `mask_char` varchar(1) DEFAULT '*' COMMENT '掩码字符' AFTER `mask_expr`,
  ADD COLUMN `mask_position` varchar(20) DEFAULT 'CENTER' COMMENT '遮蔽位置 ALL/HEAD/TAIL/CENTER' AFTER `mask_char`,
  ADD COLUMN `mask_head_keep` int DEFAULT 3 COMMENT '保留头部字符数' AFTER `mask_position`,
  ADD COLUMN `mask_tail_keep` int DEFAULT 4 COMMENT '保留尾部字符数' AFTER `mask_head_keep`,
  ADD COLUMN `mask_pattern` varchar(500) DEFAULT NULL COMMENT '掩码正则或高级替换说明' AFTER `mask_tail_keep`;

UPDATE `sec_mask_template` SET `mask_char` = '*' WHERE `mask_char` IS NULL OR `mask_char` = '';
