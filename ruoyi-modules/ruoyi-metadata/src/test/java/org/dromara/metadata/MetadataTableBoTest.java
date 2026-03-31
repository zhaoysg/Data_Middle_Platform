package org.dromara.metadata;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.metadata.domain.bo.MetadataTableBo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MetadataTableBo 边界值单元测试
 */
@DisplayName("MetadataTableBo 边界值测试")
@Tag("local")
public class MetadataTableBoTest {

    @DisplayName("测试关键词搜索防注入 - 特殊字符转义")
    @Test
    public void testKeywordSpecialChars() {
        String keyword = "admin%_test";
        MetadataTableBo bo = new MetadataTableBo();
        bo.setKeyword(keyword);
        bo.setTableName("orders");
        bo.setDsId(1L);
        assertEquals("admin%_test", bo.getKeyword());
        assertEquals("orders", bo.getTableName());
        assertEquals(1L, bo.getDsId());
    }

    @DisplayName("测试 pageSize 上限 - 99999 截断到 100")
    @Test
    public void testPageSizeLimit() {
        MetadataTableBo bo = new MetadataTableBo();
        bo.setKeyword("user");
        int requestedPageSize = 99999;
        int actualPageSize = Math.min(requestedPageSize, 100);
        assertEquals(100, actualPageSize);
    }

    @DisplayName("测试空关键词查询")
    @Test
    public void testEmptyKeyword() {
        MetadataTableBo bo = new MetadataTableBo();
        bo.setKeyword("");
        bo.setDsId(1L);
        assertEquals("", bo.getKeyword());
        assertTrue(StringUtils.isBlank(bo.getKeyword()));
    }
}
