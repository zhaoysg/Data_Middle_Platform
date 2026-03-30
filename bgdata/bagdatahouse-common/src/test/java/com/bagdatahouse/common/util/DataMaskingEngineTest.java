package com.bagdatahouse.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("数据脱敏引擎测试")
class DataMaskingEngineTest {

    private final DataMaskingEngine engine = new DataMaskingEngine();

    // ==================== applyMask - MASK 类型 ====================

    @Nested
    @DisplayName("applyMask - MASK 遮蔽类型")
    class MaskTypeTests {

        @Test
        @DisplayName("手机号遮蔽-保留首3尾4位")
        void testMaskPhoneKeepHead3Tail4() {
            String result = engine.applyMask("13812345678", "MASK", "{\"keepHead\":3,\"keepTail\":4}");
            assertThat(result).isEqualTo("138****5678");
        }

        @Test
        @DisplayName("手机号遮蔽-默认参数(保留首1位)")
        void testMaskPhoneDefault() {
            String result = engine.applyMask("13812345678", "MASK", null);
            assertThat(result).isEqualTo("1**********");
        }

        @Test
        @DisplayName("手机号遮蔽-保留首2尾3位")
        void testMaskPhoneKeepHead2Tail3() {
            String result = engine.applyMask("13812345678", "MASK", "{\"keepHead\":2,\"keepTail\":3}");
            assertThat(result).isEqualTo("13******678");
        }

        @Test
        @DisplayName("手机号遮蔽-自定义脱敏字符")
        void testMaskPhoneCustomMaskChar() {
            String result = engine.applyMask("13812345678", "MASK", "{\"keepHead\":3,\"keepTail\":4,\"maskChar\":\"#\"}");
            assertThat(result).isEqualTo("138####5678");
        }

        @Test
        @DisplayName("手机号遮蔽-字符串过短时全部替换")
        void testMaskPhoneTooShort() {
            String result = engine.applyMask("138", "MASK", "{\"keepHead\":3,\"keepTail\":4}");
            assertThat(result).isEqualTo("***");
        }

        @Test
        @DisplayName("MASK类型大小写不敏感")
        void testMaskTypeCaseInsensitive() {
            assertThat(engine.applyMask("13812345678", "mask", "{\"keepHead\":3,\"keepTail\":4}"))
                    .isEqualTo("138****5678");
            assertThat(engine.applyMask("13812345678", "Mask", "{\"keepHead\":3,\"keepTail\":4}"))
                    .isEqualTo("138****5678");
        }
    }

    // ==================== applyMask - HIDE 类型 ====================

    @Nested
    @DisplayName("applyMask - HIDE 隐藏类型")
    class HideTypeTests {

        @Test
        @DisplayName("HIDE类型-全部替换为脱敏字符")
        void testHideAllReplaced() {
            String result = engine.applyMask("13812345678", "HIDE", null);
            assertThat(result).isEqualTo("***********");
        }

        @Test
        @DisplayName("HIDE类型-自定义脱敏字符")
        void testHideCustomMaskChar() {
            String result = engine.applyMask("13812345678", "HIDE", "{\"maskChar\":\"#\"}");
            assertThat(result).hasSize(11);
            assertThat(result).matches("^#+$");
        }

        @Test
        @DisplayName("HIDE类型-保留末尾可见字符")
        void testHideKeepVisible() {
            String result = engine.applyMask("13812345678", "HIDE", "{\"keepVisible\":4}");
            assertThat(result).hasSize(7);
            assertThat(result).matches("^\\*+$");
        }
    }

    // ==================== applyMask - DELETE 类型 ====================

    @Nested
    @DisplayName("applyMask - DELETE 删除类型")
    class DeleteTypeTests {

