package org.dromara.metadata.service.impl;

import org.dromara.metadata.domain.vo.DqcRuleDefVo;
import org.dromara.metadata.mapper.DqcPlanRuleMapper;
import org.dromara.metadata.mapper.DqcRuleDefMapper;
import org.dromara.metadata.mapper.MetadataColumnMapper;
import org.dromara.metadata.mapper.MetadataTableMapper;
import org.dromara.metadata.support.DatasourceHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("local")
class DqcRuleDefServiceImplTest {

    @Mock
    private DqcRuleDefMapper baseMapper;

    @Mock
    private DqcPlanRuleMapper planRuleMapper;

    @Mock
    private DatasourceHelper datasourceHelper;

    @Mock
    private MetadataTableMapper metadataTableMapper;

    @Mock
    private MetadataColumnMapper metadataColumnMapper;

    @InjectMocks
    private DqcRuleDefServiceImpl service;

    @Test
    void enrichMetadataInfoShouldSkipBatchLookupWhenNoMetadataIdsExist() {
        ReflectionTestUtils.invokeMethod(service, "enrichMetadataInfo", List.of(new DqcRuleDefVo()));

        verify(metadataTableMapper, never()).selectBatchIds(anyList());
        verify(metadataColumnMapper, never()).selectBatchIds(anyList());
    }
}
