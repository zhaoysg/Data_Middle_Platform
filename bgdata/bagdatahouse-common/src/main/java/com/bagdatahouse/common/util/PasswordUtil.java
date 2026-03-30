package com.bagdatahouse.common.util;

import cn.hutool.crypto.SecureUtil;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 加密工具类
 */
public class PasswordUtil {

    private PasswordUtil() {}

    /**
     * BCrypt加密
     */
    public static String encrypt(String password) {
        return SecureUtil.sha256(password);
    }

    /**
     * BCrypt密码校验
     */
    public static boolean matches(String password, String encryptPassword) {
        return encrypt(password).equals(encryptPassword);
    }

    /**
     * MD5加密
     */
    public static String md5(String text) {
        return SecureUtil.md5(text);
    }

    /**
     * 生成盐值
     */
    public static String generateSalt() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * AES加密
     */
    public static String aesEncrypt(String text, String key) {
        return SecureUtil.aes(key.getBytes()).encryptBase64(text);
    }

    /**
     * AES解密
     */
    public static String aesDecrypt(String text, String key) {
        return new String(SecureUtil.aes(key.getBytes()).decrypt(text));
    }
}
