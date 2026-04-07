package org.dromara.metadata.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.datasource.adapter.DataSourceAdapter;
import org.dromara.datasource.adapter.DataSourceAdapterRegistry;
import org.dromara.datasource.domain.SysDatasource;
import org.dromara.datasource.manager.DynamicDataSourceManager;
import org.dromara.datasource.mapper.SysDatasourceMapper;
import org.dromara.datasource.support.DatasourceCryptoSupport;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据源工具类
 * <p>
 * 统一封装数据源查询逻辑，消除跨 Track 的重复代码。
 * 所有需要访问外部数据源的 Service 均通过此类获取 JdbcTemplate 或 DataSourceAdapter。
 *
 * <p>典型用法：
 * <pre>{@code
 * // 方式一：直接获取 JdbcTemplate（推荐用于简单 SQL 查询）
 * JdbcTemplate jdbc = helper.getJdbcTemplate(dsId);
 * List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM users LIMIT 10");
 *
 * // 方式二：获取 DataSourceAdapter（推荐用于跨数据库兼容的元数据查询）
 * DataSourceAdapter adapter = helper.getAdapter(dsId);
 * List<String> tables = adapter.getTables();
 * List<ColumnInfo> columns = adapter.getColumns("users");
 * }</pre>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class DatasourceHelper {

    private final SysDatasourceMapper sysDatasourceMapper;
    private final DataSourceAdapterRegistry adapterRegistry;
    private final DynamicDataSourceManager dynamicDataSourceManager;
    private final DatasourceCryptoSupport cryptoSupport;

    /**
     * 根据 dsId 获取数据源配置实体。
     * 若数据源不存在或已删除，抛出 {@link ServiceException}。
     *
     * @param dsId 数据源ID（对应 sys_datasource.ds_id）
     * @return 数据源配置实体（密码字段已解密，仅存在于内存中）
     * @throws ServiceException 数据源不存在或已删除
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public SysDatasource getSysDatasource(Long dsId) {
        SysDatasource ds = sysDatasourceMapper.selectById(dsId);
        if (ds == null) {
            throw new ServiceException("数据源不存在: " + dsId);
        }
        if ("1".equals(ds.getDelFlag())) {
            throw new ServiceException("数据源已删除: " + dsId);
        }
        return ds;
    }

    /**
     * 根据 dsId 获取 JdbcTemplate（带连接池缓存）。
     * <p>
     * 内部自动完成：数据源查询 → 密码解密 → 连接池注册 → JdbcTemplate 创建。
     * 适用于需要执行任意 SQL 的场景（如 DQC 规则执行、MaskQuery、数据探查）。
     *
     * <p>注意：返回的 JdbcTemplate 由 HikariCP 连接池管理，每次调用 {@code queryForList}
     * 会从池中获取连接，执行完自动归还。调用方不应持有 Connection 对象。
     *
     * @param dsId 数据源ID
     * @return JdbcTemplate 实例
     * @throws ServiceException 数据源不存在或已删除
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public JdbcTemplate getJdbcTemplate(Long dsId) {
        SysDatasource ds = getSysDatasource(dsId);
        // 动态注册到连接池（如尚未注册）
        if (!dynamicDataSourceManager.isRegistered(dsId)) {
            String password = cryptoSupport.decryptPassword(ds.getPassword());
            dynamicDataSourceManager.registerDataSource(
                ds.getDsId(),
                ds.getDsType(),
                ds.getHost(),
                ds.getPort(),
                ds.getDatabaseName(),
                ds.getSchemaName(),
                ds.getUsername(),
                password,
                ds.getConnectionParams(),
                resolveUrlTemplate(ds.getDsType()),
                resolveDriverClass(ds.getDsType())
            );
        }
        JdbcTemplate jdbcTemplate = dynamicDataSourceManager.getJdbcTemplateById(dsId);
        if (jdbcTemplate == null) {
            throw new ServiceException("数据源连接失败: " + dsId);
        }
        return jdbcTemplate;
    }

    /**
     * 根据 dsId 获取 DataSourceAdapter（带缓存）。
     * <p>
     * DataSourceAdapter 屏蔽了不同数据库的语法差异，提供统一的元数据查询能力：
     * {@code getTables()}、{@code getColumns(table)}、{@code getRowCount(table)} 等。
     * 适用于数据字典扫描、列信息采集等场景。
     *
     * @param dsId 数据源ID
     * @return DataSourceAdapter 实例（永不为 null，失败时抛异常）
     * @throws ServiceException 数据源不存在或类型不支持
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public DataSourceAdapter getAdapter(Long dsId) {
        SysDatasource ds = getSysDatasource(dsId);
        String password = cryptoSupport.decryptPassword(ds.getPassword());
        DataSourceAdapter adapter = adapterRegistry.getOrCreateAdapter(
            ds.getDsId(),
            ds.getDsType(),
            ds.getHost(),
            ds.getPort(),
            ds.getDatabaseName(),
            ds.getSchemaName(),
            ds.getUsername(),
            password,
            ds.getConnectionParams()
        );
        return adapter;
    }

    /**
     * 获取当前用户可访问的数据源ID列表。
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public List<Long> listAccessibleDatasourceIds() {
        return sysDatasourceMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysDatasource>()
                    .select(SysDatasource::getDsId)
            ).stream()
            .map(SysDatasource::getDsId)
            .toList();
    }

    /**
     * 解析请求允许访问的数据源ID集合。
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id")
    })
    public List<Long> resolveAccessibleDatasourceIds(Long dsId) {
        if (dsId != null) {
            getSysDatasource(dsId);
            return List.of(dsId);
        }
        return listAccessibleDatasourceIds();
    }

    /**
     * 失效指定数据源的缓存（连接池 + 适配器缓存）。
     * <p>
     * 在数据源配置变更（密码/地址/端口等）后必须调用此方法，
     * 否则旧的缓存连接会导致新配置不生效。
     *
     * @param dsId 数据源ID
     */
    public void invalidateCache(Long dsId) {
        adapterRegistry.unregisterAdapter(dsId);
        dynamicDataSourceManager.unregisterDataSource(dsId);
        log.info("数据源缓存已失效: dsId={}", dsId);
    }

    /**
     * 解析 JDBC URL 模板
     */
    private String resolveUrlTemplate(String dsType) {
        return switch (dsType.toUpperCase()) {
            case "MYSQL", "TIDB" -> "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
            case "POSTGRESQL" -> "jdbc:postgresql://%s:%d/%s";
            case "SQLSERVER" -> "jdbc:sqlserver://%s:%d;DatabaseName=%s;encrypt=false;trustServerCertificate=true";
            case "ORACLE" -> "jdbc:oracle:thin:@%s:%d:%s";
            default -> throw new ServiceException("不支持的数据源类型: " + dsType);
        };
    }

    /**
     * 解析驱动类名
     */
    private String resolveDriverClass(String dsType) {
        return switch (dsType.toUpperCase()) {
            case "MYSQL", "TIDB" -> "com.mysql.cj.jdbc.Driver";
            case "POSTGRESQL" -> "org.postgresql.Driver";
            case "SQLSERVER" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "ORACLE" -> "oracle.jdbc.OracleDriver";
            default -> throw new ServiceException("不支持的数据源类型: " + dsType);
        };
    }
}
