package org.example.domain.apikey.model.entity;

import lombok.Data;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;

import java.time.LocalDateTime;
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

    /** 服务商 openai/anthropic */
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
    public static ApiKeyEntity createNew(String userId, String provider, String apiKey, String baseUrl) {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setUserId(userId);
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
        if (provider == null || provider.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "服务商不能为空");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "API密钥不能为空");
        }
    }
}
