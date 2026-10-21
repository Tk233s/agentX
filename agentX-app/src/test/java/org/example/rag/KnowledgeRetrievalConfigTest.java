package org.example.rag;

import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.domain.rag.adapter.port.EmbeddingPort;
import org.example.domain.rag.adapter.port.KeywordSearchPort;
import org.example.domain.rag.adapter.port.VectorStorePort;
import org.example.domain.rag.adapter.repository.KnowledgeBaseRepository;
import org.example.domain.rag.model.entity.EmbeddingEntity;
import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.service.Impl.RagDomainServiceImpl;
import org.example.types.exception.AppException;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识库检索配置与对话过滤规则。
 */
public class KnowledgeRetrievalConfigTest {

    @Test
    public void newKnowledgeBaseUsesSafeRetrievalDefaults() {
        KnowledgeBaseEntity knowledgeBase = KnowledgeBaseEntity.createNew(
                "user-1",
                "测试库",
                null,
                "key-1",
                "BAAI/bge-m3",
                null,
                null,
                null,
                null,
                null);

        assertEquals(0.55D, knowledgeBase.effectiveMinScore(), 0.000001);
        assertEquals(3, knowledgeBase.effectiveTopK());
    }

    @Test(expected = AppException.class)
    public void rejectsInvalidSimilarityThreshold() {
        KnowledgeBaseEntity knowledgeBase = new KnowledgeBaseEntity();
        knowledgeBase.updateRetrievalConfig(1.1D, 3);
    }

    @Test(expected = AppException.class)
    public void rejectsTopKOutsideSupportedRange() {
        KnowledgeBaseEntity knowledgeBase = new KnowledgeBaseEntity();
        knowledgeBase.updateRetrievalConfig(0.55D, 21);
    }

    @Test
    public void conversationSearchUsesKnowledgeBaseTopKAndThreshold() {
        KnowledgeBaseRepository knowledgeBaseRepository = mock(KnowledgeBaseRepository.class);
        IApiKeyDomainService apiKeyDomainService = mock(IApiKeyDomainService.class);
        EmbeddingPort embeddingPort = mock(EmbeddingPort.class);
        VectorStorePort vectorStorePort = mock(VectorStorePort.class);
        KeywordSearchPort keywordSearchPort = mock(KeywordSearchPort.class);

        KnowledgeBaseEntity knowledgeBase = new KnowledgeBaseEntity();
        knowledgeBase.setId("kb-1");
        knowledgeBase.setUserId("user-1");
        knowledgeBase.setApiKeyId("key-1");
        knowledgeBase.setEmbeddingModel("BAAI/bge-m3");
        knowledgeBase.setDimension(3);
        knowledgeBase.setMinScore(0.55D);
        knowledgeBase.setTopK(2);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setEnabled(true);
        apiKey.setApiKey("test-key");
        apiKey.setBaseUrl("https://example.test");

        when(knowledgeBaseRepository.queryById("kb-1")).thenReturn(knowledgeBase);
        when(apiKeyDomainService.getApiKey("key-1", "user-1")).thenReturn(apiKey);
        when(embeddingPort.embed(any(EmbeddingEntity.class)))
                .thenReturn(List.of(new float[]{1F, 0F, 0F}));
        when(vectorStorePort.search(eq("kb-1"), eq("user-1"), any(float[].class), eq(10)))
                .thenReturn(List.of(
                        match("chunk-1", 0.88D),
                        match("chunk-2", 0.55D),
                        match("chunk-3", 0.54D)));
        when(keywordSearchPort.search("kb-1", "user-1", "怎么换新", 10))
                .thenReturn(List.of(keywordMatch("chunk-low", 0.10D)));

        RagDomainServiceImpl service = new RagDomainServiceImpl();
        ReflectionTestUtils.setField(service, "knowledgeBaseRepository", knowledgeBaseRepository);
        ReflectionTestUtils.setField(service, "apiKeyDomainService", apiKeyDomainService);
        ReflectionTestUtils.setField(service, "embeddingPort", embeddingPort);
        ReflectionTestUtils.setField(service, "vectorStorePort", vectorStorePort);
        ReflectionTestUtils.setField(service, "keywordSearchPort", keywordSearchPort);

        List<ChunkMatch> matches = service.searchForConversation("kb-1", "user-1", "怎么换新");

        assertEquals(2, matches.size());
        assertEquals("chunk-1", matches.get(0).chunkId());
        assertEquals("chunk-2", matches.get(1).chunkId());
        verify(vectorStorePort).search(eq("kb-1"), eq("user-1"), any(float[].class), eq(10));
        verify(keywordSearchPort).search("kb-1", "user-1", "怎么换新", 10);
    }

    private ChunkMatch match(String chunkId, double score) {
        return new ChunkMatch(
                "kb-1",
                chunkId,
                "doc-1",
                "售后政策.txt",
                "测试内容-" + chunkId,
                score);
    }

    private ChunkMatch keywordMatch(String chunkId, double score) {
        return new ChunkMatch(
                "kb-1",
                chunkId,
                "doc-1",
                "售后政策.txt",
                "低质量关键词命中-" + chunkId,
                0,
                null,
                null,
                0D,
                score,
                0D,
                "KEYWORD");
    }
}
