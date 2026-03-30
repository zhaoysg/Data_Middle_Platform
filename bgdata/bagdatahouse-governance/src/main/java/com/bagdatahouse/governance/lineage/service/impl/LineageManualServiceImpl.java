package com.bagdatahouse.governance.lineage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.core.entity.GovLineage;
import com.bagdatahouse.core.entity.GovMetadata;
import com.bagdatahouse.core.entity.GovMetadataColumn;
import com.bagdatahouse.core.entity.SecColumnSensitivity;
import com.bagdatahouse.core.entity.SecLevel;
import com.bagdatahouse.core.mapper.GovLineageMapper;
import com.bagdatahouse.core.mapper.GovMetadataColumnMapper;
import com.bagdatahouse.core.mapper.GovMetadataMapper;
import com.bagdatahouse.core.mapper.SecColumnSensitivityMapper;
import com.bagdatahouse.core.mapper.SecLevelMapper;
import com.bagdatahouse.governance.lineage.dto.LineageBatchImportDTO;
import com.bagdatahouse.governance.security.enums.SensitivityLevelEnum;
import com.bagdatahouse.governance.lineage.dto.LineageSaveDTO;
import com.bagdatahouse.governance.lineage.impact.ImpactAnalyzer;
import com.bagdatahouse.governance.lineage.service.LineageManualService;
import com.bagdatahouse.governance.lineage.service.LineageService;
import com.bagdatahouse.governance.lineage.vo.BatchImportResultVO;
import com.bagdatahouse.governance.lineage.vo.ImpactAnalysisResultVO;
import com.bagdatahouse.governance.lineage.vo.TableColumnSuggestVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 手动录入血缘增强服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineageManualServiceImpl implements LineageManualService {

    private final ImpactAnalyzer impactAnalyzer;
    private final LineageService lineageService;
    private final GovLineageMapper lineageMapper;
    private final GovMetadataMapper metadataMapper;
    private final GovMetadataColumnMapper columnMapper;
    private final SecColumnSensitivityMapper secColumnSensitivityMapper;
    private final SecLevelMapper secLevelMapper;

    @Override
    public Result<ImpactAnalysisResultVO> analyzeDownstream(Long dsId, String tableName,
                                                            String column, Integer maxDepth) {
        ImpactAnalysisResultVO result = impactAnalyzer.analyzeDownstream(dsId, tableName, column, maxDepth);
        return Result.success(result);
    }

    @Override
    public Result<ImpactAnalysisResultVO> analyzeUpstream(Long dsId, String tableName,
                                                           String column, Integer maxDepth) {
        ImpactAnalysisResultVO result = impactAnalyzer.analyzeUpstream(dsId, tableName, column, maxDepth);
        return Result.success(result);
    }

    @Override
    public Result<ImpactAnalysisResultVO> analyzeFromNode(String direction, Long dsId,
                                                           String tableName, String column,
                                                           Integer maxDepth) {
        ImpactAnalysisResultVO result = impactAnalyzer.analyzeFromNode(
                direction, dsId, tableName, column, maxDepth);
        return Result.success(result);
    }

    @Override
    public Result<TableColumnSuggestVO> suggestTablesAndColumns(Long dsId, String keyword) {
        if (dsId == null) {
            throw new BusinessException(2001, "数据源ID不能为空");
        }

        LambdaQueryWrapper<GovMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovMetadata::getDsId, dsId)
                .eq(GovMetadata::getStatus, "ACTIVE");

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(GovMetadata::getTableName, keyword)
                    .or()
                    .like(GovMetadata::getTableAlias, keyword)
                    .or()
                    .like(GovMetadata::getTableComment, keyword)
            );
        }

        wrapper.last("LIMIT 50");
        List<GovMetadata> metadataList = metadataMapper.selectList(wrapper);

        List<TableColumnSuggestVO.TableSuggest> tables = new ArrayList<>();
        for (GovMetadata meta : metadataList) {
            List<TableColumnSuggestVO.ColumnSuggest> columns = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                LambdaQueryWrapper<GovMetadataColumn> colWrapper = new LambdaQueryWrapper<>();
                colWrapper.eq(GovMetadataColumn::getMetadataId, meta.getId())
                        .and(w -> w
                                .like(GovMetadataColumn::getColumnName, keyword)
                                .or()
                                .like(GovMetadataColumn::getColumnAlias, keyword)
                        )
                        .last("LIMIT 20");
                List<GovMetadataColumn> colList = columnMapper.selectList(colWrapper);
                columns = colList.stream()
                        .map(c -> TableColumnSuggestVO.ColumnSuggest.builder()
                                .columnId(c.getId())
                                .columnName(c.getColumnName())
                                .columnAlias(c.getColumnAlias())
                                .dataType(c.getDataType())
                                .isPrimaryKey(Boolean.TRUE.equals(c.getIsPrimaryKey()))
                                .isForeignKey(Boolean.TRUE.equals(c.getIsForeignKey()))
                                .isSensitive(Boolean.TRUE.equals(c.getIsSensitive()))
                                .build())
                        .collect(Collectors.toList());
            }

            tables.add(TableColumnSuggestVO.TableSuggest.builder()
                    .metadataId(meta.getId())
                    .tableName(meta.getTableName())
                    .tableAlias(meta.getTableAlias())
                    .dataLayer(meta.getDataLayer())
                    .columnCount(null)
                    .columns(columns.isEmpty() ? null : columns)
                    .build());
        }

        TableColumnSuggestVO result = TableColumnSuggestVO.builder()
                .dsId(dsId)
                .tables(tables)
                .build();

        return Result.success(result);
    }

    @Override
    public Result<List<TableColumnSuggestVO.TableSuggest>> listTables(Long dsId) {
        if (dsId == null) {
            throw new BusinessException(2001, "数据源ID不能为空");
        }

        LambdaQueryWrapper<GovMetadata> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovMetadata::getDsId, dsId)
                .eq(GovMetadata::getStatus, "ACTIVE")
                .orderByAsc(GovMetadata::getTableName)
                .last("LIMIT 200");

        List<GovMetadata> metadataList = metadataMapper.selectList(wrapper);

        List<TableColumnSuggestVO.TableSuggest> tables = metadataList.stream()
                .map(meta -> {
                    LambdaQueryWrapper<GovMetadataColumn> colWrapper = new LambdaQueryWrapper<>();
                    colWrapper.eq(GovMetadataColumn::getMetadataId, meta.getId());
                    long colCount = columnMapper.selectCount(colWrapper);

                    return TableColumnSuggestVO.TableSuggest.builder()
                            .metadataId(meta.getId())
                            .tableName(meta.getTableName())
                            .tableAlias(meta.getTableAlias())
                            .dataLayer(meta.getDataLayer())
                            .columnCount((int) colCount)
                            .build();
                })
                .collect(Collectors.toList());

        return Result.success(tables);
    }

    @Override
    public Result<List<TableColumnSuggestVO.ColumnSuggest>> listColumns(Long dsId, String tableName) {
        if (dsId == null || !StringUtils.hasText(tableName)) {
            return Result.success(List.of());
        }

        LambdaQueryWrapper<GovMetadata> metaWrapper = new LambdaQueryWrapper<>();
        metaWrapper.eq(GovMetadata::getDsId, dsId)
                .eq(GovMetadata::getTableName, tableName)
                .eq(GovMetadata::getStatus, "ACTIVE")
                .last("LIMIT 1");
        GovMetadata meta = metadataMapper.selectOne(metaWrapper);

        if (meta == null) {
            return Result.success(List.of());
        }

        LambdaQueryWrapper<GovMetadataColumn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GovMetadataColumn::getMetadataId, meta.getId())
                .orderByAsc(GovMetadataColumn::getSortOrder)
                .orderByAsc(GovMetadataColumn::getColumnName);

        List<GovMetadataColumn> colList = columnMapper.selectList(wrapper);
        List<TableColumnSuggestVO.ColumnSuggest> columns = colList.stream()
                .map(c -> TableColumnSuggestVO.ColumnSuggest.builder()
                        .columnId(c.getId())
                        .columnName(c.getColumnName())
                        .columnAlias(c.getColumnAlias())
                        .dataType(c.getDataType())
                        .isPrimaryKey(Boolean.TRUE.equals(c.getIsPrimaryKey()))
                        .isForeignKey(Boolean.TRUE.equals(c.getIsForeignKey()))
                        .isSensitive(Boolean.TRUE.equals(c.getIsSensitive()))
                        .build())
                .collect(Collectors.toList());

        return Result.success(columns);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BatchImportResultVO> batchImport(LineageBatchImportDTO dto) {
        if (dto == null || dto.getRecords() == null || dto.getRecords().isEmpty()) {
            throw new BusinessException(2001, "导入记录不能为空");
        }

        List<BatchImportResultVO.ImportErrorDetail> errors = new ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;
        int rowNum = 1;

        for (LineageBatchImportDTO.LineageRecord record : dto.getRecords()) {
            rowNum++;
            try {
                validateRecord(record, dto);

                Long sourceDsId = record.getSourceDsId() != null
                        ? record.getSourceDsId() : dto.getDefaultSourceDsId();
                Long targetDsId = record.getTargetDsId() != null
                        ? record.getTargetDsId() : dto.getDefaultTargetDsId();

                if (sourceDsId == null) {
                    errors.add(BatchImportResultVO.ImportErrorDetail.builder()
                            .rowNum(rowNum)
                            .message("源数据源ID不能为空（请在全局配置或单条记录中指定）")
                            .build());
                    continue;
                }
                if (targetDsId == null) {
                    errors.add(BatchImportResultVO.ImportErrorDetail.builder()
                            .rowNum(rowNum)
                            .message("目标数据源ID不能为空（请在全局配置或单条记录中指定）")
                            .build());
                    continue;
                }

                LineageSaveDTO saveDTO = LineageSaveDTO.builder()
                        .lineageType(dto.getLineageType())
                        .lineageSource(dto.getLineageSource() != null ? dto.getLineageSource() : "MANUAL")
                        .sourceDsId(sourceDsId)
                        .sourceTable(record.getSourceTable())
                        .sourceColumn(record.getSourceColumn())
                        .sourceColumnAlias(record.getSourceColumnAlias())
                        .targetDsId(targetDsId)
                        .targetTable(record.getTargetTable())
                        .targetColumn(record.getTargetColumn())
                        .targetColumnAlias(record.getTargetColumnAlias())
                        .transformType(record.getTransformType())
                        .transformExpr(record.getTransformExpr())
                        .jobName(record.getJobName())
                        .deptId(dto.getDeptId())
                        .build();

                lineageService.save(saveDTO);
                successCount++;

            } catch (BusinessException be) {
                errors.add(BatchImportResultVO.ImportErrorDetail.builder()
                        .rowNum(rowNum)
                        .message(be.getMessage())
                        .build());
            } catch (Exception e) {
                log.error("批量导入第{}行失败: {}", rowNum, e.getMessage(), e);
                errors.add(BatchImportResultVO.ImportErrorDetail.builder()
                        .rowNum(rowNum)
                        .message("系统错误: " + e.getMessage())
                        .build());
            }
        }

        BatchImportResultVO result = BatchImportResultVO.builder()
                .totalRows(dto.getRecords().size())
                .successCount(successCount)
                .failCount((int) errors.stream()
                        .filter(e -> !e.getMessage().contains("已存在"))
                        .count())
                .skippedCount(skippedCount)
                .errors(errors)
                .build();

        return Result.success(result);
    }

    @Override
    public Result<String> getImportTemplate() {
        String template = """
            血缘导入模板说明：
            血缘类型: TABLE（表级血缘）或 COLUMN（字段级血缘）
            血缘来源: MANUAL（手动录入）或 AUTO_PARSER（自动解析）

            表级血缘示例（每行一条血缘记录）：
            源数据源ID,源表名,源字段名,源字段中文名,目标数据源ID,目标表名,目标字段名,目标字段中文名,转换类型,转换表达式,来源作业名称

            示例数据：
            1,ods_orders,,,2,dim_orders,,,,DIRECT,,order_etl
            1,ods_orders,,,2,dwd_orders,,,,SUM,,order_etl
            1,dwd_orders,,,2,dws_daily_sales,,,COUNT,,sales_agg

            字段级血缘示例：
            1,ods_orders,order_id,,2,dim_orders,id,,DIRECT,,order_dim_job
            1,ods_orders,amount,,2,dwd_orders,sale_amt,,SUM,amount求和后写入,sale_job
            1,dwd_orders,sale_amt,,2,dws_daily_sales,total_amount,,SUM,CASE WHEN聚合,sales_agg

            转换类型可选值：
            DIRECT（直接传递）、SUM（求和）、AVG（求平均）、COUNT（计数）、
            MAX（最大值）、MIN（最小值）、CONCAT（拼接）、CASE_WHEN（条件转换）、CUSTOM_EXPR（自定义表达式）

            说明：
            1. 源数据源ID和目标数据源ID也可以在全局配置中统一指定，单条记录中可留空
            2. 字段级血缘时，源字段名和目标字段名必填
            3. 转换类型和转换表达式为选填
            """;
        return Result.success(template);
    }

    private void validateRecord(LineageBatchImportDTO.LineageRecord record,
                                LineageBatchImportDTO dto) {
        if (!StringUtils.hasText(record.getSourceTable())) {
            throw new BusinessException(2001, "源表名不能为空");
        }
        if (!StringUtils.hasText(record.getTargetTable())) {
            throw new BusinessException(2001, "目标表名不能为空");
        }
        if ("COLUMN".equals(dto.getLineageType())) {
            if (!StringUtils.hasText(record.getSourceColumn())) {
                throw new BusinessException(2001, "字段级血缘必须填写源字段名");
            }
            if (!StringUtils.hasText(record.getTargetColumn())) {
                throw new BusinessException(2001, "字段级血缘必须填写目标字段名");
            }
        }
    }

    /**
     * 阶段三-T2：敏感等级传播校验
     * <p>
     * 字段级血缘中，目标字段的敏感等级应 >= 源字段敏感等级。
     * 若目标等级 < 源等级，返回升级建议警告。
     * <p>
     * 等级优先级（数值越大越敏感）：NORMAL(0) < L1(1) < L2(2) < L3(3) < L4(4)
     */
    @Override
    public Result<String> checkSensitivityPropagation(Long sourceDsId, String sourceTable,
                                                   String sourceColumn, Long targetDsId,
                                                   String targetTable, String targetColumn) {
        if (sourceDsId == null || !StringUtils.hasText(sourceTable)
                || !StringUtils.hasText(sourceColumn)
                || targetDsId == null || !StringUtils.hasText(targetTable)
                || !StringUtils.hasText(targetColumn)) {
            return Result.success(null); // 参数不完整，跳过校验
        }

        // 获取源字段敏感等级
        String sourceLevel = resolveColumnSensitivityLevel(sourceDsId, sourceTable, sourceColumn);
        // 获取目标字段敏感等级
        String targetLevel = resolveColumnSensitivityLevel(targetDsId, targetTable, targetColumn);

        // 双方均无敏感等级，不触发校验
        if (!StringUtils.hasText(sourceLevel) && !StringUtils.hasText(targetLevel)) {
            return Result.success(null);
        }

        // 仅源有等级，目标无等级 → 发出升级建议
        if (StringUtils.hasText(sourceLevel) && !StringUtils.hasText(targetLevel)) {
            String warning = String.format(
                    "【敏感等级升级建议】目标字段 %s.%s 当前无敏感等级，但源字段 %s.%s 敏感等级为 %s。" +
                    "建议将目标字段敏感等级设置为 %s 或更高，以符合敏感数据传播规范。",
                    targetDsId, targetTable, targetColumn,
                    sourceDsId, sourceTable, sourceColumn,
                    sourceLevel, sourceLevel);
            log.warn(warning);
            return Result.success(warning);
        }

        // 双方均有等级，比较数值
        if (StringUtils.hasText(sourceLevel) && StringUtils.hasText(targetLevel)) {
            int sourceValue = getLevelValue(sourceLevel);
            int targetValue = getLevelValue(targetLevel);
            if (targetValue < sourceValue) {
                String sourceLabel = SensitivityLevelEnum.getLabelByCode(sourceLevel);
                String targetLabel = SensitivityLevelEnum.getLabelByCode(targetLevel);
                String warning = String.format(
                        "【敏感等级降级警告】目标字段 %s.%s 敏感等级为 %s（%s），" +
                        "低于源字段 %s.%s 的 %s（%s）。" +
                        "敏感数据从高等级传播至低等级存在安全风险，建议将目标字段等级提升至 %s 或以上。",
                        targetDsId, targetTable, targetColumn,
                        targetLevel, targetLabel,
                        sourceDsId, sourceTable, sourceColumn,
                        sourceLevel, sourceLabel,
                        sourceLevel);
                log.warn(warning);
                return Result.success(warning);
            }
        }

        // 目标等级 >= 源等级，符合规范
        return Result.success(null);
    }

    /**
     * 解析字段的敏感等级（L1/L2/L3/L4/NORMAL/null）
     */
    private String resolveColumnSensitivityLevel(Long dsId, String tableName, String columnName) {
        if (dsId == null || tableName == null || columnName == null) {
            return null;
        }

        // 优先取已 APPROVED 的敏感字段记录
        LambdaQueryWrapper<SecColumnSensitivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecColumnSensitivity::getDsId, dsId)
                .eq(SecColumnSensitivity::getTableName, tableName)
                .eq(SecColumnSensitivity::getColumnName, columnName)
                .eq(SecColumnSensitivity::getReviewStatus, "APPROVED")
                .last("LIMIT 1");
        SecColumnSensitivity record = secColumnSensitivityMapper.selectOne(wrapper);

        if (record != null && record.getLevelId() != null) {
            SecLevel level = secLevelMapper.selectById(record.getLevelId());
            if (level != null && level.getLevelCode() != null) {
                return level.getLevelCode();
            }
        }

        // 其次取字段的 gov_metadata_column.sensitivity_level
        if (record == null) {
            LambdaQueryWrapper<GovMetadata> metaWrapper = new LambdaQueryWrapper<>();
            metaWrapper.eq(GovMetadata::getDsId, dsId)
                    .eq(GovMetadata::getTableName, tableName)
                    .last("LIMIT 1");
            GovMetadata meta = metadataMapper.selectOne(metaWrapper);
            if (meta != null) {
                LambdaQueryWrapper<GovMetadataColumn> colWrapper = new LambdaQueryWrapper<>();
                colWrapper.eq(GovMetadataColumn::getMetadataId, meta.getId())
                        .eq(GovMetadataColumn::getColumnName, columnName)
                        .last("LIMIT 1");
                GovMetadataColumn col = columnMapper.selectOne(colWrapper);
                if (col != null && col.getSensitivityLevel() != null
                        && !"NORMAL".equalsIgnoreCase(col.getSensitivityLevel())) {
                    return col.getSensitivityLevel();
                }
            }
        }

        return null;
    }

    /**
     * 将等级码转换为数值（数值越大越敏感）
     */
    private int getLevelValue(String levelCode) {
        if (levelCode == null) return 0;
        return switch (levelCode.toUpperCase()) {
            case "NORMAL" -> 0;
            case "L1" -> 1;
            case "L2" -> 2;
            case "L3" -> 3;
            case "L4" -> 4;
            default -> 0;
        };
    }
}
