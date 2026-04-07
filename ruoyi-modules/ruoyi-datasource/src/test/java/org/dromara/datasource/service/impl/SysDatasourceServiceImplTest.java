package org.dromara.datasource.service.impl;

import org.dromara.datasource.adapter.DataSourceAdapterRegistry;
import org.dromara.datasource.connection.ConnectionTestResultVO;
import org.dromara.datasource.domain.SysDatasource;
import org.dromara.datasource.domain.bo.SysDatasourceBo;
import org.dromara.datasource.domain.vo.SysDatasourceVo;
import org.dromara.datasource.manager.DynamicDataSourceManager;
import org.dromara.datasource.mapper.SysDatasourceMapper;
import org.dromara.datasource.support.DatasourceCryptoSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("local")
class SysDatasourceServiceImplTest {

    @Mock
    private SysDatasourceMapper baseMapper;

    @Mock
    private DynamicDataSourceManager dataSourceManager;

    @Mock
    private DataSourceAdapterRegistry adapterRegistry;

    @Mock
    private DatasourceCryptoSupport cryptoSupport;

    @InjectMocks
    private SysDatasourceServiceImpl service;

    @Test
    void getDatasourceByIdShouldNotExposePassword() {
        SysDatasourceVo vo = new SysDatasourceVo();
        vo.setDsId(1L);
        vo.setPassword("{enc}secret");
        when(baseMapper.selectVoById(1L)).thenReturn(vo);

        SysDatasourceVo result = service.getDatasourceById(1L);

        assertNotNull(result);
        assertNull(result.getPassword());
        verifyNoInteractions(cryptoSupport);
    }

    @Test
    void preserveStoredPasswordIfNecessaryShouldKeepExistingEncryptedPassword() {
        SysDatasourceBo bo = new SysDatasourceBo();
        bo.setDsId(1L);
        bo.setPassword("");

        SysDatasource existing = new SysDatasource();
        existing.setDsId(1L);
        existing.setPassword("{enc}keep-me");

        ReflectionTestUtils.invokeMethod(service, "preserveStoredPasswordIfNecessary", bo, existing);

        assertEquals("{enc}keep-me", bo.getPassword());
        verify(cryptoSupport, never()).encryptPassword(anyString());
    }

    @Test
    void testConnectionShouldRejectStoredPasswordReuseWhenTargetChanges() {
        SysDatasourceBo bo = new SysDatasourceBo();
        bo.setDsId(1L);
        bo.setHost("evil-host");

        SysDatasource existing = new SysDatasource();
        existing.setDsId(1L);
        existing.setDsType("MYSQL");
        existing.setHost("10.0.0.8");
        existing.setPort(3306);
        existing.setDatabaseName("prod_db");
        existing.setSchemaName(null);
        existing.setUsername("prod_user");
        existing.setPassword("{enc}prod-secret");

        when(baseMapper.selectById(1L)).thenReturn(existing);

        ConnectionTestResultVO result = service.testConnection(bo);

        assertNotNull(result);
        assertFalse(Boolean.TRUE.equals(result.getSuccess()));
        assertTrue(result.getMessage().contains("必须显式提供密码"));
        verifyNoInteractions(dataSourceManager);
        verify(adapterRegistry, never()).getOrCreateAdapter(anyLong(), anyString(), anyString(), any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }
}
