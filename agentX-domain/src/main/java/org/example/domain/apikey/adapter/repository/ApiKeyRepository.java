package org.example.domain.apikey.adapter.repository;

import org.example.domain.apikey.model.entity.ApiKeyEntity;

import java.util.List;

/**
 * API密钥仓储接口（端口）
 */
public interface ApiKeyRepository {

    /**
     * 新增API密钥
     */
    void save(ApiKeyEntity apiKey);

    /**
     * 更新API密钥
     */
    void update(ApiKeyEntity apiKey);

    /**
     * 根据ID删除
     */
    int deleteById(String id);

    /**
     * 根据ID查询
     */
    ApiKeyEntity findById(String id);

    /**
     * 根据用户ID查询所有密钥
     */
    List<ApiKeyEntity> queryByUserId(String userId);

    /**
     * 根据用户ID和服务商查询启用的密钥
     */
    ApiKeyEntity findByUserIdAndProvider(String userId, String provider);
}
