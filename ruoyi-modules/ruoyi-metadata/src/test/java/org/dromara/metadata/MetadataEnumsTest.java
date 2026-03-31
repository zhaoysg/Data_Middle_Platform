package org.dromara.metadata;

import org.dromara.metadata.domain.bo.DataLayerBo;
import org.dromara.metadata.domain.bo.MetadataColumnBo;
import org.dromara.metadata.domain.bo.MetadataTableBo;
import org.dromara.metadata.enums.MetadataStatusEnum;
import org.dromara.metadata.enums.SensitivityLevelEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MetadataEnumsTest 枚举单元测试
 */
@DisplayName("MetadataEnumsTest 枚举及边界值测试")
@Tag("local")
public class MetadataEnumsTest {

    @DisplayName("测试 MetadataStatusEnum 枚举值")
    @Test
    public void testMetadataStatusEnum() {
        assertEquals("ACTIVE", MetadataStatusEnum.ACTIVE.getCode());
        assertEquals("ARCHIVED", MetadataStatusEnum.ARCHIVED.getCode());
        assertEquals("DEPRECATED", MetadataStatusEnum.DEPRECATED.getCode());
        assertEquals(3, MetadataStatusEnum.values().length);
    }

    @DisplayName("测试 SensitivityLevelEnum 枚举值及描述")
    @Test
    public void testSensitivityLevelEnum() {
        assertEquals("NORMAL", SensitivityLevelEnum.NORMAL.getCode());
        assertEquals("普通", SensitivityLevelEnum.NORMAL.getDesc());
        assertEquals("HIGHLY_SENSITIVE", SensitivityLevelEnum.HIGHLY_SENSITIVE.getCode());
        assertEquals("高度敏感", SensitivityLevelEnum.HIGHLY_SENSITIVE.getDesc());
        assertEquals("SENSITIVE", SensitivityLevelEnum.SENSITIVE.getCode());
        assertEquals("敏感", SensitivityLevelEnum.SENSITIVE.getDesc());
        assertEquals(4, SensitivityLevelEnum.values().length);
    }

    @DisplayName("测试 SensitivityLevelEnum.fromCode 静态方法")
    @Test
    public void testSensitivityLevelEnumFromCode() {
        assertEquals(SensitivityLevelEnum.NORMAL, SensitivityLevelEnum.fromCode("NORMAL"));
        assertEquals(SensitivityLevelEnum.SENSITIVE, SensitivityLevelEnum.fromCode("SENSITIVE"));
        assertEquals(SensitivityLevelEnum.HIGHLY_SENSITIVE, SensitivityLevelEnum.fromCode("HIGHLY_SENSITIVE"));
        assertNull(SensitivityLevelEnum.fromCode("INVALID"));
        assertNull(SensitivityLevelEnum.fromCode(null));
    }

    @DisplayName("测试 DataLayerBo 枚举绑定场景")
    @Test
    public void testDataLayerBoBoundary() {
        DataLayerBo bo = new DataLayerBo();
        bo.setLayerCode("ODS");
        bo.setLayerName("原始层");
        bo.setLayerColor("#909399");
        bo.setSortOrder(1);
        assertEquals("ODS", bo.getLayerCode());
        assertEquals("#909399", bo.getLayerColor());
        assertEquals(1, bo.getSortOrder());
        assertNotNull(bo.getLayerCode());
        assertNotNull(bo.getLayerName());
    }

    @DisplayName("测试枚举 getCode / getDesc 方法")
    @Test
    public void testEnumMethods() {
        assertEquals("活跃", MetadataStatusEnum.ACTIVE.getDesc());
        assertEquals("普通", SensitivityLevelEnum.NORMAL.getDesc());
        assertEquals("高度敏感", SensitivityLevelEnum.HIGHLY_SENSITIVE.getDesc());
    }
}
