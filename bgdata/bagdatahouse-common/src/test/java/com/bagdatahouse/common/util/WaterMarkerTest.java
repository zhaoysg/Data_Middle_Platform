package com.bagdatahouse.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("数据水印工具类测试")
class WaterMarkerTest {

    private final WaterMarker waterMarker = new WaterMarker();

    @Nested
    @DisplayName("generateWatermark 方法测试")
    class GenerateWatermarkTests {

        @Test
        @DisplayName("生成水印应为16位十六进制字符串")
        void testGenerateWatermarkLength() {
            String watermark = waterMarker.generateWatermark(1L, 100L);
            assertThat(watermark)
                    .hasSize(16)
                    .matches("^[0-9a-f]{16}$");
        }

        @Test
        @DisplayName("相同输入在极短时间内生成相同水印")
        void testGenerateWatermarkDeterministic() {
            String watermark1 = waterMarker.generateWatermark(1L, 100L);
            String watermark2 = waterMarker.generateWatermark(1L, 100L);
            assertThat(watermark1).isEqualTo(watermark2);
        }

        @Test
        @DisplayName("不同用户ID应生成不同水印")
        void testGenerateWatermarkDifferentUserId() {
            String watermark1 = waterMarker.generateWatermark(1L, 100L);
            String watermark2 = waterMarker.generateWatermark(2L, 100L);
            assertThat(watermark1).isNotEqualTo(watermark2);
        }

        @Test
        @DisplayName("不同记录ID应生成不同水印")
        void testGenerateWatermarkDifferentRecordId() {
            String watermark1 = waterMarker.generateWatermark(1L, 100L);
            String watermark2 = waterMarker.generateWatermark(1L, 200L);
            assertThat(watermark1).isNotEqualTo(watermark2);
        }

        @Test
        @DisplayName("水印应为小写十六进制")
        void testGenerateWatermarkLowercaseHex() {
            String watermark = waterMarker.generateWatermark(999L, 999L);
            assertThat(watermark).isEqualTo(watermark.toLowerCase());
        }
    }

    @Nested
    @DisplayName("embedWatermark 方法测试")
    class EmbedWatermarkTests {

