package org.example.domain.apikey.service;

import org.example.domain.apikey.model.entity.ApiKeyEntity;

import java.util.List;

/**
 * API密钥领域服务接口
 */
public interface IApiKeyDomainService {

    /**
     * 创建API密钥
     */
    ApiKeyEntity createApiKey(ApiKeyEntity apiKey);

    /**
     * 更新API密钥
     */
    ApiKeyEntity updateApiKey(ApiKeyEntity apiKey, String userId);

    /**
     * 根据ID删除API密钥
     */
    void deleteApiKey(String id, String userId);

    /**
     * 根据ID查询API密钥
     */
    ApiKeyEntity getApiKey(String id, String userId);

    /**
     * 根据用户ID查询所有密钥
     */
    List<ApiKeyEntity> listApiKeys(String userId);

    /**
     * 根据用户ID和服务商查询启用的密钥（供对话模块调用）
     */
    ApiKeyEntity getApiKeyByProvider(String userId, String provider);
}
