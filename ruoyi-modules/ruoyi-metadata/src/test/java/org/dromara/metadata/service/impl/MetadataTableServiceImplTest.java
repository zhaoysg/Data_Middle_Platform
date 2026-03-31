package org.dromara.metadata.service.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("local")
class MetadataTableServiceImplTest {

    @Test
    void shouldSplitDeduplicateAndSortTags() {
        List<String> tagOptions = MetadataTableServiceImpl.extractTagOptions(List.of(
            "主题域,核心",
            "核心，标签",
            "  分析  , 主题域 ",
            ""
        ));

        assertEquals(List.of("主题域", "分析", "标签", "核心"), tagOptions);
    }
}
