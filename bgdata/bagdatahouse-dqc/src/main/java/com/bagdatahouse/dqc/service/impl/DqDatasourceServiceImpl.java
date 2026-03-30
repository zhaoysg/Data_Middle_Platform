package com.bagdatahouse.dqc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.common.result.Result;
import com.bagdatahouse.common.util.JdbcExceptionMessages;
import com.bagdatahouse.core.dto.DqDatasourceDTO;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.core.mapper.DqDatasourceMapper;
import com.bagdatahouse.datasource.adapter.DataSourceAdapter;
import com.bagdatahouse.datasource.adapter.DataSourceAdapterRegistry;
import com.bagdatahouse.datasource.enums.DataSourceTypeEnum;
import com.bagdatahouse.datasource.manager.DynamicDataSourceManager;
import com.bagdatahouse.datasource.service.DataSourceConnectionService;
import com.bagdatahouse.datasource.vo.ConnectionTestResultVO;
import com.bagdatahouse.dqc.dto.PreviewSelectRequest;
import com.bagdatahouse.dqc.service.DqDatasourceService;
import com.bagdatahouse.dqc.util.ReadOnlySqlPreviewUtil;
import com.bagdatahouse.dqc.vo.DatasourceTableColumnVO;
import com.bagdatahouse.dqc.vo.SqlPreviewResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DQ数据源服务实现
 */
