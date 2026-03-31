package org.dromara.datasource.adapter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("local")
class PostgresAdapterTest {

    @Test
    void getTableLastUpdateTimeShouldReturnEmptyBecausePostgresHasNoReliableMetadata() {
        PostgresAdapter adapter = new PostgresAdapter(null);

        assertTrue(adapter.getTableLastUpdateTime("public", "orders").isEmpty());
    }
}
