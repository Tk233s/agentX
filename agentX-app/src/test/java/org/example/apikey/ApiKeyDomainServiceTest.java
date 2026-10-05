package org.example.apikey;

import org.example.domain.apikey.adapter.repository.ApiKeyRepository;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.Impl.ApiKeyDomainServiceImpl;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiKeyDomainServiceTest {

    @Test
    public void updateWithBlankKeyPreservesExistingSecretAndEnabledState() {
        ApiKeyRepository repository = mock(ApiKeyRepository.class);
        ApiKeyDomainServiceImpl service = service(repository);
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 4, 10, 0);
        ApiKeyEntity existing = new ApiKeyEntity();
        existing.setId("key-1");
        existing.setUserId("user-1");
        existing.setName("OpenAI 主账号");
        existing.setProvider("openai");
        existing.setApiKey("sk-existing-secret");
        existing.setEnabled(false);
        existing.setCreatedAt(createdAt);

        ApiKeyEntity update = new ApiKeyEntity();
        update.setId("key-1");
        update.setName("OpenAI 备用账号");
        update.setProvider("openai");
        update.setApiKey(" ");

        when(repository.findById("key-1")).thenReturn(existing);

        ApiKeyEntity saved = service.updateApiKey(update, "user-1");

        assertEquals("sk-existing-secret", saved.getApiKey());
        assertEquals("OpenAI 备用账号", saved.getName());
        assertEquals(Boolean.FALSE, saved.getEnabled());
        assertEquals("user-1", saved.getUserId());
        assertEquals(createdAt, saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        verify(repository).update(update);
    }

    @Test
    public void createAllowsMultipleKeysForSameProvider() {
        ApiKeyRepository repository = mock(ApiKeyRepository.class);
        ApiKeyDomainServiceImpl service = service(repository);
        ApiKeyEntity existing = ApiKeyEntity.createNew(
                "user-1", "OpenAI 主账号", "openai", "sk-existing-secret", null);

        ApiKeyEntity second = ApiKeyEntity.createNew(
                "user-1", "DeepSeek 备用账号", "OPENAI", "sk-new-secret", "https://api.deepseek.com/v1");

        ApiKeyEntity saved = service.createApiKey(second);

        assertEquals("openai", saved.getProvider());
        assertEquals("DeepSeek 备用账号", saved.getName());
        verify(repository).save(second);
    }

    private ApiKeyDomainServiceImpl service(ApiKeyRepository repository) {
        ApiKeyDomainServiceImpl service = new ApiKeyDomainServiceImpl();
        ReflectionTestUtils.setField(service, "apiKeyRepository", repository);
        return service;
    }
}
