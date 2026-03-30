package com.bagdatahouse.common.serializer;

import com.bagdatahouse.common.annotation.Sensitive;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.bagdatahouse.common.util.DataMaskingEngine;

import java.io.IOException;

/**
 * Jackson JSON 序列化器 — 字段级脱敏
 * <p>
 * 当字段标注了 {@link Sensitive} 注解时，自动应用脱敏处理。
 * 基于 DataMaskingEngine 实现，支持所有 10 种脱敏类型。
 * <p>
 * 使用方式：在 VO/DTO 字段上加 {@code @Sensitive(type = SensitiveType.PHONE)}
 * Jackson 会自动在序列化时调用本类进行脱敏，无需手动处理。
 */
public class SensitiveJsonSerializer extends JsonSerializer<Object> implements ContextualSerializer {

    private static final DataMaskingEngine MASKING_ENGINE = new DataMaskingEngine();

    private Sensitive annotation;

    public SensitiveJsonSerializer() {
    }

    public SensitiveJsonSerializer(Sensitive annotation) {
        this.annotation = annotation;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        String strValue = value.toString();
        String masked = applySensitiveMask(strValue);
        gen.writeString(masked);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov,
            com.fasterxml.jackson.databind.BeanProperty context) throws JsonMappingException {
        if (context != null) {
            Sensitive ann = context.getAnnotation(Sensitive.class);
            if (ann != null) {
                return new SensitiveJsonSerializer(ann);
            }
        }
        return this;
    }

    private String applySensitiveMask(String value) {
        if (annotation == null) {
            return value;
        }

        Sensitive.SensitiveType type = annotation.type();
        String maskType;
        String maskPattern;

        switch (type) {
            case PHONE -> {
                maskType = "MASK";
                maskPattern = buildPattern(3, 4, annotation.maskChar());
            }
            case EMAIL -> {
                maskType = "MASK";
                maskPattern = buildEmailPattern(annotation.maskChar());
            }
            case ID_CARD -> {
                maskType = "MASK";
                maskPattern = buildPattern(6, 4, annotation.maskChar());
            }
            case BANK_CARD -> {
                maskType = "MASK";
                maskPattern = buildPattern(4, 4, annotation.maskChar());
            }
            case NAME -> {
                maskType = "FORMAT_KEEP";
                maskPattern = buildPattern(1, 1, annotation.maskChar());
            }
            case ADDRESS -> {
                maskType = "MASK";
                maskPattern = buildPattern(6, 0, annotation.maskChar());
            }
            case CUSTOM -> {
                maskType = "MASK";
                maskPattern = buildPattern(annotation.prefixKeep(), annotation.suffixKeep(), annotation.maskChar());
            }
            default -> {
                return value;
            }
        }

        return MASKING_ENGINE.applyMask(value, maskType, maskPattern);
    }

    private String buildPattern(int keepHead, int keepTail, String maskChar) {
        return String.format(
            "{\"keepHead\":%d,\"keepTail\":%d,\"maskChar\":\"%s\"}",
            keepHead, keepTail, maskChar
        );
    }

    private String buildEmailPattern(String maskChar) {
        return String.format(
            "{\"keepHead\":1,\"keepTail\":0,\"maskChar\":\"%s\"}",
            maskChar
        );
    }
}
