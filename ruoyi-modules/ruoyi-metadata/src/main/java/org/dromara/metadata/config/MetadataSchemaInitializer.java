package org.dromara.metadata.config;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Initializes metadata schema objects on the bigdata datasource.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "metadata.schema", name = "auto-init", havingValue = "true")
public class MetadataSchemaInitializer implements ApplicationRunner {

    private static final String BIGDATA_DS = "bigdata";
    private static final String SCRIPT_LOCATION = "classpath*:sql/metadata/*.sql";
    private static final String HISTORY_TABLE_NAME = "metadata_schema_history";
    private static final String HISTORY_TABLE_DDL = "CREATE TABLE IF NOT EXISTS `" + HISTORY_TABLE_NAME + "` ("
        + " `script_name` varchar(255) NOT NULL COMMENT 'script file name',"
        + " `executed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'execution time',"
        + " PRIMARY KEY (`script_name`)"
        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='metadata init history'";
    private static final String HISTORY_EXISTS_SQL =
        "SELECT COUNT(1) FROM `" + HISTORY_TABLE_NAME + "` WHERE script_name = ?";
    private static final String HISTORY_INSERT_SQL =
        "INSERT INTO `" + HISTORY_TABLE_NAME + "` (script_name, executed_at) VALUES (?, ?)";
    private static final Pattern BLOCK_COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT_PATTERN = Pattern.compile("(?m)^\\s*(--|#).*$");

    private final DataSource dataSource;
    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public MetadataSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource[] scripts = resourceResolver.getResources(SCRIPT_LOCATION);
        if (scripts.length == 0) {
            log.warn("No metadata init scripts found at {}", SCRIPT_LOCATION);
            return;
        }
        Arrays.sort(scripts, Comparator.comparing(MetadataSchemaInitializer::resolveScriptName,
            Comparator.nullsLast(String::compareTo)));
        DynamicDataSourceContextHolder.push(BIGDATA_DS);
        try (Connection connection = dataSource.getConnection()) {
            if (!supportsMetadataAutoInit(connection)) {
                String databaseProductName = resolveDatabaseProductName(connection);
                log.warn("Database {} does not support MySQL-style metadata init scripts, skip auto init",
                    databaseProductName);
                return;
            }
            ensureHistoryTable(connection);
            for (Resource script : scripts) {
                String scriptName = resolveScriptName(script);
                if (hasScriptExecuted(connection, scriptName)) {
                    log.info("Metadata init script already applied, skip: {}", scriptName);
                    continue;
                }
                if (!hasExecutableSql(script)) {
                    log.info("Metadata init script contains no executable SQL, skip: {}", scriptName);
                    continue;
                }
                log.info("Executing metadata init script: {}", scriptName);
                ScriptUtils.executeSqlScript(connection, new EncodedResource(script, StandardCharsets.UTF_8));
                markScriptExecuted(connection, scriptName);
            }
            log.info("Metadata schema initialization completed");
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    static String resolveScriptName(Resource script) {
        String filename = script.getFilename();
        return filename != null ? filename : script.getDescription();
    }

    static boolean hasExecutableSql(Resource script) throws IOException {
        String content = new String(script.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String normalized = BLOCK_COMMENT_PATTERN.matcher(content).replaceAll("");
        normalized = LINE_COMMENT_PATTERN.matcher(normalized).replaceAll("");
        normalized = normalized.replace(";", "").trim();
        return !normalized.isEmpty();
    }

    static void ensureHistoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(HISTORY_TABLE_DDL);
        }
        commitIfNecessary(connection);
    }

    static boolean hasScriptExecuted(Connection connection, String scriptName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(HISTORY_EXISTS_SQL)) {
            statement.setString(1, scriptName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    static void markScriptExecuted(Connection connection, String scriptName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(HISTORY_INSERT_SQL)) {
            statement.setString(1, scriptName);
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
        commitIfNecessary(connection);
    }

    static void commitIfNecessary(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
    }

    static boolean supportsMetadataAutoInit(Connection connection) {
        String databaseProductName = resolveDatabaseProductName(connection);
        if (databaseProductName == null) {
            return false;
        }
        String normalizedName = databaseProductName.toLowerCase(Locale.ROOT);
        return normalizedName.contains("mysql") || normalizedName.contains("mariadb");
    }

    static String resolveDatabaseProductName(Connection connection) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData != null ? metaData.getDatabaseProductName() : null;
        } catch (Exception e) {
            log.warn("Failed to read database product name, skip metadata auto init", e);
            return null;
        }
    }
}
