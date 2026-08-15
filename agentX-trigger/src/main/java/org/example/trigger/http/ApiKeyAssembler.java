package org.example.trigger.http;

import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.trigger.dto.apikey.ApiKeyReq;
import org.example.trigger.dto.apikey.ApiKeyRes;

/**
 * HTTP 层 DTO 与领域实体之间的转换器
 */
public class ApiKeyAssembler {

    /** 入参 DTO -> 领域实体（创建） */
    public static ApiKeyEntity toEntity(ApiKeyReq req) {
        return ApiKeyEntity.createNew(
                req.getUserId(),
                req.getProvider(),
                req.getApiKey(),
                req.getBaseUrl());
    }

    /** 入参 DTO -> 领域实体（更新） */
    public static ApiKeyEntity toUpdateEntity(ApiKeyReq req) {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId(req.getId());
        entity.setProvider(req.getProvider());
        entity.setApiKey(req.getApiKey());
        entity.setBaseUrl(req.getBaseUrl());
        return entity;
    }

    /** 领域实体 -> 出参 DTO（apiKey 脱敏） */
    public static ApiKeyRes toRes(ApiKeyEntity entity) {
        ApiKeyRes res = new ApiKeyRes();
        res.setId(entity.getId());
        res.setProvider(entity.getProvider());
        res.setApiKey(maskApiKey(entity.getApiKey()));
        res.setBaseUrl(entity.getBaseUrl());
        res.setEnabled(entity.getEnabled());
        res.setCreateTime(entity.getCreatedAt());
        res.setUpdateTime(entity.getUpdatedAt());
        return res;
    }

    /**
     * API密钥脱敏：只显示后4位，前面用 **** 替代
     * 例：sk-abcdefgh1234 -> ****1234
     */
    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 4) {
            return "****";
        }
        return "****" + apiKey.substring(apiKey.length() - 4);
    }
}
