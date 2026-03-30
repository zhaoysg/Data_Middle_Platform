package com.bagdatahouse.governance.advice;

import com.bagdatahouse.common.annotation.Sensitive;
import com.bagdatahouse.common.util.DataMaskingEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 响应拦截器 — 敏感字段动态脱敏
 * <p>
 * 工作流程：
 * 1. 拦截所有 JSON 响应
 * 2. 检查响应对象及其字段是否有 @Sensitive 注解（优先级1）
 * 3. 调用 DataMaskingEngine.applyMask() 执行脱敏
 * 4. 返回脱敏后 JSON
 * <p>
 * 使用方式：在 VO/DTO 字段上添加 {@code @Sensitive(type = SensitiveType.PHONE)}
 */
@Slf4j
@RestControllerAdvice
public class SensitiveFieldAdvice implements ResponseBodyAdvice<Object> {

    private static final DataMaskingEngine MASKING_ENGINE = new DataMaskingEngine();

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request,
            ServerHttpResponse response) {

        if (body == null) {
            return null;
        }

        if (!isJsonContentType(selectedContentType)) {
            return body;
        }

        try {
            return maskSensitiveFields(body);
        } catch (Exception e) {
            log.debug("敏感字段脱敏处理失败，降级返回原始响应: {}", e.getMessage());
            return body;
        }
    }

    private boolean isJsonContentType(MediaType contentType) {
        if (contentType == null) {
            return false;
        }
        String subtype = contentType.getSubtype();
        return subtype != null && (subtype.contains("json") || subtype.contains("xml"));
    }

    /**
     * 对响应体进行敏感字段脱敏处理
     */
    private Object maskSensitiveFields(Object body) throws IOException {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(body);
            JsonNode root = OBJECT_MAPPER.readTree(json);
            maskNode(root, body.getClass());
            JavaType javaType = OBJECT_MAPPER.constructType(body.getClass());
            return OBJECT_MAPPER.readValue(OBJECT_MAPPER.treeAsTokens(root), javaType);
        } catch (JsonProcessingException e) {
            log.debug("敏感字段脱敏序列化失败: {}", e.getMessage());
            return body;
        }
    }

    /**
     * 递归遍历 JSON 树，对标注了 @Sensitive 的字段进行脱敏
     */
    private void maskNode(JsonNode node, Class<?> clazz) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isArray()) {
            if (node.size() > 0) {
                Class<?> elementType = inferElementType(clazz);
                for (int i = 0; i < node.size(); i++) {
                    JsonNode element = node.get(i);
                    if (element != null && !element.isNull()) {
                        maskNode(element, elementType);
                    }
                }
            }
            return;
        }

        if (node.isObject()) {
            Map<String, Field> fieldMap = getFieldMap(clazz);
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String fieldName = entry.getKey();
                JsonNode fieldNode = entry.getValue();
                Field field = fieldMap.get(fieldName);

                if (field != null && field.isAnnotationPresent(Sensitive.class)) {
                    Sensitive sensitive = field.getAnnotation(Sensitive.class);
                    if (fieldNode != null && !fieldNode.isNull() && fieldNode.isTextual()) {
                        String original = fieldNode.asText();
                        String masked = MASKING_ENGINE.applyMask(
                                original,
                                mapSensitiveTypeToMaskType(sensitive.type()),
                                buildAnnotationPattern(sensitive)
                        );
                        ((ObjectNode) node).put(fieldName, masked);
                    }
                } else if (fieldNode != null && fieldNode.isObject()) {
                    Class<?> nestedType = field != null ? field.getType() : Object.class;
                    maskNode(fieldNode, nestedType);
                } else if (fieldNode != null && fieldNode.isArray()) {
                    if (field != null) {
                        Class<?> nestedType = inferComponentType(field.getType());
                        for (int i = 0; i < fieldNode.size(); i++) {
                            JsonNode element = fieldNode.get(i);
                            if (element != null && !element.isNull()) {
                                maskNode(element, nestedType);
                            }
                        }
                    }
                }
            }
        }
    }

    private Class<?> inferElementType(Class<?> collectionType) {
        if (List.class.isAssignableFrom(collectionType)) {
            return Object.class;
        }
        if (collectionType.isArray()) {
            return collectionType.getComponentType();
        }
        return Object.class;
    }

    private Class<?> inferComponentType(Class<?> type) {
        if (type.isArray()) {
            return type.getComponentType();
        }
        if (List.class.isAssignableFrom(type)) {
            return Object.class;
        }
        return Object.class;
    }

    private Map<String, Field> getFieldMap(Class<?> clazz) {
        Map<String, Field> map = new HashMap<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                map.put(field.getName(), field);
                com.fasterxml.jackson.annotation.JsonProperty jsonProp =
                        field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
                if (jsonProp != null) {
                    map.put(jsonProp.value(), field);
                }
            }
            current = current.getSuperclass();
        }
        return map;
    }

    private String mapSensitiveTypeToMaskType(Sensitive.SensitiveType type) {
        if (type == null) {
            return "MASK";
        }
        return switch (type) {
            case PHONE, EMAIL, ID_CARD, BANK_CARD, ADDRESS, CUSTOM -> "MASK";
            case NAME -> "FORMAT_KEEP";
            case DEFAULT -> "MASK";
        };
    }

    private String buildAnnotationPattern(Sensitive sensitive) {
        if (sensitive == null) {
            return null;
        }
        return String.format(
            "{\"keepHead\":%d,\"keepTail\":%d,\"maskChar\":\"%s\"}",
            sensitive.prefixKeep(), sensitive.suffixKeep(), sensitive.maskChar()
        );
    }
}
