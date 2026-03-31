package org.dromara.metadata.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

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
}
