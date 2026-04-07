package org.dromara.metadata.engine.executor;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("local")
class CustomSqlSecuritySupportTest {

    @Test
    void validateRuleExprShouldRejectDangerousSql() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> CustomSqlSecuritySupport.validateRuleExpr("SELECT count(*) FROM user_info; DELETE FROM user_info")
        );

        assertTrue(ex.getMessage().contains("禁止") || ex.getMessage().contains("只读"));
    }

    @Test
    void validateRuleExprShouldRejectPlainColumnProjection() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> CustomSqlSecuritySupport.validateRuleExpr("SELECT phone FROM user_info")
        );

        assertTrue(ex.getMessage().contains("聚合值") || ex.getMessage().contains("时间函数"));
    }

    @Test
    void isAllowedResultTypeShouldOnlyAcceptNumericOrTemporalPayload() {
        assertTrue(CustomSqlSecuritySupport.isAllowedResultType(123L));
        assertTrue(CustomSqlSecuritySupport.isAllowedResultType("2024-01-02T03:04:05"));
        assertFalse(CustomSqlSecuritySupport.isAllowedResultType("secret-value"));
    }
}
