package org.dromara.metadata.service.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("local")
public class MetadataScanServiceImplTest {

    @Test
    void shouldPreferEntityTenantId() {
        assertEquals("000123", MetadataScanServiceImpl.resolveTenantId(" 000123 ", null, null));
    }

    @Test
    void shouldUseRequestTenantIdBeforeThreadLocalTenant() {
        assertEquals("000456", MetadataScanServiceImpl.resolveTenantId(null, " 000456 ", "000789"));
    }

    @Test
    void shouldFallbackToDefaultTenantId() {
        assertEquals(MetadataScanServiceImpl.DEFAULT_TENANT_ID, MetadataScanServiceImpl.resolveTenantId(null, null, null));
    }
}
