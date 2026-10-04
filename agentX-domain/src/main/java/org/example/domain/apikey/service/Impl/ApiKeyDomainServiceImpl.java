package org.example.domain.apikey.service.Impl;

import org.example.domain.apikey.adapter.repository.ApiKeyRepository;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API密钥领域服务实现
 */
@Service
public class ApiKeyDomainServiceImpl implements IApiKeyDomainService {

    @Resource
    private ApiKeyRepository apiKeyRepository;

    @Transactional
    public ApiKeyEntity createApiKey(ApiKeyEntity apiKey) {
        apiKey.validate();
        apiKeyRepository.save(apiKey);
        return apiKey;
    }

    @Transactional
    public ApiKeyEntity updateApiKey(ApiKeyEntity apiKey, String userId) {
        ApiKeyEntity existing = requireOwnedApiKey(apiKey.getId(), userId);
        if (apiKey.getApiKey() == null || apiKey.getApiKey().trim().isEmpty()) {
            apiKey.setApiKey(existing.getApiKey());
        }
        if (apiKey.getEnabled() == null) {
            apiKey.setEnabled(existing.getEnabled());
        }
        apiKey.validate();
        // 保留不可覆盖字段
        apiKey.setUserId(existing.getUserId());
        apiKey.setCreatedAt(existing.getCreatedAt());
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyRepository.update(apiKey);
        return apiKey;
    }

    @Transactional
    public void deleteApiKey(String id, String userId) {
        requireOwnedApiKey(id, userId);
        int rows = apiKeyRepository.deleteById(id);
        if (rows == 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "API密钥不存在");
        }
    }

    public ApiKeyEntity getApiKey(String id, String userId) {
        ApiKeyEntity apiKey = apiKeyRepository.findById(id);
        if (apiKey == null) {
            return null;
        }
        checkOwner(apiKey, userId);
        return apiKey;
    }

    public List<ApiKeyEntity> listApiKeys(String userId) {
        return apiKeyRepository.queryByUserId(userId);
    }

    private ApiKeyEntity requireOwnedApiKey(String id, String userId) {
        ApiKeyEntity apiKey = apiKeyRepository.findById(id);
        if (apiKey == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "API密钥不存在");
        }
        checkOwner(apiKey, userId);
        return apiKey;
    }

    private void checkOwner(ApiKeyEntity apiKey, String userId) {
        if (userId == null || !userId.equals(apiKey.getUserId())) {
            throw new AppException(ResponseCode.FORBIDDEN.getCode(), "无权访问该API密钥");
        }
    }

}
