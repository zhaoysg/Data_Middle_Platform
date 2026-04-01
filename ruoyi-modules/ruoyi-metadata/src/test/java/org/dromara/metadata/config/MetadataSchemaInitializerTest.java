package org.dromara.metadata.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
class MetadataSchemaInitializerTest {

    @Test
    void shouldAllowMysqlAutoInit() {
        assertTrue(MetadataSchemaInitializer.supportsMetadataAutoInit(mockConnection("MySQL")));
    }

    @Test
    void shouldBlockPostgresAutoInit() {
        assertFalse(MetadataSchemaInitializer.supportsMetadataAutoInit(mockConnection("PostgreSQL")));
    }

    @Test
    void shouldFallbackToDescriptionWhenFilenameMissing() {
        Resource resource = new ByteArrayResource(new byte[0], "memory-script") {
            @Override
            public String getFilename() {
                return null;
            }
        };

        assertEquals(resource.getDescription(), MetadataSchemaInitializer.resolveScriptName(resource));
    }

    @Test
    void shouldTrackExecutedScriptsInHistoryTable() throws Exception {
        HistoryDb historyDb = new HistoryDb(false);
        Connection connection = historyDb.connection();

        MetadataSchemaInitializer.ensureHistoryTable(connection);
        assertTrue(historyDb.historyTableCreated.get());
        assertFalse(MetadataSchemaInitializer.hasScriptExecuted(connection, "V3__phase3_all.sql"));

        MetadataSchemaInitializer.markScriptExecuted(connection, "V3__phase3_all.sql");

        assertTrue(MetadataSchemaInitializer.hasScriptExecuted(connection, "V3__phase3_all.sql"));
        assertEquals(2, historyDb.commitCount.get());
    }

    private static Connection mockConnection(String productName) {
        DatabaseMetaData metaData = (DatabaseMetaData) Proxy.newProxyInstance(
            DatabaseMetaData.class.getClassLoader(),
            new Class<?>[]{DatabaseMetaData.class},
            (proxy, method, args) -> {
                if ("getDatabaseProductName".equals(method.getName())) {
                    return productName;
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );

        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                if ("getMetaData".equals(method.getName())) {
                    return metaData;
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private static final class HistoryDb {
        private final boolean autoCommit;
        private final AtomicBoolean historyTableCreated = new AtomicBoolean(false);
        private final AtomicInteger commitCount = new AtomicInteger(0);
        private final Set<String> executedScripts = new HashSet<>();

        private HistoryDb(boolean autoCommit) {
            this.autoCommit = autoCommit;
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "createStatement":
                            return createStatement();
                        case "prepareStatement":
                            return createPreparedStatement((String) args[0]);
                        case "getAutoCommit":
                            return autoCommit;
                        case "commit":
                            commitCount.incrementAndGet();
                            return null;
                        case "close":
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                }
            );
        }

        private Statement createStatement() {
            return (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "execute":
                            String sql = (String) args[0];
                            if (sql.contains("CREATE TABLE IF NOT EXISTS `metadata_schema_history`")) {
                                historyTableCreated.set(true);
                                return true;
                            }
                            throw new UnsupportedOperationException(sql);
                        case "close":
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                }
            );
        }

        private PreparedStatement createPreparedStatement(String sql) {
            AtomicReference<String> scriptName = new AtomicReference<>();
            return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setString":
                            scriptName.set((String) args[1]);
                            return null;
                        case "setTimestamp":
                            return null;
                        case "executeQuery":
                            if (sql.startsWith("SELECT COUNT(1) FROM `metadata_schema_history`")) {
                                return countResultSet(() -> executedScripts.contains(scriptName.get()) ? 1 : 0);
                            }
                            throw new UnsupportedOperationException(sql);
                        case "executeUpdate":
                            if (sql.startsWith("INSERT INTO `metadata_schema_history`")) {
                                executedScripts.add(scriptName.get());
                                return 1;
                            }
                            throw new UnsupportedOperationException(sql);
                        case "close":
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                }
            );
        }

        private ResultSet countResultSet(IntSupplier countSupplier) {
            AtomicBoolean firstRow = new AtomicBoolean(true);
            return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "next":
                            return firstRow.getAndSet(false);
                        case "getInt":
                            return countSupplier.getAsInt();
                        case "close":
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                }
            );
        }
    }
}