@Service
public class DqDatasourceServiceImpl extends ServiceImpl<DqDatasourceMapper, DqDatasource>
        implements DqDatasourceService {

    private static final Logger log = LoggerFactory.getLogger(DqDatasourceServiceImpl.class);

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Autowired
    private DataSourceAdapterRegistry adapterRegistry;

    @Autowired
    private DataSourceConnectionService connectionService;

    @Value("${bagdatahouse.dqc.sql-preview-query-timeout-seconds:15}")
    private int sqlPreviewQueryTimeoutSeconds;

    @Value("${bagdatahouse.dqc.sql-preview-max-rows:100}")
    private int sqlPreviewMaxRows;

    private DqDatasource getByIdInternal(Long id) {
        return baseMapper.selectById(id);
    }

    private String generateDsCode(String dsType, String dsName) {
        String prefix = dsType.toUpperCase();
        if (prefix.equals("SQLSERVER")) {
            prefix = "MSSQL";
        } else if (prefix.equals("POSTGRESQL")) {
            prefix = "PG";
        }
        // 使用 dsName 拼音首字母 + 时间戳后缀，保证重启后不重复
        String nameSuffix = dsName != null
                ? dsName.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "").substring(0, Math.min(8, dsName.length()))
                : "";
        long ts = System.currentTimeMillis() % 100000;
        return prefix + "_" + nameSuffix + "_" + ts;
    }

    @Override
    public Result<Page<DqDatasource>> page(Integer pageNum, Integer pageSize, String dsName,
                                            String dsType, String dataLayer, Integer status) {
        Page<DqDatasource> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DqDatasource> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(dsName)) {
            wrapper.like(DqDatasource::getDsName, dsName);
        }
        if (StringUtils.hasText(dsType)) {
            wrapper.eq(DqDatasource::getDsType, dsType);
        }
        if (StringUtils.hasText(dataLayer)) {
            wrapper.eq(DqDatasource::getDataLayer, dataLayer);
        }
        if (status != null) {
            wrapper.eq(DqDatasource::getStatus, status);
        }

        wrapper.orderByDesc(DqDatasource::getCreateTime);
        Page<DqDatasource> result = this.page(page, wrapper);
        return Result.success(result);
    }

    @Override
    public Result<DqDatasource> getById(Long id) {
        DqDatasource entity = getByIdInternal(id);
        if (entity == null) {
            throw new BusinessException(2001, "数据源不存在");
        }
        return Result.success(entity);
    }

    @Override
    public Result<DqDatasource> getByCode(String dsCode) {
        LambdaQueryWrapper<DqDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqDatasource::getDsCode, dsCode);
        DqDatasource entity = this.getOne(wrapper);
        if (entity == null) {
            throw new BusinessException(2001, "数据源不存在");
        }
        return Result.success(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> create(DqDatasourceDTO dto) {
        if (!StringUtils.hasText(dto.getDsType())) {
            throw new BusinessException(400, "数据源类型不能为空");
        }
        DataSourceTypeEnum typeEnum = DataSourceTypeEnum.fromCode(dto.getDsType());
        if (typeEnum == null) {
            throw new BusinessException(400, "不支持的数据源类型: " + dto.getDsType());
        }
        if (!StringUtils.hasText(dto.getHost())) {
            throw new BusinessException(400, "主机地址不能为空");
        }
        if (!StringUtils.hasText(dto.getDatabaseName())) {
            throw new BusinessException(400, "数据库名称不能为空");
        }
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }

        if (!StringUtils.hasText(dto.getDsCode())) {
            dto.setDsCode(generateDsCode(dto.getDsType(), dto.getDsName()));
        }

        LambdaQueryWrapper<DqDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqDatasource::getDsCode, dto.getDsCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(2003, "数据源编码已存在");
        }

        if (dto.getPort() == null || dto.getPort() <= 0) {
            dto.setPort(typeEnum.getDefaultPort());
        }

        DqDatasource entity = toEntity(dto);
        entity.setCreateUser(1L);
        entity.setCreateTime(LocalDateTime.now());
        entity.setStatus(1);
        this.save(entity);

        try {
            dynamicDataSourceManager.registerDataSource(entity.getId(), entity);
        } catch (Exception e) {
            log.error("注册数据源失败", e);
            this.removeById(entity.getId());
            throw new BusinessException(2002, "数据源连接失败: " + e.getMessage());
        }

        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> update(Long id, DqDatasourceDTO dto) {
        DqDatasource existing = getByIdInternal(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据源不存在");
        }

        if (StringUtils.hasText(dto.getDsCode()) && !dto.getDsCode().equals(existing.getDsCode())) {
            LambdaQueryWrapper<DqDatasource> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DqDatasource::getDsCode, dto.getDsCode());
            wrapper.ne(DqDatasource::getId, id);
            if (this.count(wrapper) > 0) {
                throw new BusinessException(2003, "数据源编码已存在");
            }
        }

        dynamicDataSourceManager.unregisterDataSource(id);

        DqDatasource entity = toEntity(dto);
        entity.setId(id);
        entity.setUpdateUser(1L);
        entity.setUpdateTime(LocalDateTime.now());
        entity.setCreateUser(existing.getCreateUser());
        entity.setCreateTime(existing.getCreateTime());
        this.updateById(entity);

        try {
            dynamicDataSourceManager.registerDataSource(id, entity);
        } catch (Exception e) {
            log.error("重新注册数据源失败", e);
            dynamicDataSourceManager.registerDataSource(id, existing);
            throw new BusinessException(2002, "数据源连接失败: " + e.getMessage());
        }

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> delete(Long id) {
        DqDatasource existing = getByIdInternal(id);
        if (existing == null) {
            throw new BusinessException(2001, "数据源不存在");
        }

        dynamicDataSourceManager.unregisterDataSource(id);

        if (!this.removeById(id)) {
            throw new BusinessException(2001, "数据源不存在");
        }
        return Result.success();
    }

    @Override
    public Result<Boolean> testConnection(DqDatasourceDTO dto) {
        try {
            ConnectionTestResultVO result = connectionService.testConnection(dto);
            return Result.success(result.getSuccess());
        } catch (Exception e) {
            log.error("测试连接失败: {}", e.getMessage());
            return Result.success(false);
        }
    }

    @Override
    public Result<ConnectionTestResultVO> testConnectionDetail(DqDatasourceDTO dto) {
        ConnectionTestResultVO result = connectionService.testConnection(dto);
        return Result.success(result);
    }

    @Override
    public Result<ConnectionTestResultVO> testConnectionById(Long id) {
        ConnectionTestResultVO result = connectionService.testConnectionById(id);
        return Result.success(result);
    }

    @Override
    public Result<Void> enable(Long id) {
        DqDatasource entity = getByIdInternal(id);
        if (entity == null) {
            throw new BusinessException(2001, "数据源不存在");
        }

        DqDatasource update = new DqDatasource();
        update.setId(id);
        update.setStatus(1);
        update.setUpdateTime(LocalDateTime.now());
        this.updateById(update);

        return Result.success();
    }

    @Override
    public Result<Void> disable(Long id) {
        DqDatasource entity = getByIdInternal(id);
        if (entity == null) {
            throw new BusinessException(2001, "数据源不存在");
        }

        DqDatasource update = new DqDatasource();
        update.setId(id);
        update.setStatus(0);
        update.setUpdateTime(LocalDateTime.now());
        this.updateById(update);

        return Result.success();
    }

    @Override
    public Result<List<DqDatasource>> listByType(String dsType) {
        LambdaQueryWrapper<DqDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqDatasource::getDsType, dsType);
        wrapper.eq(DqDatasource::getStatus, 1);
        wrapper.orderByAsc(DqDatasource::getDsName);
        List<DqDatasource> list = this.list(wrapper);
        return Result.success(list);
    }

    @Override
    public Result<List<DqDatasource>> listByLayer(String layerCode) {
        LambdaQueryWrapper<DqDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqDatasource::getDataLayer, layerCode);
        wrapper.eq(DqDatasource::getStatus, 1);
        wrapper.orderByAsc(DqDatasource::getDsName);
        List<DqDatasource> list = this.list(wrapper);
        return Result.success(list);
    }

    @Override
    public Result<List<DqDatasource>> listEnabled() {
        LambdaQueryWrapper<DqDatasource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DqDatasource::getStatus, 1);
        wrapper.orderByAsc(DqDatasource::getDsName);
        List<DqDatasource> list = this.list(wrapper);
        return Result.success(list);
    }

    @Override
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = new LinkedHashMap<>();

        long totalCount = this.count();
        LambdaQueryWrapper<DqDatasource> enabledWrapper = new LambdaQueryWrapper<>();
        enabledWrapper.eq(DqDatasource::getStatus, 1);
        long enabledCount = this.count(enabledWrapper);

        LambdaQueryWrapper<DqDatasource> allWrapper = new LambdaQueryWrapper<>();
        List<DqDatasource> allDatasources = this.list(allWrapper);

        Map<String, Long> countByType = allDatasources.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        d -> d.getDsType() != null ? d.getDsType() : "UNKNOWN",
                        java.util.stream.Collectors.counting()
                ));

        Map<String, Long> countByLayer = allDatasources.stream()
                .filter(d -> d.getDataLayer() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        DqDatasource::getDataLayer,
                        java.util.stream.Collectors.counting()
                ));

        statistics.put("totalCount", totalCount);
        statistics.put("enabledCount", enabledCount);
        statistics.put("countByType", countByType);
        statistics.put("countByLayer", countByLayer);

        return Result.success(statistics);
    }

    @Override
    public Result<List<String>> getTables(Long id, String schema) {
        DqDatasource datasource = getByIdInternal(id);
        if (datasource == null) {
            throw new BusinessException(2001, "数据源不存在");
        }
        if (datasource.getStatus() != 1) {
            throw new BusinessException(2001, "数据源未启用");
        }

        try {
            DataSourceAdapter adapter = adapterRegistry.getAdapterById(id);
            if (adapter == null) {
                throw new BusinessException(2001, "数据源适配器未找到，请检查数据源配置");
            }
            List<String> tables;
            if (StringUtils.hasText(schema)) {
                tables = adapter.getTables(schema.trim());
            } else {
                tables = adapter.getTables();
            }
            return Result.success(tables);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            String hint = JdbcExceptionMessages.connectionIssueHint(e);
            if (hint != null) {
                log.warn("获取表列表失败（数据源不可达）, dsId={}, schema={}", id, schema, e);
                throw new BusinessException(2002, hint);
            }
            log.error("获取表列表失败, dsId={}, schema={}", id, schema, e);
            throw new BusinessException(2002, "获取表列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<DatasourceTableColumnVO>> getTableColumns(Long id, String tableName, String schema) {
        if (!StringUtils.hasText(tableName)) {
            throw new BusinessException(400, "表名不能为空");
        }
        DqDatasource datasource = getByIdInternal(id);
        if (datasource == null) {
            throw new BusinessException(2001, "数据源不存在");
        }
        if (datasource.getStatus() != 1) {
            throw new BusinessException(2001, "数据源未启用");
        }
        try {
            DataSourceAdapter adapter = adapterRegistry.getAdapterById(id);
            if (adapter == null) {
                throw new BusinessException(2001, "数据源适配器未找到，请检查数据源配置");
            }
            List<DataSourceAdapter.ColumnInfo> cols;
            if (StringUtils.hasText(schema)) {
                cols = adapter.getColumns(schema.trim(), tableName.trim());
            } else {
                cols = adapter.getColumns(tableName.trim());
            }
            List<DatasourceTableColumnVO> list = cols.stream()
                    .map(c -> DatasourceTableColumnVO.builder()
                            .columnName(c.columnName())
                            .dataType(c.dataType())
                            .columnComment(c.columnComment())
                            .nullable(c.nullable())
                            .primaryKey(c.primaryKey())
                            .build())
                    .collect(Collectors.toList());
            return Result.success(list);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            String hint = JdbcExceptionMessages.connectionIssueHint(e);
            if (hint != null) {
                log.warn("获取表字段失败（数据源不可达）, dsId={}, table={}, schema={}", id, tableName, schema, e);
                throw new BusinessException(2002, hint);
            }
            log.error("获取表字段失败, dsId={}, table={}, schema={}", id, tableName, schema, e);
            throw new BusinessException(2002, "获取表字段失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<String>> getSchemas(Long id) {
        DqDatasource datasource = getByIdInternal(id);
        if (datasource == null) {
            throw new BusinessException(2001, "数据源不存在");
        }
        if (datasource.getStatus() != 1) {
            throw new BusinessException(2001, "数据源未启用");
        }
        try {
            DataSourceAdapter adapter = adapterRegistry.getAdapterById(id);
            if (adapter == null) {
                throw new BusinessException(2001, "数据源适配器未找到，请检查数据源配置");
            }
            List<String> schemas = adapter.getSchemas();
            return Result.success(schemas);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            String hint = JdbcExceptionMessages.connectionIssueHint(e);
            if (hint != null) {
                log.warn("获取 schema 列表失败（数据源不可达）, dsId={}", id, e);
                throw new BusinessException(2002, hint);
            }
            log.error("获取schema列表失败, dsId={}", id, e);
            throw new BusinessException(2002, "获取schema列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<SqlPreviewResultVO> previewSelect(Long id, PreviewSelectRequest request) {
        if (request == null || !StringUtils.hasText(request.getSql())) {
            throw new BusinessException(400, "SQL 不能为空");
        }
        DqDatasource datasource = getByIdInternal(id);
        if (datasource == null) {
            throw new BusinessException(2001, "数据源不存在");
        }
        if (datasource.getStatus() != 1) {
            throw new BusinessException(2001, "数据源未启用");
        }
        try {
            DataSourceAdapter adapter = adapterRegistry.getAdapterById(id);
            if (adapter == null) {
                throw new BusinessException(2001, "数据源适配器未找到，请检查数据源配置");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("预览查询前注册适配器失败 dsId={}", id, e);
            throw new BusinessException(2001, "数据源连接池注册失败: " + e.getMessage());
        }

        String raw = request.getSql();
        boolean wrapped = ReadOnlySqlPreviewUtil.wasWrappedInOuterSingleQuotes(raw);
        String stripped = ReadOnlySqlPreviewUtil.stripOuterSingleQuotes(raw);
        String sql = ReadOnlySqlPreviewUtil.validateAndNormalizeSelect(stripped);
        String normalizeNote = wrapped
                ? "已去掉首尾单层单引号后再执行；保存规则时请去掉外层引号，否则可能被当作字符串常量而非 SQL。"
                : null;

        JdbcTemplate base = dynamicDataSourceManager.getJdbcTemplateById(id);
        if (base == null || base.getDataSource() == null) {
            throw new BusinessException(2002, "数据源连接池不可用，请稍后重试或刷新数据源配置");
        }
        JdbcTemplate jt = new JdbcTemplate(Objects.requireNonNull(base.getDataSource()));
        jt.setQueryTimeout(Math.max(1, sqlPreviewQueryTimeoutSeconds));
        jt.setMaxRows(Math.max(1, sqlPreviewMaxRows));

        try {
            List<Map<String, Object>> rows = jt.queryForList(sql);
            boolean truncated = rows.size() >= sqlPreviewMaxRows;
            SqlPreviewResultVO vo = SqlPreviewResultVO.builder()
                    .sqlExecuted(sql)
                    .rows(rows)
                    .rowCount(rows.size())
                    .truncated(truncated)
                    .normalizeNote(normalizeNote)
                    .build();
            return Result.success(vo);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("预览查询执行失败 dsId={}", id, e);
            String hint = JdbcExceptionMessages.connectionIssueHint(e);
            if (hint != null) {
                throw new BusinessException(2002, hint);
            }
            throw new BusinessException(2002, "预览查询失败: " + e.getMessage());
        }
    }

    private DqDatasource toEntity(DqDatasourceDTO dto) {
        return DqDatasource.builder()
                .id(dto.getId())
                .dsName(dto.getDsName())
                .dsCode(dto.getDsCode())
                .dsType(dto.getDsType())
                .host(dto.getHost())
                .port(dto.getPort())
                .databaseName(dto.getDatabaseName())
                .username(dto.getUsername())
                .password(dto.getPassword())
                .connectionParams(dto.getConnectionParams())
                .dataLayer(dto.getDataLayer())
                .deptId(dto.getDeptId())
                .ownerId(dto.getOwnerId())
                .status(dto.getStatus())
                .lastTestTime(dto.getLastTestTime())
                .lastTestResult(dto.getLastTestResult())
                .remark(dto.getRemark())
                .build();
    }

    private DqDatasourceDTO toDTO(DqDatasource entity) {
        return DqDatasourceDTO.builder()
                .id(entity.getId())
                .dsName(entity.getDsName())
                .dsCode(entity.getDsCode())
                .dsType(entity.getDsType())
                .host(entity.getHost())
                .port(entity.getPort())
                .databaseName(entity.getDatabaseName())
                .username(entity.getUsername())
                .password(entity.getPassword())
                .connectionParams(entity.getConnectionParams())
                .dataLayer(entity.getDataLayer())
                .deptId(entity.getDeptId())
                .ownerId(entity.getOwnerId())
                .status(entity.getStatus())
                .lastTestTime(entity.getLastTestTime())
                .lastTestResult(entity.getLastTestResult())
                .remark(entity.getRemark())
                .createUser(entity.getCreateUser())
                .createTime(entity.getCreateTime())
                .updateUser(entity.getUpdateUser())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
