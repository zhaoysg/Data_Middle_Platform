package org.dromara.metadata.support;

import org.dromara.metadata.domain.bo.MetadataScanBo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("local")
class MetadataScanRuleMatcherTest {

    @Test
    void shouldFallbackToSelectedTableModeWhenTableNamesProvided() {
        MetadataScanBo bo = new MetadataScanBo();
        bo.setTableNames(List.of("user", "order"));

        List<String> resolved = MetadataScanRuleMatcher.resolveTables(
            List.of("user", "order", "log"), bo);

        assertEquals(List.of("user", "order"), resolved);
    }

    @Test
    void shouldMatchRuleModeWithExcludePattern() {
        MetadataScanBo bo = new MetadataScanBo();
        bo.setScanMode(MetadataScanRuleMatcher.MODE_RULE);
        bo.setRuleType(MetadataScanRuleMatcher.RULE_PREFIX);
        bo.setIncludePattern("ods_,dwd_");
        bo.setExcludePattern("ods_tmp");

        List<String> resolved = MetadataScanRuleMatcher.resolveTables(
            List.of("ods_order", "ods_tmp_log", "dwd_order", "ads_report"), bo);

        assertEquals(List.of("ods_order", "dwd_order"), resolved);
    }

    @Test
    void shouldSupportRegexRuleMatching() {
        MetadataScanBo bo = new MetadataScanBo();
        bo.setScanMode(MetadataScanRuleMatcher.MODE_RULE);
        bo.setRuleType(MetadataScanRuleMatcher.RULE_REGEX);
        bo.setIncludePattern("^dim_.*_(di|df)$");

        List<String> resolved = MetadataScanRuleMatcher.resolveTables(
            List.of("dim_user_di", "dim_order_df", "fact_order_di"), bo);

        assertEquals(List.of("dim_user_di", "dim_order_df"), resolved);
    }
}
