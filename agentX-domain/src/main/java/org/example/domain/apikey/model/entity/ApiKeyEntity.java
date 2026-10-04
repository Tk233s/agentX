package org.example.domain.apikey.model.entity;

import lombok.Data;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * API密钥领域实体
 */
@Data
public class ApiKeyEntity {

    /** 主键ID */
    private String id;

    /** 所属用户ID */
    private String userId;

    /** 密钥名称，用于区同一协议下的多个账号或中转地址 */
    private String name;

    /** 接口协议 openai/anthropic */
    private String provider;

    /** API密钥 */
    private String apiKey;

    /** 自定义API地址（代理/中转） */
    private String baseUrl;

    /** 是否启用 */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 工厂方法：创建新的API密钥
     */
    public static ApiKeyEntity createNew(String userId, String name, String provider, String apiKey, String baseUrl) {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setUserId(userId);
        entity.setName(name);
        entity.setProvider(provider);
        entity.setApiKey(apiKey);
        entity.setBaseUrl(baseUrl);
        entity.setEnabled(true);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    /**
     * 业务规则校验
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密钥名称不能为空");
        }
        if (provider == null || provider.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "接口协议不能为空");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "API密钥不能为空");
        }

        name = name.trim();
        provider = provider.trim().toLowerCase(Locale.ROOT);
        apiKey = apiKey.trim();
        if (!"openai".equals(provider) && !"anthropic".equals(provider)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "接口协议仅支持 openai 或 anthropic");
        }
        if (baseUrl != null) {
            baseUrl = baseUrl.trim();
            if (baseUrl.isEmpty()) {
                baseUrl = null;
            }
        }
    }
}