        @Test
        @DisplayName("null文本输入应返回null")
        void testEmbedWatermarkNullValue() {
            String result = waterMarker.embedWatermark(null, "abc123");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null水印输入应返回原始文本")
        void testEmbedWatermarkNullWatermark() {
            String result = waterMarker.embedWatermark("原始文本", null);
            assertThat(result).isEqualTo("原始文本");
        }

        @Test
        @DisplayName("嵌入水印后文本应包含零宽字符")
        void testEmbedWatermarkContainsZWJ() {
            String original = "测试文本";
            String watermark = "abc123";
            String result = waterMarker.embedWatermark(original, watermark);
            assertThat(result).contains("\u200B");
        }

        @Test
        @DisplayName("嵌入水印后原始文本应保留")
        void testEmbedWatermarkPreservesOriginal() {
            String original = "原始文本内容";
            String watermark = "def456";
            String result = waterMarker.embedWatermark(original, watermark);
            assertThat(result).startsWith(original);
        }

        @Test
        @DisplayName("空字符串输入应返回空字符串")
        void testEmbedWatermarkEmptyValue() {
            String result = waterMarker.embedWatermark("", "abc123");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("空水印输入应返回原始文本")
        void testEmbedWatermarkEmptyWatermark() {
            String result = waterMarker.embedWatermark("测试", "");
            assertThat(result).isEqualTo("测试");
        }
    }

    @Nested
    @DisplayName("extractWatermark 方法测试")
    class ExtractWatermarkTests {

        @Test
        @DisplayName("从含水印文本提取水印应成功")
        void testExtractWatermarkFromEmbedded() {
            String original = "敏感数据报表";
            String watermark = "a1b2c3d4";
            String embedded = waterMarker.embedWatermark(original, watermark);

            WaterMarker.WatermarkInfo info = waterMarker.extractWatermark(embedded);

            assertThat(info).isNotNull();
            assertThat(info.getRawWatermark()).isEqualTo(watermark);
        }

        @Test
        @DisplayName("从普通文本提取水印应返回null")
        void testExtractWatermarkFromPlainText() {
            WaterMarker.WatermarkInfo info = waterMarker.extractWatermark("普通文本没有水印");
            assertThat(info).isNull();
        }

        @Test
        @DisplayName("null输入应返回null")
        void testExtractWatermarkNull() {
            WaterMarker.WatermarkInfo info = waterMarker.extractWatermark(null);
            assertThat(info).isNull();
        }

        @Test
        @DisplayName("仅含零宽字符应返回null")
        void testExtractWatermarkOnlyZWJ() {
            WaterMarker.WatermarkInfo info = waterMarker.extractWatermark("\u200B\u200B\u200B");
            assertThat(info).isNull();
        }
    }

    @Nested
    @DisplayName("hasWatermark 方法测试")
    class HasWatermarkTests {

        @Test
        @DisplayName("含水印文本应返回true")
        void testHasWatermarkTrue() {
            String embedded = waterMarker.embedWatermark("数据", "test123");
            assertThat(waterMarker.hasWatermark(embedded)).isTrue();
        }

        @Test
        @DisplayName("普通文本应返回false")
        void testHasWatermarkFalse() {
            assertThat(waterMarker.hasWatermark("普通文本")).isFalse();
        }

        @Test
        @DisplayName("null输入应返回false")
        void testHasWatermarkNull() {
            assertThat(waterMarker.hasWatermark(null)).isFalse();
        }

        @Test
        @DisplayName("空字符串应返回false")
        void testHasWatermarkEmpty() {
            assertThat(waterMarker.hasWatermark("")).isFalse();
        }
    }

    @Nested
    @DisplayName("traceLeak 方法测试")
    class TraceLeakTests {

        @Test
        @DisplayName("追踪含水印泄露文本应返回LeakSource")
        void testTraceLeakWithWatermark() {
            String embedded = waterMarker.embedWatermark("泄露数据", "leak001");
            WaterMarker.LeakSource source = waterMarker.traceLeak(embedded);

            assertThat(source).isNotNull();
            assertThat(source.getMatchedWatermark()).isEqualTo("leak001");
            assertThat(source.getConfidence()).isEqualTo("HIGH");
            assertThat(source.getLeakTime()).isNotNull();
        }

        @Test
        @DisplayName("追踪无水印文本应返回null")
        void testTraceLeakWithoutWatermark() {
            WaterMarker.LeakSource source = waterMarker.traceLeak("普通文本");
            assertThat(source).isNull();
        }

        @Test
        @DisplayName("追踪null应返回null")
        void testTraceLeakNull() {
            WaterMarker.LeakSource source = waterMarker.traceLeak(null);
            assertThat(source).isNull();
        }
    }

    @Nested
    @DisplayName("水印完整流程测试")
    class RoundtripTests {

        @Test
        @DisplayName("水印生成→嵌入→提取应保持一致")
        void testWatermarkRoundtrip() {
            Long userId = 12345L;
            Long recordId = 67890L;
            String original = "这是一份包含敏感信息的报告";

            String watermark = waterMarker.generateWatermark(userId, recordId);
            String embedded = waterMarker.embedWatermark(original, watermark);
            WaterMarker.WatermarkInfo extracted = waterMarker.extractWatermark(embedded);

            assertThat(extracted).isNotNull();
            assertThat(extracted.getRawWatermark()).isEqualTo(watermark);
        }

        @Test
        @DisplayName("水印嵌入不影响原始文本可读性")
        void testWatermarkDoesNotAffectReadability() {
            String original = "重要数据需要保护";
            String watermark = "protected";
            String embedded = waterMarker.embedWatermark(original, watermark);

            assertThat(embedded).startsWith(original);
            assertThat(embedded).contains(original);
            assertThat(embedded.length()).isGreaterThan(original.length());
        }

        @Test
        @DisplayName("多次嵌入同一水印应都能正确提取")
        void testMultipleEmbedSameWatermark() {
            String text1 = "第一份文档";
            String text2 = "第二份文档";
            String watermark = "shared001";

            String embedded1 = waterMarker.embedWatermark(text1, watermark);
            String embedded2 = waterMarker.embedWatermark(text2, watermark);

            WaterMarker.WatermarkInfo info1 = waterMarker.extractWatermark(embedded1);
            WaterMarker.WatermarkInfo info2 = waterMarker.extractWatermark(embedded2);

            assertThat(info1.getRawWatermark()).isEqualTo(watermark);
            assertThat(info2.getRawWatermark()).isEqualTo(watermark);
        }
    }
}
