package com.bagdatahouse.dqc.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("规则类型枚举测试")
class RuleTypeEnumTest {

    @Nested
    @DisplayName("基础方法测试")
    class BasicMethodsTest {
        @Test
        @DisplayName("NULL_CHECK枚举值验证")
        void testNullCheckEnum() {
            RuleTypeEnum nullCheck = RuleTypeEnum.NULL_CHECK;
            assertThat(nullCheck.getCode()).isEqualTo("NULL_CHECK");
            assertThat(nullCheck.getName()).isEqualTo("空值检查");
            assertThat(nullCheck.getDescription()).isEqualTo("检测字段的空值/非空状态");
        }

        @Test
        @DisplayName("UNIQUE_CHECK枚举值验证")
        void testUniqueCheckEnum() {
            RuleTypeEnum uniqueCheck = RuleTypeEnum.UNIQUE_CHECK;
            assertThat(uniqueCheck.getCode()).isEqualTo("UNIQUE_CHECK");
            assertThat(uniqueCheck.getName()).isEqualTo("唯一性检查");
        }

        @Test
        @DisplayName("CUSTOM_SQL枚举值验证")
        void testCustomSqlEnum() {
            RuleTypeEnum customSql = RuleTypeEnum.CUSTOM_SQL;
            assertThat(customSql.getCode()).isEqualTo("CUSTOM_SQL");
            assertThat(customSql.getName()).isEqualTo("自定义SQL检查");
        }

        @Test
        @DisplayName("CUSTOM_FUNC枚举值验证")
        void testCustomFuncEnum() {
            RuleTypeEnum customFunc = RuleTypeEnum.CUSTOM_FUNC;
            assertThat(customFunc.getCode()).isEqualTo("CUSTOM_FUNC");
            assertThat(customFunc.getName()).isEqualTo("自定义函数检查");
        }
    }

    @Nested
    @DisplayName("fromCode方法测试")
    class FromCodeTest {
        @Test
        @DisplayName("正常code转换")
        void testNormalCode() {
            assertThat(RuleTypeEnum.fromCode("NULL_CHECK")).isEqualTo(RuleTypeEnum.NULL_CHECK);
            assertThat(RuleTypeEnum.fromCode("UNIQUE_CHECK")).isEqualTo(RuleTypeEnum.UNIQUE_CHECK);
            assertThat(RuleTypeEnum.fromCode("REGEX_PHONE")).isEqualTo(RuleTypeEnum.REGEX_PHONE);
        }

        @Test
        @DisplayName("大小写不敏感")
        void testCaseInsensitive() {
            assertThat(RuleTypeEnum.fromCode("null_check")).isEqualTo(RuleTypeEnum.NULL_CHECK);
            assertThat(RuleTypeEnum.fromCode("Null_Check")).isEqualTo(RuleTypeEnum.NULL_CHECK);
        }

        @Test
        @DisplayName("无效code返回null")
        void testInvalidCode() {
            assertThat(RuleTypeEnum.fromCode("INVALID")).isNull();
            assertThat(RuleTypeEnum.fromCode(null)).isNull();
            assertThat(RuleTypeEnum.fromCode("")).isNull();
        }
    }

    @Nested
    @DisplayName("getNameByCode方法测试")
    class GetNameByCodeTest {
        @Test
        @DisplayName("正常获取名称")
        void testNormalName() {
            assertThat(RuleTypeEnum.getNameByCode("NULL_CHECK")).isEqualTo("空值检查");
            assertThat(RuleTypeEnum.getNameByCode("ROW_COUNT_FLUCTUATION")).isEqualTo("行数波动检查");
        }

        @Test
        @DisplayName("无效code返回原code")
        void testInvalidReturnsCode() {
            assertThat(RuleTypeEnum.getNameByCode("INVALID")).isEqualTo("INVALID");
            assertThat(RuleTypeEnum.getNameByCode(null)).isNull();
        }
    }

    @Nested
    @DisplayName("枚举完整性测试")
    class EnumCompletenessTest {
        @Test
        @DisplayName("所有枚举值都有code")
        void testAllHaveCode() {
            for (RuleTypeEnum type : RuleTypeEnum.values()) {
                assertThat(type.getCode())
                        .as("枚举 %s 的 code 不应为空", type)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("所有枚举值都有name")
        void testAllHaveName() {
            for (RuleTypeEnum type : RuleTypeEnum.values()) {
                assertThat(type.getName())
                        .as("枚举 %s 的 name 不应为空", type)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("所有枚举值都有description")
        void testAllHaveDescription() {
            for (RuleTypeEnum type : RuleTypeEnum.values()) {
                assertThat(type.getDescription())
                        .as("枚举 %s 的 description 不应为空", type)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("枚举总数验证")
        void testEnumCount() {
            // 当前有22种规则类型
            assertThat(RuleTypeEnum.values()).hasSize(22);
        }
    }
}