        @Test
        @DisplayName("DELETE类型-返回空字符串")
        void testDeleteReturnsEmpty() {
            String result = engine.applyMask("13812345678", "DELETE", null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("DELETE类型-空字符串仍返回空字符串")
        void testDeleteEmptyString() {
            String result = engine.applyMask("", "DELETE", null);
            assertThat(result).isEmpty();
        }
    }

    // ==================== applyMask - NONE 类型 ====================

    @Nested
    @DisplayName("applyMask - NONE 不脱敏类型")
    class NoneTypeTests {

        @Test
        @DisplayName("NONE类型-返回原值")
        void testNoneReturnsOriginal() {
            String result = engine.applyMask("13812345678", "NONE", null);
            assertThat(result).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("NONE大写-返回原值")
        void testNoneUpperCase() {
            String result = engine.applyMask("test@example.com", "NONE", null);
            assertThat(result).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("空字符串NONE-返回空字符串")
        void testNoneEmptyString() {
            String result = engine.applyMask("", "NONE", null);
            assertThat(result).isEmpty();
        }
    }

    // ==================== applyMask - null 和空值处理 ====================

    @Nested
    @DisplayName("applyMask - null 和边界值处理")
    class NullAndEmptyTests {

        @Test
        @DisplayName("null输入-返回null")
        void testNullInput() {
            assertThat(engine.applyMask(null, "MASK", null)).isNull();
            assertThat(engine.applyMask(null, "HIDE", null)).isNull();
            assertThat(engine.applyMask(null, "DELETE", null)).isNull();
            assertThat(engine.applyMask(null, "NONE", null)).isNull();
        }

        @Test
        @DisplayName("null类型-视为NONE返回原值")
        void testNullType() {
            String result = engine.applyMask("13812345678", null, null);
            assertThat(result).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("空字符串类型-视为NONE返回原值")
        void testBlankType() {
            String result = engine.applyMask("13812345678", "", null);
            assertThat(result).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("空格类型-视为NONE返回原值")
        void testWhitespaceType() {
            String result = engine.applyMask("13812345678", "   ", null);
            assertThat(result).isEqualTo("13812345678");
        }
    }

    // ==================== applyMask - 未知类型 ====================

    @Nested
    @DisplayName("applyMask - 未知类型处理")
    class UnknownTypeTests {

        @Test
        @DisplayName("未知类型-返回原值")
        void testUnknownType() {
            String result = engine.applyMask("13812345678", "UNKNOWN_TYPE", null);
            assertThat(result).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("未知类型-忽略maskPattern")
        void testUnknownTypeIgnoresPattern() {
            String result = engine.applyMask("13812345678", "UNKNOWN", "{\"keepHead\":3}");
            assertThat(result).isEqualTo("13812345678");
        }
    }

    // ==================== applyMask - ENCRYPT 类型 ====================

    @Nested
    @DisplayName("applyMask - ENCRYPT 加密类型")
    class EncryptTypeTests {

        @Test
        @DisplayName("ENCRYPT类型-返回SHA-256 AES加密结果")
        void testEncryptReturnsHexString() {
            String result = engine.applyMask("13812345678", "ENCRYPT", null);
            assertThat(result).isNotNull();
            assertThat(result).hasSize(32); // AES/ECB/PKCS5Padding output is 16 bytes = 32 hex chars
            assertThat(result).matches("^[0-9a-f]{32}$");
        }

        @Test
        @DisplayName("ENCRYPT类型-相同输入相同密钥产生相同输出")
        void testEncryptDeterministic() {
            String result1 = engine.applyMask("test", "ENCRYPT", "{\"key\":\"test-key\"}");
            String result2 = engine.applyMask("test", "ENCRYPT", "{\"key\":\"test-key\"}");
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        @DisplayName("ENCRYPT类型-不同密钥产生不同输出")
        void testEncryptDifferentKeys() {
            String result1 = engine.applyMask("test", "ENCRYPT", "{\"key\":\"key1\"}");
            String result2 = engine.applyMask("test", "ENCRYPT", "{\"key\":\"key2\"}");
            assertThat(result1).isNotEqualTo(result2);
        }
    }

    // ==================== applyMask - HASH 类型 ====================

    @Nested
    @DisplayName("applyMask - HASH 哈希类型")
    class HashTypeTests {

        @Test
        @DisplayName("HASH类型-默认SHA256产生64位十六进制字符串")
        void testHashDefaultSha256() {
            String result = engine.applyMask("13812345678", "HASH", null);
            assertThat(result).isNotNull();
            assertThat(result).hasSize(64); // SHA256 = 256 bits = 64 hex chars
            assertThat(result).matches("^[0-9a-f]{64}$");
        }

        @Test
        @DisplayName("HASH类型-相同输入相同盐值产生相同输出")
        void testHashDeterministic() {
            String result1 = engine.applyMask("test", "HASH", "{\"salt\":\"mysalt\"}");
            String result2 = engine.applyMask("test", "HASH", "{\"salt\":\"mysalt\"}");
            assertThat(result1).isEqualTo(result2);
        }

        @Test
        @DisplayName("HASH类型-不同盐值产生不同输出")
        void testHashDifferentSalts() {
            String result1 = engine.applyMask("test", "HASH", "{\"salt\":\"salt1\"}");
            String result2 = engine.applyMask("test", "HASH", "{\"salt\":\"salt2\"}");
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("HASH类型-MD5算法产生32位十六进制字符串")
        void testHashMd5() {
            String result = engine.applyMask("test", "HASH", "{\"algorithm\":\"MD5\",\"salt\":\"salt\"}");
            assertThat(result).hasSize(32);
            assertThat(result).matches("^[0-9a-f]{32}$");
        }

        @Test
        @DisplayName("HASH类型-不支持的算法fallback到SHA256")
        void testHashUnknownAlgorithmFallsBack() {
            String result = engine.applyMask("test", "HASH", "{\"algorithm\":\"UNKNOWN\"}");
            assertThat(result).hasSize(64);
        }
    }

    // ==================== applyMask - RANDOM_REPLACE 类型 ====================

    @Nested
    @DisplayName("applyMask - RANDOM_REPLACE 随机替换类型")
    class RandomReplaceTypeTests {

        @Test
        @DisplayName("RANDOM_REPLACE类型-保持长度不变")
        void testRandomReplacePreservesLength() {
            String result = engine.applyMask("13812345678", "RANDOM_REPLACE", null);
            assertThat(result).hasSize(11);
        }

        @Test
        @DisplayName("RANDOM_REPLACE类型-保持数字格式")
        void testRandomReplaceKeepsDigits() {
            String result = engine.applyMask("13812345678", "RANDOM_REPLACE", null);
            assertThat(result).matches("^\\d{11}$");
        }

        @Test
        @DisplayName("RANDOM_REPLACE类型-字母转为小写字母")
        void testRandomReplaceLetters() {
            String result = engine.applyMask("abcdefgh", "RANDOM_REPLACE", null);
            assertThat(result).hasSize(8);
            assertThat(result).matches("^[a-z]{8}$");
        }

        @Test
        @DisplayName("RANDOM_REPLACE类型-混合字符串保持格式")
        void testRandomReplaceMixed() {
            String result = engine.applyMask("abc123", "RANDOM_REPLACE", null);
            assertThat(result).hasSize(6);
            // First 3 chars should be letters, last 3 should be digits
            assertThat(result.substring(0, 3)).matches("^[a-z]{3}$");
            assertThat(result.substring(3)).matches("^\\d{3}$");
        }
    }

    // ==================== applyMask - RANGE 类型 ====================

    @Nested
    @DisplayName("applyMask - RANGE 区间化类型")
    class RangeTypeTests {

        @Test
        @DisplayName("RANGE类型-数值落入区间范围")
        void testRangeNumeric() {
            String result = engine.applyMask("2500", "RANGE", "{\"rangeMin\":0,\"rangeMax\":10000,\"step\":1000}");
            assertThat(result).isEqualTo("2000-3000");
        }

        @Test
        @DisplayName("RANGE类型-正好是区间边界")
        void testRangeAtBoundary() {
            String result = engine.applyMask("3000", "RANGE", "{\"rangeMin\":0,\"rangeMax\":10000,\"step\":1000}");
            assertThat(result).isEqualTo("3000-4000");
        }

        @Test
        @DisplayName("RANGE类型-超出最大范围")
        void testRangeExceedsMax() {
            String result = engine.applyMask("15000", "RANGE", "{\"rangeMin\":0,\"rangeMax\":10000,\"step\":1000}");
            assertThat(result).isEqualTo("15000-10000");
        }

        @Test
        @DisplayName("RANGE类型-小于最小范围")
        void testRangeBelowMin() {
            String result = engine.applyMask("-100", "RANGE", "{\"rangeMin\":0,\"rangeMax\":10000,\"step\":1000}");
            assertThat(result).isEqualTo("0");
        }

        @Test
        @DisplayName("RANGE类型-非数字字符串fallback到mask")
        void testRangeNonNumeric() {
            String result = engine.applyMask("abc", "RANGE", "{\"rangeMin\":0,\"rangeMax\":10000}");
            assertThat(result).isNotEmpty();
        }
    }

    // ==================== applyMask - WATERMARK 类型 ====================

    @Nested
    @DisplayName("applyMask - WATERMARK 水印嵌入类型")
    class WatermarkTypeTests {

        @Test
        @DisplayName("WATERMARK类型-原始值前追加水印字符")
        void testWatermarkContainsZeroWidthChars() {
            String result = engine.applyMask("test", "WATERMARK", "{\"userId\":1,\"recordId\":100}");
            assertThat(result.length()).isGreaterThan(4);
            assertThat(result).contains("\u200B"); // Zero-width space
        }

        @Test
        @DisplayName("WATERMARK类型-包含零宽字符序列")
        void testWatermarkHasZeroWidthPattern() {
            String result = engine.applyMask("data", "WATERMARK", null);
            // Should contain zero-width spaces alternating with hex chars
            assertThat(result).containsSubsequence("\u200B");
            assertThat(result.length()).isGreaterThan(4);
        }

        @Test
        @DisplayName("WATERMARK类型-不同userId产生不同水印")
        void testWatermarkDifferentUserIds() {
            String result1 = engine.applyMask("data", "WATERMARK", "{\"userId\":1}");
            String result2 = engine.applyMask("data", "WATERMARK", "{\"userId\":2}");
            assertThat(result1).isNotEqualTo(result2);
        }
    }

    // ==================== applyMask - FORMAT_KEEP 类型 ====================

    @Nested
    @DisplayName("applyMask - FORMAT_KEEP 保留格式类型")
    class FormatKeepTypeTests {

        @Test
        @DisplayName("FORMAT_KEEP-三字姓名张三丰→张*丰")
        void testFormatKeepThreeCharName() {
            String result = engine.applyMask("张三丰", "FORMAT_KEEP", null);
            assertThat(result).isEqualTo("张*丰");
        }

        @Test
        @DisplayName("FORMAT_KEEP-两字姓名李四→**")
        void testFormatKeepTwoCharName() {
            String result = engine.applyMask("李四", "FORMAT_KEEP", null);
            assertThat(result).isEqualTo("**");
        }

        @Test
        @DisplayName("FORMAT_KEEP-四字姓名王老五→王*五")
        void testFormatKeepFourCharName() {
            String result = engine.applyMask("王老五", "FORMAT_KEEP", null);
            assertThat(result).isEqualTo("王*五");
        }

        @Test
        @DisplayName("FORMAT_KEEP-自定义参数保留首尾")
        void testFormatKeepCustomParams() {
            String result = engine.applyMask("欧阳锋", "FORMAT_KEEP", "{\"keepHead\":2,\"keepTail\":1}");
            assertThat(result).isEqualTo("欧*");
        }

        @Test
        @DisplayName("FORMAT_KEEP-短字符串全部替换")
        void testFormatKeepShortString() {
            String result = engine.applyMask("张", "FORMAT_KEEP", null);
            assertThat(result).isEqualTo("*");
        }
    }

    // ==================== isValidPhone 校验 ====================

    @Nested
    @DisplayName("isValidPhone 手机号校验")
    class PhoneValidationTests {

        @Test
        @DisplayName("正确手机号-138开头")
        void testValidPhone138() {
            assertThat(engine.isValidPhone("13812345678")).isTrue();
        }

        @Test
        @DisplayName("正确手机号-150开头")
        void testValidPhone150() {
            assertThat(engine.isValidPhone("15012345678")).isTrue();
        }

        @Test
        @DisplayName("正确手机号-199开头")
        void testValidPhone199() {
            assertThat(engine.isValidPhone("19912345678")).isTrue();
        }

        @Test
        @DisplayName("错误手机号-10开头")
        void testInvalidPhone10() {
            assertThat(engine.isValidPhone("10812345678")).isFalse();
        }

        @Test
        @DisplayName("错误手机号-位数不足")
        void testInvalidPhoneTooShort() {
            assertThat(engine.isValidPhone("138123456")).isFalse();
        }

        @Test
        @DisplayName("错误手机号-位数过多")
        void testInvalidPhoneTooLong() {
            assertThat(engine.isValidPhone("138123456789")).isFalse();
        }

        @Test
        @DisplayName("错误手机号-含字母")
        void testInvalidPhoneWithLetter() {
            assertThat(engine.isValidPhone("1381234567a")).isFalse();
        }

        @Test
        @DisplayName("null手机号-返回false")
        void testNullPhone() {
            assertThat(engine.isValidPhone(null)).isFalse();
        }

        @Test
        @DisplayName("空字符串-返回false")
        void testEmptyPhone() {
            assertThat(engine.isValidPhone("")).isFalse();
        }
    }

    // ==================== isValidIdCard 校验 ====================

    @Nested
    @DisplayName("isValidIdCard 身份证校验")
    class IdCardValidationTests {

        @Test
        @DisplayName("正确身份证-标准18位")
        void testValidIdCardStandard() {
            // 110105194912310022 通过 ISO 7064 MOD 11-2 校验
            assertThat(engine.isValidIdCard("110105194912310022")).isTrue();
        }

        @Test
        @DisplayName("正确身份证-最后一位为X")
        void testValidIdCardWithX() {
            // 110105199912310018 通过 ISO 7064 MOD 11-2 校验
            assertThat(engine.isValidIdCard("110105199912310018")).isTrue();
        }

        @Test
        @DisplayName("正确身份证-最后一位为小写x")
        void testValidIdCardWithLowerX() {
            assertThat(engine.isValidIdCard("11010519491231002x")).isTrue();
        }

        @Test
        @DisplayName("错误身份证-校验位错误")
        void testInvalidIdCardBadChecksum() {
            assertThat(engine.isValidIdCard("110101199001011235")).isFalse();
        }

        @Test
        @DisplayName("错误身份证-位数不足")
        void testInvalidIdCardTooShort() {
            assertThat(engine.isValidIdCard("11010119900101123")).isFalse();
        }

        @Test
        @DisplayName("错误身份证-位数过多")
        void testInvalidIdCardTooLong() {
            assertThat(engine.isValidIdCard("1101011990010112345")).isFalse();
        }

        @Test
        @DisplayName("错误身份证-前17位含字母")
        void testInvalidIdCardLetterInFirst17() {
            assertThat(engine.isValidIdCard("11010119900101A234")).isFalse();
        }

        @Test
        @DisplayName("null身份证-返回false")
        void testNullIdCard() {
            assertThat(engine.isValidIdCard(null)).isFalse();
        }
    }

    // ==================== isValidBankCard 校验 ====================

    @Nested
    @DisplayName("isValidBankCard 银行卡校验")
    class BankCardValidationTests {

        @Test
        @DisplayName("正确银行卡-标准16位")
        void testValidBankCard16() {
            // 4916338506082837 通过Luhn校验
            assertThat(engine.isValidBankCard("4916338506082837")).isTrue();
        }

        @Test
        @DisplayName("正确银行卡-带空格格式")
        void testValidBankCardWithSpaces() {
            assertThat(engine.isValidBankCard("4916 3385 0608 2837")).isTrue();
        }

        @Test
        @DisplayName("正确银行卡-带横杠格式")
        void testValidBankCardWithDashes() {
            assertThat(engine.isValidBankCard("4916-3385-0608-2837")).isTrue();
        }

        @Test
        @DisplayName("错误银行卡-Luhn校验失败")
        void testInvalidBankCardBadLuhn() {
            assertThat(engine.isValidBankCard("6222021234567891")).isFalse();
        }

        @Test
        @DisplayName("错误银行卡-位数过少")
        void testInvalidBankCardTooShort() {
            assertThat(engine.isValidBankCard("622202123456")).isFalse();
        }

        @Test
        @DisplayName("错误银行卡-含字母")
        void testInvalidBankCardWithLetter() {
            assertThat(engine.isValidBankCard("622202123456789a")).isFalse();
        }

        @Test
        @DisplayName("null银行卡-返回false")
        void testNullBankCard() {
            assertThat(engine.isValidBankCard(null)).isFalse();
        }
    }

    // ==================== isValidEmail 校验 ====================

    @Nested
    @DisplayName("isValidEmail 邮箱校验")
    class EmailValidationTests {

        @Test
        @DisplayName("正确邮箱-标准格式")
        void testValidEmailStandard() {
            assertThat(engine.isValidEmail("test@example.com")).isTrue();
        }

        @Test
        @DisplayName("正确邮箱-含数字")
        void testValidEmailWithNumbers() {
            assertThat(engine.isValidEmail("user123@example.com")).isTrue();
        }

        @Test
        @DisplayName("正确邮箱-含加号")
        void testValidEmailWithPlus() {
            assertThat(engine.isValidEmail("user+tag@example.com")).isTrue();
        }

        @Test
        @DisplayName("正确邮箱-子域名")
        void testValidEmailSubdomain() {
            assertThat(engine.isValidEmail("user@mail.example.com")).isTrue();
        }

        @Test
        @DisplayName("错误邮箱-无@符号")
        void testInvalidEmailNoAt() {
            assertThat(engine.isValidEmail("testexample.com")).isFalse();
        }

        @Test
        @DisplayName("错误邮箱-无域名")
        void testInvalidEmailNoDomain() {
            assertThat(engine.isValidEmail("test@")).isFalse();
        }

        @Test
        @DisplayName("错误邮箱-无用户名")
        void testInvalidEmailNoUser() {
            assertThat(engine.isValidEmail("@example.com")).isFalse();
        }

        @Test
        @DisplayName("错误邮箱-顶级域名过短")
        void testInvalidEmailShortTld() {
            assertThat(engine.isValidEmail("test@example.c")).isFalse();
        }

        @Test
        @DisplayName("正确邮箱-带前后空格")
        void testValidEmailWithSpaces() {
            assertThat(engine.isValidEmail("  test@example.com  ")).isTrue();
        }

        @Test
        @DisplayName("null邮箱-返回false")
        void testNullEmail() {
            assertThat(engine.isValidEmail(null)).isFalse();
        }
    }

    // ==================== isValidIpv4 校验 ====================

    @Nested
    @DisplayName("isValidIpv4 IP地址校验")
    class Ipv4ValidationTests {

        @Test
        @DisplayName("正确IP-标准格式")
        void testValidIpv4Standard() {
            assertThat(engine.isValidIpv4("192.168.1.1")).isTrue();
        }

        @Test
        @DisplayName("正确IP-边界值0")
        void testValidIpv4Zero() {
            assertThat(engine.isValidIpv4("0.0.0.0")).isTrue();
        }

        @Test
        @DisplayName("正确IP-边界值255")
        void testValidIpv4Max() {
            assertThat(engine.isValidIpv4("255.255.255.255")).isTrue();
        }

        @Test
        @DisplayName("正确IP-127本地回环")
        void testValidIpv4Localhost() {
            assertThat(engine.isValidIpv4("127.0.0.1")).isTrue();
        }

        @Test
        @DisplayName("错误IP-单段超过255")
        void testInvalidIpv4SegmentOverflow() {
            assertThat(engine.isValidIpv4("192.168.1.256")).isFalse();
        }

        @Test
        @DisplayName("错误IP-位数不足")
        void testInvalidIpv4TooFewSegments() {
            assertThat(engine.isValidIpv4("192.168.1")).isFalse();
        }

        @Test
        @DisplayName("错误IP-含字母")
        void testInvalidIpv4WithLetter() {
            assertThat(engine.isValidIpv4("192.168.1.a")).isFalse();
        }

        @Test
        @DisplayName("错误IP-负数")
        void testInvalidIpv4Negative() {
            assertThat(engine.isValidIpv4("192.168.1.-1")).isFalse();
        }

        @Test
        @DisplayName("null IP-返回false")
        void testNullIpv4() {
            assertThat(engine.isValidIpv4(null)).isFalse();
        }

        @Test
        @DisplayName("正确IP-带前后空格")
        void testValidIpv4WithSpaces() {
            assertThat(engine.isValidIpv4("  192.168.1.1  ")).isTrue();
        }
    }

    // ==================== normalizePhone 标准化 ====================

    @Nested
    @DisplayName("normalizePhone 手机号标准化")
    class NormalizePhoneTests {

        @Test
        @DisplayName("normalizePhone-正常手机号保持不变")
        void testNormalizePhoneNormal() {
            assertThat(engine.normalizePhone("13812345678")).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("normalizePhone-移除空格")
        void testNormalizePhoneRemoveSpaces() {
            assertThat(engine.normalizePhone("138 1234 5678")).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("normalizePhone-移除横杠")
        void testNormalizePhoneRemoveDashes() {
            assertThat(engine.normalizePhone("138-1234-5678")).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("normalizePhone-移除混合分隔符")
        void testNormalizePhoneRemoveMixed() {
            assertThat(engine.normalizePhone("138-1234 5678")).isEqualTo("13812345678");
        }

        @Test
        @DisplayName("normalizePhone-无效手机号返回原值")
        void testNormalizePhoneInvalidReturnsOriginal() {
            assertThat(engine.normalizePhone("12345678901")).isEqualTo("12345678901");
        }

        @Test
        @DisplayName("normalizePhone-null返回null")
        void testNormalizePhoneNull() {
            assertThat(engine.normalizePhone(null)).isNull();
        }
    }

    // ==================== 集成场景测试 ====================

    @Nested
    @DisplayName("集成场景测试")
    class IntegrationTests {

        @Test
        @DisplayName("手机号全流程-校验→脱敏→验证")
        void testPhoneFullWorkflow() {
            String phone = "13812345678";
            assertThat(engine.isValidPhone(phone)).isTrue();
            String masked = engine.applyMask(phone, "MASK", "{\"keepHead\":3,\"keepTail\":4}");
            assertThat(masked).isEqualTo("138****5678");
        }

        @Test
        @DisplayName("身份证全流程-校验→脱敏→验证")
        void testIdCardFullWorkflow() {
            String idCard = "110101199001011234";
            assertThat(engine.isValidIdCard(idCard)).isTrue();
            String masked = engine.applyMask(idCard, "MASK", "{\"keepHead\":6,\"keepTail\":4}");
            assertThat(masked).isEqualTo("110101********1234");
        }

        @Test
        @DisplayName("邮箱全流程-校验→脱敏→验证")
        void testEmailFullWorkflow() {
            String email = "test@example.com";
            assertThat(engine.isValidEmail(email)).isTrue();
            String hashed = engine.applyMask(email, "HASH", "{\"salt\":\"emailsalt\"}");
            assertThat(hashed).hasSize(64);
        }

        @Test
        @DisplayName("姓名假名化-两字三字四字")
        void testChineseNameMasking() {
            assertThat(engine.applyMask("张三丰", "FORMAT_KEEP", null)).isEqualTo("张*丰");
            assertThat(engine.applyMask("李四", "FORMAT_KEEP", null)).isEqualTo("**");
            assertThat(engine.applyMask("王老五", "FORMAT_KEEP", null)).isEqualTo("王*五");
        }
    }
}
