INSERT INTO ry_bigdata_v1.data_domain (tenant_id,domain_name,domain_code,domain_desc,owner_id,dept_id,status,remark,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000','11','111','222',NULL,100,'0',NULL,'0',103,'1','2026-03-31 13:54:08','1','2026-03-31 16:45:49');
INSERT INTO ry_bigdata_v1.data_layer (tenant_id,layer_code,layer_name,layer_desc,layer_color,sort_order,status,remark,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000','ODS','操作数据层','Operational Data Store，承接所有业务系统原始数据，保持与源系统一致的结构','#909399',1,'0',NULL,NULL,'-1','2026-03-31 07:48:53','1','2026-03-31 21:51:13'),
	 ('000000','DWD','明细数据层','Data Warehouse Detail，对ODS层数据进行清洗和规范化处理','#409EFF',2,'0',NULL,NULL,'-1','2026-03-31 07:48:53','','2026-03-31 21:51:13'),
	 ('000000','DWS','汇总数据层','Data Warehouse Summary，按业务主题进行汇总加工','#67C23A',3,'0',NULL,NULL,'-1','2026-03-31 07:48:53','','2026-03-31 21:51:13'),
	 ('000000','ADS','应用数据层','Application Data Store，面向应用和报表的集市层','#E6A23C',4,'0',NULL,NULL,'-1','2026-03-31 07:48:53','','2026-03-31 21:51:13');
INSERT INTO ry_bigdata_v1.dqc_rule_template (tenant_id,template_code,template_name,template_desc,rule_type,apply_level,default_expr,threshold_json,param_spec,dimension,builtin,enabled,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000','TBL_ROW_COUNT_NULL','表行数空值检测','检测表行数是否为0或NULL','NULL_CHECK','TABLE','SELECT COUNT(*) AS cnt FROM ${table}','{"threshold_min": 1}','{"remark": "table_name=表名", "table_name": ""}','COMPLETENESS','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','TBL_ROW_COUNT_FLUCTUATION','表行数波动检测','检测表行数相对上次波动是否超过阈值','FLUCTUATION','TABLE','SELECT COUNT(*) AS cnt FROM ${table}','{"fluctuation_threshold": 30}','{"table_name": "", "threshold_pct": ""}','TIMELINESS','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','TBL_UPDATE_TIMELINESS','表更新时效检测','检测表最后更新时间距今是否超过阈值','THRESHOLD','TABLE','SELECT MAX(${update_column}) AS last_update FROM ${table}','{"threshold_hours": 24}','{"table_name": "", "update_column": ""}','TIMELINESS','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','COL_NULL_CHECK','字段空值检测','检测字段空值比例是否超过阈值','NULL_CHECK','COLUMN','SELECT (COUNT(*) - COUNT(${column})) * 100.0 / NULLIF(COUNT(*), 0) AS null_pct FROM ${table}','{"threshold_pct": 5}','{"table_name": "", "column_name": "", "threshold_pct": ""}','COMPLETENESS','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','COL_UNIQUE_CHECK','字段唯一性检测','检测字段值是否唯一','UNIQUE','COLUMN','SELECT COUNT(*) - COUNT(DISTINCT ${column}) AS dup_count FROM ${table}','{"threshold_min": 0}','{"table_name": "", "column_name": ""}','UNIQUENESS','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','COL_DUPLICATE_COUNT','字段重复值检测','检测字段重复次数最多的值及次数','UNIQUE','COLUMN','SELECT ${column}, COUNT(*) AS cnt FROM ${table} GROUP BY ${column} ORDER BY cnt DESC LIMIT 1','{"max_dup_count": 1000}','{"table_name": "", "column_name": "", "max_dup_count": ""}','UNIQUENESS','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','COL_VALUE_RANGE','字段值域检测','检测字段值是否在合法值域内','THRESHOLD','COLUMN','SELECT COUNT(*) AS invalid_count FROM ${table} WHERE ${column} NOT BETWEEN ${min_value} AND ${max_value}','{"threshold_min": 0}','{"max_value": "", "min_value": "", "table_name": "", "column_name": ""}','VALIDITY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','COL_ENUM_CHECK','字段枚举值检测','检测字段值是否在允许的枚举值列表内','REGEX','COLUMN','SELECT COUNT(*) AS enum_violation FROM ${table} WHERE ${column} NOT IN (${enum_values})','{"threshold_min": 0}','{"table_name": "", "column_name": "", "enum_values": ""}','CONSISTENCY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','COL_REGEX_PATTERN','字段正则匹配检测','检测字段值是否符合指定正则表达式','REGEX','COLUMN','SELECT COUNT(*) AS pattern_violation FROM ${table} WHERE ${column} IS NOT NULL AND ${column} NOT REGEXP ${pattern}','{"threshold_min": 0}','{"pattern": "", "table_name": "", "column_name": ""}','VALIDITY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','COL_MIN_MAX','字段最小值检测','检测字段最小值是否低于阈值','THRESHOLD','COLUMN','SELECT COALESCE(MIN(${column}), 0) AS min_val FROM ${table}','{"threshold_min": 0}','{"table_name": "", "column_name": "", "min_threshold": ""}','ACCURACY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL);
INSERT INTO ry_bigdata_v1.dqc_rule_template (tenant_id,template_code,template_name,template_desc,rule_type,apply_level,default_expr,threshold_json,param_spec,dimension,builtin,enabled,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000','COL_MAX_VALUE','字段最大值检测','检测字段最大值是否超过阈值','THRESHOLD','COLUMN','SELECT COALESCE(MAX(${column}), 0) AS max_val FROM ${table}','{"threshold_max": 999999999}','{"table_name": "", "column_name": "", "max_threshold": ""}','ACCURACY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','COL_AVG_FLUCTUATION','字段平均值波动检测','检测字段平均值相对上次波动','FLUCTUATION','COLUMN','SELECT AVG(${column}) AS avg_val FROM ${table}','{"fluctuation_threshold": 20}','{"table_name": "", "column_name": "", "threshold_pct": ""}','TIMELINESS','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','COL_STRING_LENGTH','字段字符串长度检测','检测字段字符串长度是否在指定范围内','THRESHOLD','COLUMN','SELECT COUNT(*) AS length_violation FROM ${table} WHERE LENGTH(${column}) NOT BETWEEN ${min_len} AND ${max_len}','{"threshold_min": 0}','{"max_len": "", "min_len": "", "table_name": "", "column_name": ""}','VALIDITY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','COL_Z_SCORE','字段Z-Score异常检测','检测字段值Z-Score超过阈值的数据比例','THRESHOLD','COLUMN','SELECT (SELECT COUNT(*) FROM ${table} WHERE ABS((${column} - (SELECT AVG(${column}) FROM ${table})) / NULLIF(STDDEV(${column}), 0)) > ${z_threshold}) * 100.0 / NULLIF((SELECT COUNT(*) FROM ${table}), 0) AS anomaly_pct','{"threshold_pct": 5}','{"table_name": "", "column_name": "", "z_threshold": ""}','ACCURACY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','CROSS_A_GREATER_B','字段A>字段B一致性','检测字段A是否始终大于字段B','THRESHOLD','CROSS_FIELD','SELECT COUNT(*) AS violation_count FROM ${table} WHERE NOT (${column_a} > ${column_b})','{"threshold_min": 0}','{"column_a": "", "column_b": "", "table_name": ""}','CONSISTENCY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','CROSS_A_PLUS_B','字段A+B=C一致性','检测字段A+字段B是否等于字段C','THRESHOLD','CROSS_FIELD','SELECT COUNT(*) AS violation_count FROM ${table} WHERE NOT (${column_a} + ${column_b} = ${column_c})','{"threshold_min": 0}','{"column_a": "", "column_b": "", "column_c": "", "table_name": ""}','CONSISTENCY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','CROSS_NULL_CONSISTENCY','字段空值一致性检测','检测两个字段的空值是否同时存在或同时不存在','THRESHOLD','CROSS_FIELD','SELECT COUNT(*) AS null_inconsistency FROM ${table} WHERE (${column_a} IS NULL) <> (${column_b} IS NULL)','{"threshold_min": 0}','{"column_a": "", "column_b": "", "table_name": ""}','CONSISTENCY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','CROSS_TBL_ROW_COUNT','跨表记录数一致性','检测两张表的记录数是否一致','THRESHOLD','CROSS_TABLE','SELECT ABS((SELECT COUNT(*) FROM ${table}) - (SELECT COUNT(*) FROM ${compare_table})) AS row_diff','{"threshold_min": 0}','{"table_name": "", "compare_table": ""}','CONSISTENCY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','CROSS_TBL_PK_CONSISTENCY','跨表主键一致性','检测两张表的主键是否完全一致','THRESHOLD','CROSS_TABLE','SELECT (SELECT COUNT(*) FROM (SELECT ${pk_column} FROM ${table} UNION SELECT ${pk_column} FROM ${compare_table}) t) - (SELECT COUNT(*) FROM ${table} WHERE ${pk_column} IN (SELECT ${pk_column} FROM ${compare_table})) AS key_diff','{"threshold_min": 0}','{"pk_column": "", "table_name": "", "compare_table": ""}','CONSISTENCY','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL),
	 ('000000','CUSTOM_SQL','自定义SQL规则','用户自定义SQL校验规则，返回单值供阈值比对','CUSTOM','TABLE','${custom_sql}','{}','{"custom_sql": ""}','CUSTOM','1','1','0',NULL,'admin','2026-03-31 08:55:43','',NULL);
INSERT INTO ry_bigdata_v1.metadata_catalog (tenant_id,catalog_name,catalog_code,catalog_type,parent_id,sort_order,status,remark,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000','12','2','BUSINESS_DOMAIN',0,0,'0',NULL,'0',103,'1','2026-03-31 19:55:57','1','2026-03-31 19:55:57');
INSERT INTO ry_bigdata_v1.metadata_column (tenant_id,table_id,ds_id,table_name,column_name,column_alias,column_comment,data_type,is_nullable,column_key,default_value,is_primary_key,is_foreign_key,fk_reference,is_sensitive,sensitivity_level,sort_order,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000',2038856910367727618,2038582379342303233,'category','id',NULL,'','bigint','NO',NULL,NULL,1,NULL,NULL,NULL,NULL,1,'0',103,'1','2026-03-31 22:10:59','1','2026-03-31 22:12:39'),
	 ('000000',2038856910367727618,2038582379342303233,'category','user_id',NULL,'','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'0',103,'1','2026-03-31 22:10:59','1','2026-03-31 22:12:39'),
	 ('000000',2038856910367727618,2038582379342303233,'category','name',NULL,'','varchar','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'0',103,'1','2026-03-31 22:10:59','1','2026-03-31 22:12:39'),
	 ('000000',2038856910367727618,2038582379342303233,'category','color',NULL,'','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,4,'0',103,'1','2026-03-31 22:10:59','1','2026-03-31 22:12:39'),
	 ('000000',2038856910367727618,2038582379342303233,'category','icon',NULL,'','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,5,'0',103,'1','2026-03-31 22:10:59','1','2026-03-31 22:12:39'),
	 ('000000',2038856910367727618,2038582379342303233,'category','description',NULL,'分类描述','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,6,'0',103,'1','2026-03-31 22:10:59','1','2026-03-31 22:12:39'),
	 ('000000',2038856910367727618,2038582379342303233,'category','sort_order',NULL,'','int','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,7,'0',103,'1','2026-03-31 22:10:59','1','2026-03-31 22:12:39'),
	 ('000000',2038856910367727618,2038582379342303233,'category','created_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,8,'0',103,'1','2026-03-31 22:10:59','1','2026-03-31 22:12:39'),
	 ('000000',2038856910761992194,2038582379342303233,'chat_message','id',NULL,'','bigint','NO',NULL,NULL,1,NULL,NULL,NULL,NULL,1,'0',103,'1','2026-03-31 22:12:02','1','2026-03-31 22:12:39'),
	 ('000000',2038856910761992194,2038582379342303233,'chat_message','session_id',NULL,'','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'0',103,'1','2026-03-31 22:12:02','1','2026-03-31 22:12:39');
INSERT INTO ry_bigdata_v1.metadata_column (tenant_id,table_id,ds_id,table_name,column_name,column_alias,column_comment,data_type,is_nullable,column_key,default_value,is_primary_key,is_foreign_key,fk_reference,is_sensitive,sensitivity_level,sort_order,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000',2038856910761992194,2038582379342303233,'chat_message','role',NULL,'user / ai / system','varchar','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'0',103,'1','2026-03-31 22:12:02','1','2026-03-31 22:12:39'),
	 ('000000',2038856910761992194,2038582379342303233,'chat_message','content',NULL,'','longtext','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,4,'0',103,'1','2026-03-31 22:12:02','1','2026-03-31 22:12:39'),
	 ('000000',2038856910761992194,2038582379342303233,'chat_message','ai_metadata',NULL,'AI 回复的元数据','json','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,5,'0',103,'1','2026-03-31 22:12:02','1','2026-03-31 22:12:39'),
	 ('000000',2038856910761992194,2038582379342303233,'chat_message','note_id',NULL,'关联保存的笔记ID','bigint','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,6,'0',103,'1','2026-03-31 22:12:02','1','2026-03-31 22:12:39'),
	 ('000000',2038856910761992194,2038582379342303233,'chat_message','created_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,7,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:39'),
	 ('000000',2038856911101730818,2038582379342303233,'chat_session','id',NULL,'','bigint','NO',NULL,NULL,1,NULL,NULL,NULL,NULL,1,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911101730818,2038582379342303233,'chat_session','user_id',NULL,'','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911101730818,2038582379342303233,'chat_session','note_id',NULL,'关联的笔记ID','bigint','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911101730818,2038582379342303233,'chat_session','title',NULL,'','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,4,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911101730818,2038582379342303233,'chat_session','created_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,5,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40');
INSERT INTO ry_bigdata_v1.metadata_column (tenant_id,table_id,ds_id,table_name,column_name,column_alias,column_comment,data_type,is_nullable,column_key,default_value,is_primary_key,is_foreign_key,fk_reference,is_sensitive,sensitivity_level,sort_order,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000',2038856911487606786,2038582379342303233,'merge_suggestion','id',NULL,'','bigint','NO',NULL,NULL,1,NULL,NULL,NULL,NULL,1,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911487606786,2038582379342303233,'merge_suggestion','user_id',NULL,'','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911487606786,2038582379342303233,'merge_suggestion','source_note_id',NULL,'被建议合并到目标笔记的源笔记','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911487606786,2038582379342303233,'merge_suggestion','target_note_id',NULL,'目标笔记ID','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,4,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911487606786,2038582379342303233,'merge_suggestion','similarity_score',NULL,'相似度得分 0.0000-1.0000','decimal','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,5,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911487606786,2038582379342303233,'merge_suggestion','status',NULL,'pending / accepted / dismissed','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,6,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911487606786,2038582379342303233,'merge_suggestion','created_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,7,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911487606786,2038582379342303233,'merge_suggestion','handled_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,8,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911802179585,2038582379342303233,'note','id',NULL,'','bigint','NO',NULL,NULL,1,NULL,NULL,NULL,NULL,1,'0',103,'1','2026-03-31 22:12:03','1','2026-03-31 22:12:40'),
	 ('000000',2038856911802179585,2038582379342303233,'note','user_id',NULL,'','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41');
INSERT INTO ry_bigdata_v1.metadata_column (tenant_id,table_id,ds_id,table_name,column_name,column_alias,column_comment,data_type,is_nullable,column_key,default_value,is_primary_key,is_foreign_key,fk_reference,is_sensitive,sensitivity_level,sort_order,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000',2038856911802179585,2038582379342303233,'note','title',NULL,'','varchar','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856911802179585,2038582379342303233,'note','content',NULL,'','longtext','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,4,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856911802179585,2038582379342303233,'note','summary',NULL,'','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,5,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856911802179585,2038582379342303233,'note','source',NULL,'来源：chat_input / manual / merge','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,6,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856911802179585,2038582379342303233,'note','merged_from_ids',NULL,'被合并的笔记ID列表','json','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,7,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856911802179585,2038582379342303233,'note','category_id',NULL,'','bigint','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,8,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856911802179585,2038582379342303233,'note','ai_metadata',NULL,'AI元数据：分类结果、标签、推荐图片等','json','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,9,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856911802179585,2038582379342303233,'note','view_count',NULL,'','int','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,10,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856911802179585,2038582379342303233,'note','created_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,11,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856911802179585,2038582379342303233,'note','updated_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,12,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41');
INSERT INTO ry_bigdata_v1.metadata_column (tenant_id,table_id,ds_id,table_name,column_name,column_alias,column_comment,data_type,is_nullable,column_key,default_value,is_primary_key,is_foreign_key,fk_reference,is_sensitive,sensitivity_level,sort_order,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000',2038856912129335297,2038582379342303233,'note_image','id',NULL,'','bigint','NO',NULL,NULL,1,NULL,NULL,NULL,NULL,1,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856912129335297,2038582379342303233,'note_image','note_id',NULL,'','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856912129335297,2038582379342303233,'note_image','image_url',NULL,'','varchar','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856912129335297,2038582379342303233,'note_image','ai_caption',NULL,'','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,4,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856912129335297,2038582379342303233,'note_image','source',NULL,'','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,5,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856912129335297,2038582379342303233,'note_image','created_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,6,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:41'),
	 ('000000',2038856912502628353,2038582379342303233,'note_link','id',NULL,'','bigint','NO',NULL,NULL,1,NULL,NULL,NULL,NULL,1,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:42'),
	 ('000000',2038856912502628353,2038582379342303233,'note_link','source_note_id',NULL,'源笔记（发出链接的笔记）','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:42'),
	 ('000000',2038856912502628353,2038582379342303233,'note_link','target_note_id',NULL,'目标笔记（被引用的笔记）','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:42'),
	 ('000000',2038856912502628353,2038582379342303233,'note_link','link_text',NULL,'链接显示文本（[[标题]] 中的标题）','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,4,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:42');
INSERT INTO ry_bigdata_v1.metadata_column (tenant_id,table_id,ds_id,table_name,column_name,column_alias,column_comment,data_type,is_nullable,column_key,default_value,is_primary_key,is_foreign_key,fk_reference,is_sensitive,sensitivity_level,sort_order,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000',2038856912502628353,2038582379342303233,'note_link','created_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,5,'0',103,'1','2026-03-31 22:12:04','1','2026-03-31 22:12:42'),
	 ('000000',2038856912888504322,2038582379342303233,'note_tag','id',NULL,'','bigint','NO',NULL,NULL,1,NULL,NULL,NULL,NULL,1,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:42'),
	 ('000000',2038856912888504322,2038582379342303233,'note_tag','note_id',NULL,'','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:42'),
	 ('000000',2038856912888504322,2038582379342303233,'note_tag','tag_id',NULL,'','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:42'),
	 ('000000',2038856913240825858,2038582379342303233,'tag','id',NULL,'','bigint','NO',NULL,NULL,1,NULL,NULL,NULL,NULL,1,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:42'),
	 ('000000',2038856913240825858,2038582379342303233,'tag','user_id',NULL,'','bigint','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:42'),
	 ('000000',2038856913240825858,2038582379342303233,'tag','name',NULL,'','varchar','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:43'),
	 ('000000',2038856913240825858,2038582379342303233,'tag','color',NULL,'','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,4,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:43'),
	 ('000000',2038856913240825858,2038582379342303233,'tag','created_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,5,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:43'),
	 ('000000',2038856913555398657,2038582379342303233,'user','id',NULL,'','bigint','NO',NULL,NULL,1,NULL,NULL,NULL,NULL,1,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:43');
INSERT INTO ry_bigdata_v1.metadata_column (tenant_id,table_id,ds_id,table_name,column_name,column_alias,column_comment,data_type,is_nullable,column_key,default_value,is_primary_key,is_foreign_key,fk_reference,is_sensitive,sensitivity_level,sort_order,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000',2038856913555398657,2038582379342303233,'user','username',NULL,'','varchar','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,2,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:43'),
	 ('000000',2038856913555398657,2038582379342303233,'user','email',NULL,'','varchar','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,3,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:43'),
	 ('000000',2038856913555398657,2038582379342303233,'user','password_hash',NULL,'','varchar','NO',NULL,NULL,0,NULL,NULL,NULL,NULL,4,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:43'),
	 ('000000',2038856913555398657,2038582379342303233,'user','avatar_url',NULL,'','varchar','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,5,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:43'),
	 ('000000',2038856913555398657,2038582379342303233,'user','settings',NULL,'用户设置 JSON','json','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,6,'0',103,'1','2026-03-31 22:12:05','1','2026-03-31 22:12:43'),
	 ('000000',2038856913555398657,2038582379342303233,'user','created_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,7,'0',103,'1','2026-03-31 22:12:06','1','2026-03-31 22:12:43'),
	 ('000000',2038856913555398657,2038582379342303233,'user','updated_at',NULL,'','datetime','YES',NULL,NULL,0,NULL,NULL,NULL,NULL,8,'0',103,'1','2026-03-31 22:12:06','1','2026-03-31 22:12:43');
INSERT INTO ry_bigdata_v1.metadata_scan_log (tenant_id,ds_id,ds_name,ds_code,scan_type,status,total_tables,success_count,partial_count,failed_count,error_detail,start_time,end_time,elapsed_ms,scan_user_id,remark,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','FULL','FAILED',10,0,0,10,'category: 入库失败; chat_message: 入库失败; chat_session: 入库失败; merge_suggestion: 入库失败; note: 入库失败; note_image: 入库失败; note_link: 入库失败; note_tag: 入库失败; tag: 入库失败; user: 入库失败','2026-03-31 13:52:22','2026-03-31 13:52:23',970,NULL,NULL,103,'1','2026-03-31 13:52:22','1','2026-03-31 13:52:23'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','FULL','FAILED',10,0,0,10,'category: 
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-category'' for key ''metadata_table.uk_ds_table''
### The error may exist in org/dromara/metadata/mapper/MetadataTableMapper.java (best guess)
### The error may involve org.dromara.metadata.mapper.MetadataTableMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO metadata_table (id, ds_id, ds_name, ds_code, table_name, table_comment, table_type, data_layer, row_count, last_scan_time, status, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-category'' for key ''metadata_table.uk_ds_table''
; Duplicate entry ''2038582379342303233-category'' for key ''metadata_table.uk_ds_table''; chat_message: 
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-chat_message'' for key ''metadata_table.uk_ds_table''
### The error may exist in org/dromara/metadata/mapper/MetadataTableMapper.java (best guess)
### The error may involve org.dromara.metadata.mapper.MetadataTableMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO metadata_table (id, ds_id, ds_name, ds_code, table_name, table_comment, table_type, data_layer, row_count, last_scan_time, status, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-chat_message'' for key ''metadata_table.uk_ds_table''
; Duplicate entry ''2038582379342303233-chat_message'' for key ''metadata_table.uk_ds_table''; chat_session: 
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-chat_session'' for key ''metadata_table.uk_ds_table''
### The error may exist in org/dromara/metadata/mapper/MetadataTableMapper.java (best guess)
### The error may involve org.dromara.metadata.mapper.MetadataTableMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO metadata_table (id, ds_id, ds_name, ds_code, table_name, table_comment, table_type, data_layer, row_count, last_scan_time, status, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-chat_session'' for key ''metadata_table.uk_ds_table''
; Duplicate entry ''2038582379342303233-chat_session'' for key ''metadata_table.uk_ds_table''; merge_suggestion: 
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-merge_suggestion'' for key ''metadata_table.uk_ds_table''
### The error may exist in org/dromara/metadata/mapper/MetadataTableMapper.java (best guess)
### The error may involve org.dromara.metadata.mapper.MetadataTableMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO metadata_table (id, ds_id, ds_name, ds_code, table_name, table_comment, table_type, data_layer, row_count, last_scan_time, status, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-merge_suggestion'' for key ''metadata_table.uk_ds_table''
; Duplicate entry ''2038582379342303233-merge_suggestion'' for key ''metadata_table.uk_ds_table''; note: 
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-note'' for key ''metadata_table.uk_ds_table''
### The error may exist in org/dromara/metadata/mapper/MetadataTableMapper.java (best guess)
### The error may involve org.dromara.metadata.mapper.MetadataTableMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO metadata_table (id, ds_id, ds_name, ds_code, table_name, table_comment, table_type, data_layer, row_count, last_scan_time, status, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-note'' for key ''metadata_table.uk_ds_table''
; Duplicate entry ''2038582379342303233-note'' for key ''metadata_table.uk_ds_table''; note_image: 
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-note_image'' for key ''metadata_table.uk_ds_table''
### The error may exist in org/dromara/metadata/mapper/MetadataTableMapper.java (best guess)
### The error may involve org.dromara.metadata.mapper.MetadataTableMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO metadata_table (id, ds_id, ds_name, ds_code, table_name, table_comment, table_type, data_layer, row_count, last_scan_time, status, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-note_image'' for key ''metadata_table.uk_ds_table''
; Duplicate entry ''2038582379342303233-note_image'' for key ''metadata_table.uk_ds_table''; note_link: 
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-note_link'' for key ''metadata_table.uk_ds_table''
### The error may exist in org/dromara/metadata/mapper/MetadataTableMapper.java (best guess)
### The error may involve org.dromara.metadata.mapper.MetadataTableMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO metadata_table (id, ds_id, ds_name, ds_code, table_name, table_comment, table_type, data_layer, row_count, last_scan_time, status, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-note_link'' for key ''metadata_table.uk_ds_table''
; Duplicate entry ''2038582379342303233-note_link'' for key ''metadata_table.uk_ds_table''; note_tag: 
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-note_tag'' for key ''metadata_table.uk_ds_table''
### The error may exist in org/dromara/metadata/mapper/MetadataTableMapper.java (best guess)
### The error may involve org.dromara.metadata.mapper.MetadataTableMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO metadata_table (id, ds_id, ds_name, ds_code, table_name, table_comment, table_type, data_layer, row_count, last_scan_time, status, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-note_tag'' for key ''metadata_table.uk_ds_table''
; Duplicate entry ''2038582379342303233-note_tag'' for key ''metadata_table.uk_ds_table''; tag: 
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-tag'' for key ''metadata_table.uk_ds_table''
### The error may exist in org/dromara/metadata/mapper/MetadataTableMapper.java (best guess)
### The error may involve org.dromara.metadata.mapper.MetadataTableMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO metadata_table (id, ds_id, ds_name, ds_code, table_name, table_comment, table_type, data_layer, row_count, last_scan_time, status, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-tag'' for key ''metadata_table.uk_ds_table''
; Duplicate entry ''2038582379342303233-tag'' for key ''metadata_table.uk_ds_table''; user: 
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-user'' for key ''metadata_table.uk_ds_table''
### The error may exist in org/dromara/metadata/mapper/MetadataTableMapper.java (best guess)
### The error may involve org.dromara.metadata.mapper.MetadataTableMapper.insert-Inline
### The error occurred while setting parameters
### SQL: INSERT INTO metadata_table (id, ds_id, ds_name, ds_code, table_name, table_comment, table_type, data_layer, row_count, last_scan_time, status, tenant_id, create_dept, create_by, create_time, update_by, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
### Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ''2038582379342303233-user'' for key ''metadata_table.uk_ds_table''
; Duplicate entry ''2038582379342303233-user'' for key ''metadata_table.uk_ds_table''','2026-03-31 13:56:54','2026-03-31 13:56:54',757,NULL,NULL,103,'1','2026-03-31 13:56:54','1','2026-03-31 13:56:54'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','TABLES','RUNNING',0,0,0,0,NULL,'2026-03-31 16:44:25',NULL,NULL,1,NULL,103,'1','2026-03-31 16:44:25','1','2026-03-31 16:44:25'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','ALL','RUNNING',0,0,0,0,NULL,'2026-03-31 16:46:34',NULL,NULL,1,NULL,103,'1','2026-03-31 16:46:34','1','2026-03-31 16:46:34'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','TABLES','FAILED',1,0,1,0,'user: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]','2026-03-31 19:55:26','2026-03-31 19:55:27',206,1,NULL,103,'1','2026-03-31 19:55:26','1','2026-03-31 19:55:27'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','TABLES','FAILED',1,0,1,0,'category: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]','2026-03-31 19:55:37','2026-03-31 19:55:37',157,1,NULL,103,'1','2026-03-31 19:55:37','1','2026-03-31 19:55:37'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','ALL','FAILED',10,0,10,0,'category: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]; chat_message: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]; chat_session: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]; merge_suggestion: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]; note: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]; note_image: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]; note_link: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]; note_tag: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]; tag: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]; user: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]','2026-03-31 19:56:19','2026-03-31 19:56:20',1024,1,NULL,103,'1','2026-03-31 19:56:19','1','2026-03-31 19:56:20'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','TABLES','FAILED',1,0,1,0,'user: PreparedStatementCallback; bad SQL grammar [SELECT
    COLUMN_NAME as columnName,
    DATA_TYPE as dataType,
    COLUMN_COMMENT as columnComment,
    IS_NULLABLE as nullable,
    COLUMN_KEY as columnKey
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
ORDER BY ORDINAL_POSITION
]','2026-03-31 20:18:08','2026-03-31 20:18:08',189,1,NULL,103,'1','2026-03-31 20:18:08','1','2026-03-31 20:18:08'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','TABLES','SUCCESS',1,1,0,0,NULL,'2026-03-31 22:10:59','2026-03-31 22:11:00',620,1,NULL,103,'1','2026-03-31 22:10:59','1','2026-03-31 22:11:00'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','TABLES','SUCCESS',1,1,0,0,NULL,'2026-03-31 22:11:18','2026-03-31 22:11:19',676,1,NULL,103,'1','2026-03-31 22:11:18','1','2026-03-31 22:11:19');
INSERT INTO ry_bigdata_v1.metadata_scan_log (tenant_id,ds_id,ds_name,ds_code,scan_type,status,total_tables,success_count,partial_count,failed_count,error_detail,start_time,end_time,elapsed_ms,scan_user_id,remark,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','ALL','SUCCESS',10,10,0,0,NULL,'2026-03-31 22:12:02','2026-03-31 22:12:06',4032,1,NULL,103,'1','2026-03-31 22:12:02','1','2026-03-31 22:12:06'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','ALL','SUCCESS',10,10,0,0,NULL,'2026-03-31 22:12:38','2026-03-31 22:12:43',4746,1,NULL,103,'1','2026-03-31 22:12:38','1','2026-03-31 22:12:43');
INSERT INTO ry_bigdata_v1.metadata_table (tenant_id,ds_id,ds_name,ds_code,table_name,table_alias,table_comment,table_type,data_layer,data_domain,row_count,storage_bytes,source_update_time,sensitivity_level,owner_id,dept_id,catalog_id,tags,last_scan_time,status,del_flag,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','category','333','','TABLE','ODS','11',1,NULL,'2026-03-20 15:56:32','SENSITIVE',NULL,NULL,NULL,'','2026-03-31 22:12:39','ACTIVE','0',103,'1','2026-03-31 05:52:22','1','2026-03-31 22:12:39'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','chat_message',NULL,'','TABLE','ODS',NULL,15,NULL,'2026-03-20 15:47:28','NORMAL',NULL,NULL,NULL,NULL,'2026-03-31 22:12:39','ACTIVE','0',103,'1','2026-03-31 13:52:22','1','2026-03-31 22:12:39'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','chat_session',NULL,'','TABLE','ODS',NULL,5,NULL,'2026-03-20 15:47:28','NORMAL',NULL,NULL,NULL,NULL,'2026-03-31 22:12:40','ACTIVE','0',103,'1','2026-03-31 13:52:22','1','2026-03-31 22:12:40'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','merge_suggestion',NULL,'','TABLE','ODS',NULL,0,NULL,'2026-03-20 15:47:28','NORMAL',NULL,NULL,NULL,NULL,'2026-03-31 22:12:40','ACTIVE','0',103,'1','2026-03-31 13:52:22','1','2026-03-31 22:12:40'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','note',NULL,'','TABLE','ODS',NULL,5,NULL,'2026-03-20 15:47:28','NORMAL',NULL,NULL,NULL,NULL,'2026-03-31 22:12:40','ACTIVE','0',103,'1','2026-03-31 13:52:23','1','2026-03-31 22:12:40'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','note_image',NULL,'','TABLE','ODS',NULL,0,NULL,'2026-03-20 15:47:28','NORMAL',NULL,NULL,NULL,NULL,'2026-03-31 22:12:41','ACTIVE','0',103,'1','2026-03-31 13:52:23','1','2026-03-31 22:12:41'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','note_link',NULL,'','TABLE','ODS',NULL,0,NULL,'2026-03-20 20:32:32','NORMAL',NULL,NULL,NULL,NULL,'2026-03-31 22:12:42','ACTIVE','0',103,'1','2026-03-31 13:52:23','1','2026-03-31 22:12:42'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','note_tag',NULL,'','TABLE','ODS',NULL,20,NULL,'2026-03-20 15:47:28','NORMAL',NULL,NULL,NULL,NULL,'2026-03-31 22:12:42','ACTIVE','0',103,'1','2026-03-31 13:52:23','1','2026-03-31 22:12:42'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','tag',NULL,'','TABLE','ODS',NULL,5,NULL,'2026-03-20 15:47:28','NORMAL',NULL,NULL,NULL,NULL,'2026-03-31 22:12:42','ACTIVE','0',103,'1','2026-03-31 13:52:23','1','2026-03-31 22:12:42'),
	 ('000000',2038582379342303233,'Mysql_ods','Mysql_ods','user',NULL,'','TABLE','ODS','11',2,NULL,'2026-03-20 15:47:27','NORMAL',NULL,NULL,NULL,'','2026-03-31 22:12:43','ACTIVE','0',103,'1','2026-03-31 05:52:23','1','2026-03-31 22:12:43');
INSERT INTO ry_bigdata_v1.sys_datasource (tenant_id,ds_name,ds_code,ds_type,host,port,database_name,schema_name,username,password,connection_params,data_layer,data_source,ds_flag,dept_id,status,del_flag,remark,create_dept,create_by,create_time,update_by,update_time) VALUES
	 ('000000','Mysql_ods','Mysql_ods','MYSQL','49.232.153.150',3366,'biji_db',NULL,'root','sdfwer@re3f',NULL,'ODS',NULL,'0',NULL,'0','0','',103,1,'2026-03-30 19:41:29',1,'2026-03-30 22:29:01');
