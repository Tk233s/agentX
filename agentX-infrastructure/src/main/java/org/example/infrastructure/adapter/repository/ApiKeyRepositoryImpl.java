package org.example.infrastructure.adapter.repository;

import org.example.domain.apikey.adapter.repository.ApiKeyRepository;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.infrastructure.dao.IApiKeyDao;
import org.example.infrastructure.dao.po.ApiKeyPO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API密钥仓储实现
 * 实现领域层定义的 ApiKeyRepository 端口
 */
@Repository
public class ApiKeyRepositoryImpl implements ApiKeyRepository {

    @Resource
    private IApiKeyDao apiKeyDao;

    @Override
    public void save(ApiKeyEntity apiKey) {
        apiKeyDao.insert(toPO(apiKey));
    }

    @Override
    public void update(ApiKeyEntity apiKey) {
        apiKeyDao.update(toPO(apiKey));
    }

    @Override
    public int deleteById(String id) {
        return apiKeyDao.deleteById(id);
    }

    @Override
    public ApiKeyEntity findById(String id) {
        return toEntity(apiKeyDao.queryById(id));
    }

    @Override
    public List<ApiKeyEntity> queryByUserId(String userId) {
        List<ApiKeyPO> pos = apiKeyDao.queryByUserId(userId);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }
        return pos.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public ApiKeyEntity findByUserIdAndProvider(String userId, String provider) {
        return toEntity(apiKeyDao.queryByUserIdAndProvider(userId, provider));
    }

    /** Entity -> PO */
    private ApiKeyPO toPO(ApiKeyEntity entity) {
        ApiKeyPO po = new ApiKeyPO();
        po.setId(entity.getId());
        po.setUserId(entity.getUserId());
        po.setProvider(entity.getProvider());
        po.setApiKey(entity.getApiKey());
        po.setBaseUrl(entity.getBaseUrl());
        po.setEnabled(entity.getEnabled());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        return po;
    }

    /** PO -> Entity */
    private ApiKeyEntity toEntity(ApiKeyPO po) {
        if (po == null) {
            return null;
        }
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setId(po.getId());
        entity.setUserId(po.getUserId());
        entity.setProvider(po.getProvider());
        entity.setApiKey(po.getApiKey());
        entity.setBaseUrl(po.getBaseUrl());
        entity.setEnabled(po.getEnabled());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        return entity;
    }
}
