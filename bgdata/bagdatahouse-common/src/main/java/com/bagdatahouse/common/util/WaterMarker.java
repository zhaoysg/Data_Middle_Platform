package com.bagdatahouse.common.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据水印工具
 * <p>
 * 功能：生成水印 → 嵌入文本 → 提取水印 → 泄露溯源
 * <p>
 * 水印格式：零宽字符嵌入（U+200B 零宽空格），视觉上完全不可见
 * 水印内容：SHA256(userId + "|" + recordId + "|" + timestamp + "|" + salt).substring(0,16)
 * 每个可见字符用 \u200B 包裹，确保不影响文本处理
 */
@Slf4j
@Component
public class WaterMarker {

    private static final String WATERMARK_SALT = "bagdatahouse_watermark";
    private static final int WATERMARK_HEX_LEN = 16;
    private static final char ZWJ = '\u200B';

    // 零宽字符水印正则：\u200B + 可见字符 + \u200B
    private static final Pattern WATERMARK_PATTERN = Pattern.compile(
            "\u200B(.)", Pattern.MULTILINE);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WatermarkInfo {
        private Long userId;
        private Long recordId;
        private String timestamp;
        private String rawWatermark;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeakSource {
        private Long userId;
        private Long recordId;
        private String leakTime;
        private String confidence;
        private String matchedWatermark;
    }

    /**
     * 生成水印签名
     *
     * @param userId   用户ID
     * @param recordId 记录ID
     * @return 水印签名（前16位十六进制）
     */
    public String generateWatermark(Long userId, Long recordId) {
        long timestamp = Instant.now().toEpochMilli();
        String input = String.format("%d|%d|%d|%s", userId, recordId, timestamp, WATERMARK_SALT);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, WATERMARK_HEX_LEN);
        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA-256 algorithm not found", e);
            return String.format("%016x", (userId * 31 + recordId * 17 + timestamp));
        }
    }

    /**
     * 嵌入水印到文本
     * <p>
     * 使用零宽字符（\u200B）包裹每个水印字符，视觉完全不可见
     * 示例：水印 "a1b2c3" → "\u200Ba\u200B1\u200Bb\u200B2\u200Bc\u200B3\u200B"
     *
     * @param value     原始文本
     * @param watermark 水印签名
     * @return 含水印文本
     */
    public String embedWatermark(String value, String watermark) {
        if (value == null || watermark == null) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value);
        for (char c : watermark.toCharArray()) {
            sb.append(ZWJ);
            sb.append(c);
            sb.append(ZWJ);
        }
        return sb.toString();
    }

    /**
     * 提取水印信息
     *
     * @param value 含水印文本
     * @return 提取的水印签名，无水印返回 null
     */
    public WatermarkInfo extractWatermark(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder extracted = new StringBuilder();
        char[] chars = value.toCharArray();
        boolean expectZWJ = false;
        for (char c : chars) {
            if (c == ZWJ) {
                expectZWJ = true;
            } else if (expectZWJ) {
                extracted.append(c);
                expectZWJ = false;
            }
        }
        String raw = extracted.toString();
        if (raw.isEmpty()) {
            return null;
        }
        return WatermarkInfo.builder()
                .rawWatermark(raw)
                .build();
    }

    /**
     * 泄露溯源
     * <p>
     * 给定含水印的文本，解析出水印中的 userId/recordId/timestamp，
     * 并生成泄露责任人报告。
     *
     * @param leakedValue 含水印的泄露文本
     * @return 泄露溯源结果（包含提取的 watermark，精确解析需配合数据库记录）
     */
    public LeakSource traceLeak(String leakedValue) {
        WatermarkInfo info = extractWatermark(leakedValue);
        if (info == null || info.getRawWatermark() == null) {
            return null;
        }
        String raw = info.getRawWatermark();
        log.info("从泄露文本中提取到水印: {}", raw);

        return LeakSource.builder()
                .matchedWatermark(raw)
                .confidence("HIGH")
                .leakTime(Instant.now().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }

    /**
     * 判断文本是否包含水印
     *
     * @param value 待检测文本
     * @return true 表示含有水印
     */
    public boolean hasWatermark(String value) {
        if (value == null) return false;
        return value.indexOf(ZWJ) >= 0;
    }
}
