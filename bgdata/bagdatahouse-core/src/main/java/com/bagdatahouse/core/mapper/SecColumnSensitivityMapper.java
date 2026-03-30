package com.bagdatahouse.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bagdatahouse.core.entity.SecColumnSensitivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 字段敏感等级记录 Mapper 接口
 */
@Mapper
public interface SecColumnSensitivityMapper extends BaseMapper<SecColumnSensitivity> {

    /**
     * 按审核状态聚合统计（单条 SQL，替代多次 selectCount）
     */
    @Select("SELECT "
            + "COUNT(*) AS total, "
            + "SUM(CASE WHEN review_status = 'PENDING' THEN 1 ELSE 0 END) AS pending, "
            + "SUM(CASE WHEN review_status = 'APPROVED' THEN 1 ELSE 0 END) AS approved, "
            + "SUM(CASE WHEN review_status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected "
            + "FROM sec_column_sensitivity WHERE deleted = 0")
    Map<String, Object> selectReviewStatusAggregate();

    /**
     * 按 level_id 聚合（替代全表 selectList 再在内存分组）
     */
    @Select("SELECT level_id AS levelId, COUNT(*) AS cnt FROM sec_column_sensitivity "
            + "WHERE deleted = 0 AND level_id IS NOT NULL GROUP BY level_id")
    List<Map<String, Object>> countGroupByLevelId();

    /**
     * 按日 + 等级统计新增敏感字段（用于趋势图）
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS statDate, level_id AS levelId, COUNT(*) AS cnt "
            + "FROM sec_column_sensitivity WHERE deleted = 0 "
            + "AND create_time >= #{start} AND create_time <= #{end} "
            + "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d'), level_id")
    List<Map<String, Object>> countCreateByDayAndLevel(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS statDate, COUNT(*) AS cnt "
            + "FROM sec_column_sensitivity WHERE deleted = 0 "
            + "AND create_time >= #{start} AND create_time <= #{end} "
            + "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')")
    List<Map<String, Object>> countCreateByDay(@Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Select("SELECT DATE_FORMAT(approved_time, '%Y-%m-%d') AS statDate, COUNT(*) AS cnt "
            + "FROM sec_column_sensitivity WHERE deleted = 0 AND review_status = 'APPROVED' "
            + "AND approved_time >= #{start} AND approved_time <= #{end} "
            + "GROUP BY DATE_FORMAT(approved_time, '%Y-%m-%d')")
    List<Map<String, Object>> countApprovedByDay(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);
}
