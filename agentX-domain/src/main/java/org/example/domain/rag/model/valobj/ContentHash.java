package org.example.domain.rag.model.valobj;

import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 文档与分片的内容指纹，用于上传去重和引用定位。
 */
public final class ContentHash {

    private ContentHash() {
    }

    public static String sha256(byte[] content) {
        return sha256Hex(content);
    }

    public static String sha256(String content) {
        return sha256Hex(content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content == null ? new byte[0] : content));
        } catch (NoSuchAlgorithmException e) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "当前运行环境不支持 SHA-256");
        }
    }
}
