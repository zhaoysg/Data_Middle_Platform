package org.dromara.datasource.support;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.encrypt.utils.EncryptUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 数据源敏感信息加解密支持
 */
@Slf4j
@Component
public class DatasourceCryptoSupport {

    private static final String PREFIX = "{enc}";

    @Value("${datasource.encrypt.key:}")
    private String encryptKey;

    public String encryptPassword(String raw) {
        if (StringUtils.isBlank(raw) || isEncrypted(raw)) {
            return raw;
        }
        if (StringUtils.isBlank(encryptKey)) {
            return raw;
        }
        try {
            return PREFIX + EncryptUtils.encryptByAes(raw, encryptKey.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("数据源密码加密失败: {}", ex.getMessage());
            throw new ServiceException("数据源密码加密失败，请检查 datasource.encrypt.key 配置");
        }
    }

    public String decryptPassword(String raw) {
        if (StringUtils.isBlank(raw) || !isEncrypted(raw)) {
            return raw;
        }
        if (StringUtils.isBlank(encryptKey)) {
            throw new ServiceException("数据源密码已加密，请配置 datasource.encrypt.key");
        }
        String cipher = raw.substring(PREFIX.length());
        try {
            return EncryptUtils.decryptByAes(cipher, encryptKey.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("数据源密码解密失败: {}", ex.getMessage());
            throw new ServiceException("数据源密码解密失败，请检查 datasource.encrypt.key 配置");
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}
