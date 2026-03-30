package com.bagdatahouse.datasource.support;

import com.bagdatahouse.common.exception.BusinessException;
import com.bagdatahouse.core.entity.DqDatasource;
import com.bagdatahouse.datasource.enums.DataSourceTypeEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatasourceConfigSupport {

    private static final String ENCRYPTED_PREFIX = "enc::v1::";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final byte[] secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public DatasourceConfigSupport(
            @Value("${bagdatahouse.datasource.secret-key:bagdatahouse-datasource-secret-key-2026}") String secret
    ) {
        this.secretKey = digestSecret(secret);
    }

    public String encryptPasswordIfNecessary(String rawPassword) {
        if (!StringUtils.hasText(rawPassword) || isEncrypted(rawPassword)) {
            return rawPassword;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(rawPassword.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new BusinessException(500, "数据源密码加密失败: " + e.getMessage());
        }
    }

    public String decryptPasswordIfNecessary(String storedPassword) {
        if (!StringUtils.hasText(storedPassword) || !isEncrypted(storedPassword)) {
            return storedPassword;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(storedPassword.substring(ENCRYPTED_PREFIX.length()));
            if (payload.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("密文格式无效");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[payload.length - GCM_IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(500, "数据源密码解密失败: " + e.getMessage());
        }
    }

    public boolean hasStoredPassword(String storedPassword) {
        return StringUtils.hasText(storedPassword);
    }

    public String maskPassword(String storedPassword) {
        return hasStoredPassword(storedPassword) ? "********" : "";
    }

    public String normalizeConnectionParams(String rawParams) {
        Map<String, String> parsed = parseConnectionParams(rawParams);
        if (parsed.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        parsed.forEach((key, value) -> lines.add(key + "=" + value));
        return String.join("\n", lines);
    }

    public Map<String, String> parseConnectionParams(String rawParams) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        if (!StringUtils.hasText(rawParams)) {
            return params;
        }

        String trimmed = rawParams.trim();
        if (looksLikeJsonObject(trimmed)) {
            for (String token : splitTokens(trimmed.substring(1, trimmed.length() - 1), ',')) {
                putParam(params, stripJsonQuotes(token));
            }
            return params;
        }

        for (String token : splitTokens(trimmed, '\n', '\r', '&', ';')) {
            putParam(params, token);
        }
        return params;
    }

    public String buildJdbcUrl(DqDatasource datasource) {
        return buildJdbcUrl(
                datasource.getDsType(),
                datasource.getHost(),
                datasource.getPort(),
                datasource.getDatabaseName(),
                datasource.getSchemaName(),
                datasource.getConnectionParams()
        );
    }

    public String buildJdbcUrl(String dsType, String host, Integer port, String databaseName, String schemaName, String connectionParams) {
        DataSourceTypeEnum typeEnum = DataSourceTypeEnum.fromCode(dsType);
        if (typeEnum == null) {
            throw new BusinessException(400, "不支持的数据源类型: " + dsType);
        }
        if (!StringUtils.hasText(host)) {
            throw new BusinessException(400, "主机地址不能为空");
        }

        int targetPort = port != null && port > 0 ? port : typeEnum.getDefaultPort();
        String normalizedHost = host.trim();
        String normalizedDatabase = normalizeDatabaseName(typeEnum, databaseName);
        LinkedHashMap<String, String> mergedParams = new LinkedHashMap<>(defaultParams(typeEnum));
        mergedParams.putAll(parseConnectionParams(connectionParams));

        if (typeEnum == DataSourceTypeEnum.POSTGRESQL
                && StringUtils.hasText(schemaName)
                && !mergedParams.containsKey("currentSchema")) {
            mergedParams.put("currentSchema", schemaName.trim());
        }

        return switch (typeEnum) {
            case MYSQL, TIDB -> appendQueryParams(
                    "jdbc:mysql://" + normalizedHost + ":" + targetPort + "/" + nullSafeDatabasePath(normalizedDatabase),
                    mergedParams
            );
            case POSTGRESQL -> appendQueryParams(
                    "jdbc:postgresql://" + normalizedHost + ":" + targetPort + "/" + nullSafeDatabasePath(normalizedDatabase),
                    mergedParams
            );
            case SQLSERVER -> appendSemicolonParams(
                    "jdbc:sqlserver://" + normalizedHost + ":" + targetPort,
                    withOptionalDatabase(mergedParams, normalizedDatabase)
            );
            case ORACLE -> appendQueryParams(
                    "jdbc:oracle:thin:@" + normalizedHost + ":" + targetPort + ":" + requiredDatabase(typeEnum, normalizedDatabase),
                    mergedParams
            );
        };
    }

    public String normalizeDatabaseName(DataSourceTypeEnum typeEnum, String databaseName) {
        if (StringUtils.hasText(databaseName)) {
            return databaseName.trim();
        }
        return typeEnum.getDefaultDatabaseWhenBlank();
    }

    public boolean isDatabaseNameRequired(String dsType) {
        DataSourceTypeEnum typeEnum = DataSourceTypeEnum.fromCode(dsType);
        return typeEnum != null && typeEnum.isDatabaseNameRequired();
    }

    public String getValidationQuery(String dsType) {
        if (!StringUtils.hasText(dsType)) {
            return "SELECT 1";
        }
        return switch (dsType.toUpperCase()) {
            case "ORACLE" -> "SELECT 1 FROM DUAL";
            default -> "SELECT 1";
        };
    }

    private byte[] digestSecret(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("初始化数据源密钥失败", e);
        }
    }

    private boolean isEncrypted(String value) {
        return value.startsWith(ENCRYPTED_PREFIX);
    }

    private boolean looksLikeJsonObject(String value) {
        return value.startsWith("{") && value.endsWith("}");
    }

    private List<String> splitTokens(String value, char... separators) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaping) {
                current.append(ch);
                escaping = false;
                continue;
            }
            if (ch == '\\') {
                current.append(ch);
                escaping = true;
                continue;
            }
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }

            if (!inSingleQuote && !inDoubleQuote && isSeparator(ch, separators)) {
                addToken(tokens, current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        addToken(tokens, current.toString());
        return tokens;
    }

    private boolean isSeparator(char value, char... separators) {
        for (char separator : separators) {
            if (value == separator) {
                return true;
            }
        }
        return false;
    }

    private void addToken(List<String> tokens, String token) {
        String normalized = token == null ? "" : token.trim();
        if (StringUtils.hasText(normalized)) {
            tokens.add(normalized);
        }
    }

    private void putParam(Map<String, String> params, String token) {
        String normalized = token == null ? "" : token.trim();
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        while (!normalized.isEmpty() && (normalized.charAt(0) == '?' || normalized.charAt(0) == '&' || normalized.charAt(0) == ';')) {
            normalized = normalized.substring(1).trim();
        }
        if (!StringUtils.hasText(normalized)) {
            return;
        }

        int index = normalized.indexOf('=');
        if (index < 0) {
            params.put(normalized, "");
            return;
        }

        String key = normalized.substring(0, index).trim();
        String value = normalized.substring(index + 1).trim();
        if (StringUtils.hasText(key)) {
            params.put(stripJsonQuotes(key), stripJsonQuotes(value));
        }
    }

    private String stripJsonQuotes(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() >= 2) {
            boolean quoted = (trimmed.startsWith("\"") && trimmed.endsWith("\""))
                    || (trimmed.startsWith("'") && trimmed.endsWith("'"));
            if (quoted) {
                return trimmed.substring(1, trimmed.length() - 1).replace("\\\"", "\"");
            }
        }
        return trimmed;
    }

    private LinkedHashMap<String, String> defaultParams(DataSourceTypeEnum typeEnum) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        switch (typeEnum) {
            case MYSQL, TIDB -> {
                params.put("useUnicode", "true");
                params.put("characterEncoding", "utf8");
                params.put("zeroDateTimeBehavior", "convertToNull");
                params.put("useSSL", "false");
                params.put("serverTimezone", "Asia/Shanghai");
                params.put("allowPublicKeyRetrieval", "true");
            }
            case SQLSERVER -> {
                params.put("encrypt", "false");
                params.put("trustServerCertificate", "true");
            }
            default -> {
            }
        }
        return params;
    }

    private String appendQueryParams(String baseUrl, Map<String, String> params) {
        if (params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder builder = new StringBuilder(baseUrl);
        builder.append(baseUrl.contains("?") ? "&" : "?");

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            builder.append(entry.getKey());
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                builder.append("=").append(entry.getValue());
            }
            first = false;
        }
        return builder.toString();
    }

    private String appendSemicolonParams(String baseUrl, Map<String, String> params) {
        if (params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder builder = new StringBuilder(baseUrl);
        params.forEach((key, value) -> {
            builder.append(";").append(key);
            if (value != null && !value.isEmpty()) {
                builder.append("=").append(value);
            }
        });
        return builder.toString();
    }

    private LinkedHashMap<String, String> withOptionalDatabase(LinkedHashMap<String, String> params, String databaseName) {
        LinkedHashMap<String, String> all = new LinkedHashMap<>();
        if (StringUtils.hasText(databaseName)) {
            all.put("databaseName", databaseName);
        }
        all.putAll(params);
        return all;
    }

    private String requiredDatabase(DataSourceTypeEnum typeEnum, String databaseName) {
        if (StringUtils.hasText(databaseName)) {
            return databaseName;
        }
        throw new BusinessException(400, typeEnum.getDisplayName() + " 连接必须填写数据库名/服务名");
    }

    private String nullSafeDatabasePath(String databaseName) {
        return StringUtils.hasText(databaseName) ? databaseName : "";
    }
}
