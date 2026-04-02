package org.dromara.datasource.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.helper.DataPermissionHelper;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.datasource.adapter.DataSourceAdapterRegistry;
import org.dromara.datasource.connection.ConnectionTestResultVO;
import org.dromara.datasource.domain.SysDatasource;
import org.dromara.datasource.domain.bo.SysDatasourceBo;
import org.dromara.datasource.domain.vo.SysDatasourceVo;
import org.dromara.datasource.enums.DataSourceTypeEnum;
import org.dromara.datasource.manager.DynamicDataSourceManager;
import org.dromara.datasource.mapper.SysDatasourceMapper;
import org.dromara.datasource.service.ISysDatasourceService;
import org.dromara.datasource.support.DatasourceCryptoSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数据源管理服务实现
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DS("bigdata")
public class SysDatasourceServiceImpl implements ISysDatasourceService {

    private static final DateTimeFormatter DS_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SysDatasourceMapper baseMapper;
    private final DynamicDataSourceManager dataSourceManager;
    private final DataSourceAdapterRegistry adapterRegistry;
    private final DatasourceCryptoSupport cryptoSupport;

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public TableDataInfo<SysDatasourceVo> pageDatasourceList(SysDatasourceBo bo, PageQuery pageQuery) {
        Page<SysDatasourceVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        maskSensitiveFields(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public List<SysDatasourceVo> listDatasource(SysDatasourceBo bo) {
        List<SysDatasourceVo> list = baseMapper.selectVoList(buildQueryWrapper(bo));
        maskSensitiveFields(list);
        return list;
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public List<SysDatasourceVo> listDatasourceByIds(List<Long> dsIds) {
        if (dsIds == null || dsIds.isEmpty()) {
            return List.of();
        }
        List<SysDatasourceVo> list = baseMapper.selectVoList(
            Wrappers.<SysDatasource>lambdaQuery()
                .in(SysDatasource::getDsId, dsIds)
                .orderByAsc(SysDatasource::getDsId)
        );
        maskSensitiveFields(list);
        return list;
    }

    private Wrapper<SysDatasource> buildQueryWrapper(SysDatasourceBo bo) {
        LambdaQueryWrapper<SysDatasource> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ObjectUtil.isNotNull(bo.getDsId()), SysDatasource::getDsId, bo.getDsId())
            .like(StringUtils.isNotBlank(bo.getDsName()), SysDatasource::getDsName, bo.getDsName())
            .like(StringUtils.isNotBlank(bo.getDsCode()), SysDatasource::getDsCode, bo.getDsCode())
            .eq(StringUtils.isNotBlank(bo.getDsType()), SysDatasource::getDsType, bo.getDsType())
            .eq(StringUtils.isNotBlank(bo.getDataSource()), SysDatasource::getDataSource, bo.getDataSource())
            .eq(StringUtils.isNotBlank(bo.getDsFlag()), SysDatasource::getDsFlag, bo.getDsFlag())
            .eq(StringUtils.isNotBlank(bo.getStatus()), SysDatasource::getStatus, bo.getStatus())
            .eq(ObjectUtil.isNotNull(bo.getDeptId()), SysDatasource::getDeptId, bo.getDeptId())
            .orderByAsc(SysDatasource::getDsId);
        return wrapper;
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public SysDatasourceVo getDatasourceById(Long dsId) {
        SysDatasourceVo datasource = baseMapper.selectVoById(dsId);
        if (datasource == null) {
            throw new ServiceException("数据源不存在或无权限访问");
        }
        datasource.setPassword(cryptoSupport.decryptPassword(datasource.getPassword()));
        return datasource;
    }

    @Override
    public SysDatasourceVo getDatasourceByCode(String dsCode) {
        SysDatasource entity = baseMapper.selectByDsCode(dsCode);
        if (entity == null) {
            return null;
        }
        return MapstructUtils.convert(entity, SysDatasourceVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertDatasource(SysDatasourceBo bo) {
        ensureDsCodeForInsert(bo);
        encryptSensitiveFields(bo);
        SysDatasource ds = MapstructUtils.convert(bo, SysDatasource.class);
        baseMapper.insert(ds);
        return ds.getDsId();
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    @Transactional(rollbackFor = Exception.class)
    public int updateDatasource(SysDatasourceBo bo) {
        if (ObjectUtil.isNull(bo.getDsId())) {
            throw new ServiceException("数据源ID不能为空");
        }
        if (StringUtils.isBlank(bo.getDsCode())) {
            throw new ServiceException("数据源编码不能为空");
        }
        if (!DataPermissionHelper.ignore(() -> checkDsCodeUnique(bo))) {
            throw new ServiceException("数据源编码已存在");
        }
        getDatasourceOrThrow(bo.getDsId(), false);
        // 如果数据源已注册，需要先注销旧的连接池
        if (dataSourceManager.isRegistered(bo.getDsId())) {
            adapterRegistry.unregisterAdapter(bo.getDsId());
        }
        encryptSensitiveFields(bo);
        SysDatasource ds = MapstructUtils.convert(bo, SysDatasource.class);
        return baseMapper.updateById(ds);
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    @Transactional(rollbackFor = Exception.class)
    public int deleteDatasource(Long[] dsIds) {
        for (Long dsId : dsIds) {
            getDatasourceOrThrow(dsId, false);
            // 删除前先注销连接池
            adapterRegistry.unregisterAdapter(dsId);
        }
        return baseMapper.deleteByIds(List.of(dsIds));
    }

    @Override
    public ConnectionTestResultVO testConnection(SysDatasourceBo bo) {
        long start = System.currentTimeMillis();
        try {
            prepareTestConnectionBo(bo);
            if (StringUtils.isBlank(bo.getDsType())) {
                return ConnectionTestResultVO.builder()
                    .success(false)
                    .message("数据源类型不能为空，请先完善数据源信息")
                    .elapsedMs(System.currentTimeMillis() - start)
                    .testTime(LocalDateTime.now())
                    .build();
            }
            DataSourceTypeEnum typeEnum = DataSourceTypeEnum.fromCode(bo.getDsType());
            if (typeEnum == null) {
                return ConnectionTestResultVO.builder()
                    .success(false)
                    .message("不支持的数据源类型: " + bo.getDsType())
                    .testTime(LocalDateTime.now())
                    .build();
            }

            long tempDsId = -ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);

            // 临时注册数据源进行连接测试
            dataSourceManager.registerDataSource(
                tempDsId,
                bo.getDsType(),
                bo.getHost(),
                bo.getPort(),
                bo.getDatabaseName(),
                bo.getSchemaName(),
                bo.getUsername(),
                bo.getPassword(),
                bo.getConnectionParams(),
                typeEnum.getUrlTemplate(),
                typeEnum.getDriverClass()
            );

            try {
                DataSourceAdapter adapter = adapterRegistry.getOrCreateAdapter(
                    tempDsId, bo.getDsType(),
                    bo.getHost(), bo.getPort(),
                    bo.getDatabaseName(), bo.getSchemaName(),
                    bo.getUsername(), bo.getPassword(),
                    bo.getConnectionParams()
                );
                boolean success = adapter.testConnection();
                long elapsed = System.currentTimeMillis() - start;

                if (success) {
                    String version = getDatabaseVersion(adapter);
                    return ConnectionTestResultVO.builder()
                        .success(true)
                        .message("连接成功")
                        .databaseVersion(version)
                        .elapsedMs(elapsed)
                        .testTime(LocalDateTime.now())
                        .build();
                } else {
                    return ConnectionTestResultVO.builder()
                        .success(false)
                        .message("连接失败，请检查配置")
                        .elapsedMs(elapsed)
                        .testTime(LocalDateTime.now())
                        .build();
                }
            } finally {
                // 清理临时注册的数据源
                adapterRegistry.unregisterAdapter(tempDsId);
            }
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("连接测试异常", e);
            return ConnectionTestResultVO.builder()
                .success(false)
                .message("连接异常: " + e.getMessage())
                .elapsedMs(elapsed)
                .testTime(LocalDateTime.now())
                .build();
        }
    }

    private String getDatabaseVersion(DataSourceAdapter adapter) {
        try {
            List<Map<String, Object>> result = adapter.executeQuery(buildDatabaseVersionSql(adapter.getDataSourceType()));
            if (!result.isEmpty()) {
                Map<String, Object> row = result.get(0);
                Object version = row.get("version");
                if (version == null) {
                    version = row.get("VERSION");
                }
                if (version != null) {
                    return version.toString();
                }
                for (Object v : row.values()) {
                    if (v != null) {
                        return v.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("获取数据库版本失败", e);
        }
        return "";
    }

    private String buildDatabaseVersionSql(String dsType) {
        return switch (StringUtils.toRootUpperCase(dsType)) {
            case "SQLSERVER" -> "SELECT @@VERSION AS version";
            case "ORACLE" -> "SELECT banner AS version FROM v$version WHERE ROWNUM = 1";
            default -> "SELECT VERSION() AS version";
        };
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public int updateStatus(Long dsId, String status) {
        getDatasourceOrThrow(dsId, false);
        SysDatasource ds = new SysDatasource(dsId);
        ds.setStatus(status);
        int rows = baseMapper.updateById(ds);
        // 如果停用，需要注销连接池
        if (SystemConstants.DISABLE.equals(status)) {
            adapterRegistry.unregisterAdapter(dsId);
        }
        return rows;
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public List<SysDatasourceVo> listEnabledDatasource() {
        List<SysDatasourceVo> list = baseMapper.selectVoList(
            Wrappers.<SysDatasource>lambdaQuery()
                .eq(SysDatasource::getStatus, SystemConstants.NORMAL)
                .orderByAsc(SysDatasource::getDsId)
        );
        maskSensitiveFields(list);
        return list;
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public List<String> getTables(Long dsId, String schema) {
        SysDatasource ds = getDatasourceOrThrow(dsId, true);
        ds.setPassword(cryptoSupport.decryptPassword(ds.getPassword()));
        DataSourceAdapter adapter = adapterRegistry.getOrCreateAdapter(
            dsId, ds.getDsType(),
            ds.getHost(), ds.getPort(),
            ds.getDatabaseName(), ds.getSchemaName(),
            ds.getUsername(), ds.getPassword(),
            ds.getConnectionParams()
        );
        String resolvedSchema = resolveSchemaName(ds, schema);
        if (StringUtils.isNotBlank(resolvedSchema)) {
            return adapter.getTables(resolvedSchema);
        }
        return adapter.getTables();
    }

    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public List<?> getTableColumns(Long dsId, String tableName, String schema) {
        SysDatasource ds = getDatasourceOrThrow(dsId, true);
        ds.setPassword(cryptoSupport.decryptPassword(ds.getPassword()));
        DataSourceAdapter adapter = adapterRegistry.getOrCreateAdapter(
            dsId, ds.getDsType(),
            ds.getHost(), ds.getPort(),
            ds.getDatabaseName(), ds.getSchemaName(),
            ds.getUsername(), ds.getPassword(),
            ds.getConnectionParams()
        );
        String resolvedSchema = resolveSchemaName(ds, schema);
        if (StringUtils.isNotBlank(resolvedSchema)) {
            return adapter.getColumns(resolvedSchema, tableName);
        }
        return adapter.getColumns(tableName);
    }

    /**
     * 校验数据源编码唯一性
     */
    private boolean checkDsCodeUnique(SysDatasourceBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysDatasource>()
            .eq(SysDatasource::getDsCode, bo.getDsCode())
            .ne(ObjectUtil.isNotNull(bo.getDsId()), SysDatasource::getDsId, bo.getDsId()));
        return !exist;
    }

    private void ensureDsCodeForInsert(SysDatasourceBo bo) {
        boolean autoGenerated = false;
        if (StringUtils.isBlank(bo.getDsCode())) {
            if (StringUtils.isBlank(bo.getDsType())) {
                throw new ServiceException("数据源类型不能为空");
            }
            bo.setDsCode(generateDsCode(bo.getDsType()));
            autoGenerated = true;
        } else {
            bo.setDsCode(StringUtils.trim(bo.getDsCode()));
        }
        if (!autoGenerated && !checkDsCodeUnique(bo)) {
            throw new ServiceException("数据源编码已存在");
        }
        int attempts = 0;
        while (autoGenerated && !checkDsCodeUnique(bo)) {
            if (attempts++ > 10) {
                throw new ServiceException("生成数据源编码失败，请稍后重试");
            }
            bo.setDsCode(generateDsCode(bo.getDsType()));
        }
    }

    private String generateDsCode(String dsType) {
        String type = StringUtils.toRootUpperCase(StringUtils.blankToDefault(dsType, "DS"));
        String timestamp = DS_CODE_FORMATTER.format(LocalDateTime.now());
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return type + "_" + timestamp + "_" + random;
    }

    private void encryptSensitiveFields(SysDatasourceBo bo) {
        if (StringUtils.isNotBlank(bo.getPassword())) {
            bo.setPassword(cryptoSupport.encryptPassword(bo.getPassword()));
        }
    }

    private void prepareTestConnectionBo(SysDatasourceBo bo) {
        if (bo.getDsId() == null) {
            if (cryptoSupport.isEncrypted(bo.getPassword())) {
                bo.setPassword(cryptoSupport.decryptPassword(bo.getPassword()));
            }
            return;
        }
        SysDatasource ds = getDatasourceOrThrow(bo.getDsId(), false);
        if (StringUtils.isBlank(bo.getDsType())) {
            bo.setDsType(ds.getDsType());
        }
        if (StringUtils.isBlank(bo.getHost())) {
            bo.setHost(ds.getHost());
        }
        if (bo.getPort() == null) {
            bo.setPort(ds.getPort());
        }
        if (StringUtils.isBlank(bo.getDatabaseName())) {
            bo.setDatabaseName(ds.getDatabaseName());
        }
        if (StringUtils.isBlank(bo.getSchemaName())) {
            bo.setSchemaName(ds.getSchemaName());
        }
        if (StringUtils.isBlank(bo.getUsername())) {
            bo.setUsername(ds.getUsername());
        }
        if (StringUtils.isBlank(bo.getPassword())) {
            bo.setPassword(cryptoSupport.decryptPassword(ds.getPassword()));
        } else if (cryptoSupport.isEncrypted(bo.getPassword())) {
            bo.setPassword(cryptoSupport.decryptPassword(bo.getPassword()));
        }
        if (StringUtils.isBlank(bo.getConnectionParams())) {
            bo.setConnectionParams(ds.getConnectionParams());
        }
    }

    private SysDatasource getDatasourceOrThrow(Long dsId, boolean requireEnabled) {
        SysDatasource datasource = baseMapper.selectById(dsId);
        if (datasource == null) {
            throw new ServiceException("数据源不存在或无权限访问");
        }
        if (requireEnabled && !SystemConstants.NORMAL.equals(datasource.getStatus())) {
            throw new ServiceException("数据源已停用，请先启用后再操作");
        }
        return datasource;
    }

    private String resolveSchemaName(SysDatasource ds, String schema) {
        String resolvedSchema = StringUtils.isNotBlank(schema) ? schema : ds.getSchemaName();
        if (StringUtils.isBlank(resolvedSchema)) {
            return null;
        }
        return StringUtils.trim(resolvedSchema);
    }

    private void maskSensitiveFields(List<SysDatasourceVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SysDatasourceVo item : list) {
            item.setUsername(null);
            item.setPassword(null);
            item.setConnectionParams(null);
        }
    }
}
