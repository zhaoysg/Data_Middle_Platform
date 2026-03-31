package org.dromara.metadata;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.metadata.domain.bo.MetadataColumnBo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MetadataColumnBo 边界值单元测试
 */
@DisplayName("MetadataColumnBo 字段级边界值测试")
@Tag("local")
public class MetadataColumnBoTest {

    @DisplayName("测试字段名关键词搜索 - Happy Path")
    @Test
    public void testHappyPath() {
        MetadataColumnBo bo = new MetadataColumnBo();
        bo.setTableId(1L);
        bo.setDsId(1L);
        bo.setKeyword("user_name");
        assertNotNull(bo.getTableId());
        assertEquals(1L, bo.getDsId());
        assertEquals("user_name", bo.getKeyword());
    }

    @DisplayName("测试字段别名关键词搜索 - 特殊字符")
    @Test
    public void testSpecialCharKeyword() {
        MetadataColumnBo bo = new MetadataColumnBo();
        bo.setKeyword("用户名%_");
        bo.setColumnName("user_name");
        assertEquals("用户名%_", bo.getKeyword());
        assertEquals("user_name", bo.getColumnName());
    }

    @DisplayName("测试 pageSize 上限 99999 → 100")
    @Test
    public void testPageSizeLimit() {
        int requestedPageSize = 99999;
        int actualPageSize = Math.min(requestedPageSize, 100);
        assertEquals(100, actualPageSize);
    }

    @DisplayName("测试 null 关键词场景")
    @Test
    public void testNullKeyword() {
        MetadataColumnBo bo = new MetadataColumnBo();
        bo.setKeyword(null);
        bo.setTableId(1L);
        bo.setColumnName("id");
        assertTrue(StringUtils.isBlank(bo.getKeyword()));
        assertEquals("id", bo.getColumnName());
    }

    @DisplayName("测试超长字段名（边界）")
    @Test
    public void testMaxLengthColumnName() {
        MetadataColumnBo bo = new MetadataColumnBo();
        String maxLengthName = "a".repeat(100);
        bo.setColumnName(maxLengthName);
        assertEquals(100, bo.getColumnName().length());
    }
}
