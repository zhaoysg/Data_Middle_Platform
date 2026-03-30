package com.bagdatahouse.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("字符串工具类测试")
class StrUtilsTest {

    @Nested
    @DisplayName("isEmpty / isNotEmpty 方法测试")
    class IsEmptyTests {
        @Test
        @DisplayName("null字符串应返回true")
        void testNullString() {
            assertThat(StrUtils.isEmpty(null)).isTrue();
        }

        @Test
        @DisplayName("空字符串应返回true")
        void testEmptyString() {
            assertThat(StrUtils.isEmpty("")).isTrue();
        }

        @Test
        @DisplayName("仅空格字符串应返回true")
        void testBlankString() {
            assertThat(StrUtils.isEmpty("   ")).isTrue();
        }

        @Test
        @DisplayName("正常字符串应返回false")
        void testNormalString() {
            assertThat(StrUtils.isEmpty("hello")).isFalse();
        }

        @Test
        @DisplayName("isNotEmpty方法测试")
        void testIsNotEmpty() {
            assertThat(StrUtils.isNotEmpty("test")).isTrue();
            assertThat(StrUtils.isNotEmpty(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("isBlank / isNotBlank 方法测试")
    class IsBlankTests {
        @Test
        @DisplayName("null字符串应返回true")
        void testNullString() {
            assertThat(StrUtils.isBlank(null)).isTrue();
        }

        @Test
        @DisplayName("空字符串应返回true")
        void testEmptyString() {
            assertThat(StrUtils.isBlank("")).isTrue();
        }

        @Test
        @DisplayName("仅空格字符串应返回true")
        void testOnlySpaces() {
            assertThat(StrUtils.isBlank("   ")).isTrue();
        }

        @Test
        @DisplayName("正常字符串应返回false")
        void testNormalString() {
            assertThat(StrUtils.isBlank("hello")).isFalse();
        }

        @Test
        @DisplayName("isNotBlank方法测试")
        void testIsNotBlank() {
            assertThat(StrUtils.isNotBlank("test")).isTrue();
            assertThat(StrUtils.isNotBlank(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("defaultIfEmpty / defaultIfBlank 方法测试")
    class DefaultValueTests {
        @Test
        @DisplayName("null时返回默认值")
        void testNullWithDefault() {
            assertThat(StrUtils.defaultIfEmpty(null, "default")).isEqualTo("default");
        }

        @Test
        @DisplayName("空字符串时返回默认值")
        void testEmptyWithDefault() {
            assertThat(StrUtils.defaultIfEmpty("", "default")).isEqualTo("default");
        }

        @Test
        @DisplayName("正常值时返回原值")
        void testNormalValue() {
            assertThat(StrUtils.defaultIfEmpty("value", "default")).isEqualTo("value");
        }

        @Test
        @DisplayName("defaultIfBlank测试-空格字符串")
        void testBlankWithDefault() {
            assertThat(StrUtils.defaultIfBlank("   ", "default")).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("脱敏方法测试")
    class MaskTests {
        @Test
        @DisplayName("手机号脱敏-正常")
        void testMaskPhoneNormal() {
            assertThat(StrUtils.maskPhone("13812345678")).isEqualTo("138****5678");
        }

        @Test
        @DisplayName("手机号脱敏-短号码返回原值")
        void testMaskPhoneShort() {
            assertThat(StrUtils.maskPhone("138")).isEqualTo("138");
        }

        @Test
        @DisplayName("手机号脱敏-null返回null")
        void testMaskPhoneNull() {
            assertThat(StrUtils.maskPhone(null)).isNull();
        }

        @Test
        @DisplayName("身份证号脱敏-正常")
        void testMaskIdCardNormal() {
            assertThat(StrUtils.maskIdCard("110101199001011234"))
                    .isEqualTo("110101**********1234");
        }

        @Test
        @DisplayName("身份证号脱敏-短号码返回原值")
        void testMaskIdCardShort() {
            assertThat(StrUtils.maskIdCard("110101")).isEqualTo("110101");
        }

        @Test
        @DisplayName("邮箱脱敏-正常")
        void testMaskEmailNormal() {
            assertThat(StrUtils.maskEmail("test@example.com")).isEqualTo("t***@example.com");
        }

        @Test
        @DisplayName("邮箱脱敏-无@符号返回原值")
        void testMaskEmailNoAt() {
            assertThat(StrUtils.maskEmail("testexample.com")).isEqualTo("testexample.com");
        }

        @Test
        @DisplayName("邮箱脱敏-单字符用户名返回原值")
        void testMaskEmailSingleChar() {
            assertThat(StrUtils.maskEmail("t@example.com")).isEqualTo("t@example.com");
        }

        @Test
        @DisplayName("银行卡脱敏-正常")
        void testMaskBankCardNormal() {
            assertThat(StrUtils.maskBankCard("6222021234567890123"))
                    .isEqualTo("**** **** **** 0123");
        }

        @Test
        @DisplayName("银行卡脱敏-短卡号返回原值")
        void testMaskBankCardShort() {
            assertThat(StrUtils.maskBankCard("123456")).isEqualTo("123456");
        }
    }

    @Nested
    @DisplayName("驼峰下划线转换测试")
    class CamelUnderscoreTests {
        @Test
        @DisplayName("驼峰转下划线-正常")
        void testCamelToUnderscoreNormal() {
            assertThat(StrUtils.camelToUnderscore("userName")).isEqualTo("user_name");
        }

        @Test
        @DisplayName("驼峰转下划线-多个大写")
        void testCamelToUnderscoreMultiple() {
            // 每个大写字母前都会加下划线：XMLParser -> x_m_l_parser
            assertThat(StrUtils.camelToUnderscore("XMLParser")).isEqualTo("x_m_l_parser");
        }

        @Test
        @DisplayName("下划线转驼峰-正常")
        void testUnderscoreToCamelNormal() {
            assertThat(StrUtils.underscoreToCamel("user_name")).isEqualTo("userName");
        }

        @Test
        @DisplayName("下划线转驼峰-多个下划线")
        void testUnderscoreToCamelMultiple() {
            assertThat(StrUtils.underscoreToCamel("user_name_test")).isEqualTo("userNameTest");
        }
    }

    @Nested
    @DisplayName("格式化方法测试")
    class FormatTests {
        @Test
        @DisplayName("首字母大写")
        void testCapitalize() {
            assertThat(StrUtils.capitalize("hello")).isEqualTo("Hello");
        }

        @Test
        @DisplayName("首字母小写")
        void testUncapitalize() {
            assertThat(StrUtils.uncapitalize("Hello")).isEqualTo("hello");
        }

        @Test
        @DisplayName("截断字符串-无需截断")
        void testTruncateNoNeed() {
            assertThat(StrUtils.truncate("hello", 10)).isEqualTo("hello");
        }

        @Test
        @DisplayName("截断字符串-需要截断")
        void testTruncateNeed() {
            assertThat(StrUtils.truncate("hello world", 5)).isEqualTo("hello...");
        }

        @Test
        @DisplayName("文件大小格式化-字节")
        void testFormatSizeBytes() {
            assertThat(StrUtils.formatSize(512)).isEqualTo("512 B");
        }

        @Test
        @DisplayName("文件大小格式化-千字节")
        void testFormatSizeKB() {
            assertThat(StrUtils.formatSize(1536)).isEqualTo("1.50 KB");
        }

        @Test
        @DisplayName("文件大小格式化-兆字节")
        void testFormatSizeMB() {
            assertThat(StrUtils.formatSize(1024 * 1024 * 5)).isEqualTo("5.00 MB");
        }

        @Test
        @DisplayName("文件大小格式化-吉字节")
        void testFormatSizeGB() {
            assertThat(StrUtils.formatSize(1024L * 1024 * 1024 * 2)).isEqualTo("2.00 GB");
        }

        @Test
        @DisplayName("时长格式化-毫秒")
        void testFormatDurationMs() {
            assertThat(StrUtils.formatDuration(500)).isEqualTo("500 ms");
        }

        @Test
        @DisplayName("时长格式化-秒")
        void testFormatDurationSec() {
            assertThat(StrUtils.formatDuration(2500)).isEqualTo("2.50 s");
        }

        @Test
        @DisplayName("时长格式化-分钟")
        void testFormatDurationMin() {
            assertThat(StrUtils.formatDuration(125000)).isEqualTo("2.08 min");
        }
    }
}
