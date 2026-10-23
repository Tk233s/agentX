package org.example.domain.rag.service.Impl;

import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.domain.rag.adapter.port.DocumentTaskPort;
import org.example.domain.rag.adapter.port.DocumentParserPort;
import org.example.domain.rag.adapter.port.EmbeddingPort;
import org.example.domain.rag.adapter.port.FileStoragePort;
import org.example.domain.rag.adapter.port.KeywordSearchPort;
import org.example.domain.rag.adapter.port.VectorStorePort;
import org.example.domain.rag.adapter.repository.KnowledgeBaseRepository;
import org.example.domain.rag.adapter.repository.KnowledgeChunkRepository;
import org.example.domain.rag.adapter.repository.KnowledgeDocumentRepository;
import org.example.domain.rag.model.entity.EmbeddingEntity;
import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.example.domain.rag.model.entity.KnowledgeChunkEntity;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.model.valobj.ContentHash;
import org.example.domain.rag.model.valobj.DocumentIngestResult;
import org.example.domain.rag.model.valobj.EmbeddingCodec;
import org.example.domain.rag.model.valobj.TextChunk;
import org.example.domain.rag.service.IRagDomainService;
import org.example.domain.rag.service.RrfFusion;
import org.example.domain.rag.service.TextChunker;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 知识库领域服务实现。
 *
 * 入库链路刻意不套在一个大事务里：向量化要调外部服务，耗时且可能失败，
 * 让每个仓储操作各自提交，失败时才能把 FAILED 状态和原因真正写进去，
 * 而不是跟事务一起回滚掉。
 */
@Service
public class RagDomainServiceImpl implements IRagDomainService {

    private static final Logger log = LoggerFactory.getLogger(RagDomainServiceImpl.class);

    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Resource
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Resource
    private EmbeddingPort embeddingPort;

    @Resource
    private DocumentParserPort documentParserPort;

    @Resource
    private FileStoragePort fileStoragePort;

    @Resource
    private VectorStorePort vectorStorePort;

    @Resource
    private KeywordSearchPort keywordSearchPort;

    @Resource
    private DocumentTaskPort documentTaskPort;

    @Resource
    private IApiKeyDomainService apiKeyDomainService;

    @Override
    public KnowledgeBaseEntity createKnowledgeBase(KnowledgeBaseEntity knowledgeBase) {
        knowledgeBase.validate();
        // 提前把密钥取一次，避免建完知识库、上传完文档才发现密钥不可用。
        // getApiKey 找不到时返回 null（归属校验才抛异常），所以这里必须自己判空。
        ApiKeyEntity apiKey =
                apiKeyDomainService.getApiKey(knowledgeBase.getApiKeyId(), knowledgeBase.getUserId());
        if (apiKey == null) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "用于向量化的API密钥不存在");
        }
        if (Boolean.FALSE.equals(apiKey.getEnabled())) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "用于向量化的API密钥已停用");
        }
        knowledgeBaseRepository.save(knowledgeBase);
        return knowledgeBase;
    }

    @Override
    public List<KnowledgeBaseEntity> listKnowledgeBases(String userId) {
        return knowledgeBaseRepository.queryByUserId(userId);
    }

    @Override
    public KnowledgeBaseEntity getKnowledgeBase(String id, String userId) {
        return requireKnowledgeBase(id, userId);
    }

    @Override
    public KnowledgeBaseEntity updateRetrievalConfig(
            String id,
            String userId,
            Double minScore,
            Integer topK) {
        KnowledgeBaseEntity knowledgeBase = requireKnowledgeBase(id, userId);
        knowledgeBase.updateRetrievalConfig(minScore, topK);
        knowledgeBaseRepository.updateRetrievalConfig(
                id,
                userId,
                knowledgeBase.effectiveMinScore(),
                knowledgeBase.effectiveTopK());
        return knowledgeBase;
    }

    @Override
    @Transactional("ragTransactionManager")
    public void deleteKnowledgeBase(String id, String userId) {
        requireKnowledgeBase(id, userId);
        List<KnowledgeDocumentEntity> documents =
                knowledgeDocumentRepository.queryByKnowledgeBaseId(id);
        if (documents.stream().anyMatch(this::isActiveDocument)) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "知识库中仍有文档正在处理，请等待处理完成后再删除");
        }
        for (KnowledgeDocumentEntity document : documents) {
            fileStoragePort.delete(document.getFilePath());
        }
        knowledgeChunkRepository.deleteByKnowledgeBaseId(id);
        knowledgeDocumentRepository.deleteByKnowledgeBaseId(id);
        knowledgeBaseRepository.deleteById(id);
    }

    @Override
    public DocumentIngestResult ingestDocument(
            String knowledgeBaseId,
            String userId,
            String fileName,
            byte[] content) {
        KnowledgeBaseEntity knowledgeBase = requireKnowledgeBase(knowledgeBaseId, userId);
        if (content == null || content.length == 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "上传的文件内容为空");
        }
        String fileType = resolveFileType(fileName);
        if (!documentParserPort.supports(fileType)) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "暂不支持的文件类型：" + (fileType.isEmpty() ? "未知" : fileType));
        }

        String contentHash = ContentHash.sha256(content);
        KnowledgeDocumentEntity existing =
                knowledgeDocumentRepository.queryByContentHash(knowledgeBaseId, contentHash);
        if (existing != null) {
            return new DocumentIngestResult(existing, true);
        }

        String filePath = fileStoragePort.store(fileName, content);
        KnowledgeDocumentEntity document = KnowledgeDocumentEntity.createPending(
                knowledgeBaseId,
                userId,
                fileName,
                filePath,
                (long) content.length,
                fileType,
                contentHash);
        knowledgeDocumentRepository.save(document);

        try {
            documentTaskPort.submit(() -> processStoredDocument(document.getId(), userId));
            return new DocumentIngestResult(document, false);
        } catch (RuntimeException e) {
            document.markFailed(rootMessage(e));
            knowledgeDocumentRepository.updateProcessResult(document);
            log.warn("提交文档后台任务失败，document={}, file={}: {}", document.getId(), fileName, e.getMessage());
            throw e;
        }
    }

    @Override
    public KnowledgeDocumentEntity getDocument(String documentId, String userId) {
        return requireDocument(documentId, userId);
    }

    @Override
    public List<KnowledgeDocumentEntity> listDocuments(String knowledgeBaseId, String userId) {
        requireKnowledgeBase(knowledgeBaseId, userId);
        return knowledgeDocumentRepository.queryByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    public void processStoredDocument(String documentId, String userId) {
        KnowledgeDocumentEntity document = knowledgeDocumentRepository.queryById(documentId);
        if (document == null || !userId.equals(document.getUserId())) {
            log.warn("后台处理时文档不存在或归属不符，document={}, user={}", documentId, userId);
            return;
        }
        if (KnowledgeDocumentEntity.STATUS_READY.equals(document.getStatus())) {
            return;
        }

        KnowledgeBaseEntity knowledgeBase =
                requireKnowledgeBase(document.getKnowledgeBaseId(), userId);
        try {
            document.markProcessing();
            knowledgeDocumentRepository.updateProcessResult(document);
            knowledgeChunkRepository.deleteByDocumentId(documentId);

            String text = documentParserPort.parse(document.getFilePath(), document.getFileType());
            List<TextChunk> pieces = TextChunker.chunkWithMetadata(
                    text,
                    knowledgeBase.getChunkSize(),
                    knowledgeBase.getChunkOverlap());
            if (pieces.isEmpty()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "文档里没有可入库的文本");
            }

            List<float[]> vectors = embedTexts(
                    knowledgeBase,
                    userId,
                    pieces.stream().map(TextChunk::content).toList());
            List<KnowledgeChunkEntity> chunks = new ArrayList<>(pieces.size());
            for (int i = 0; i < pieces.size(); i++) {
                TextChunk piece = pieces.get(i);
                chunks.add(KnowledgeChunkEntity.create(
                        document.getKnowledgeBaseId(),
                        documentId,
                        userId,
                        piece.chunkIndex(),
                        piece.content(),
                        EmbeddingCodec.encode(vectors.get(i)),
                        piece.pageNo(),
                        piece.sectionTitle(),
                        ContentHash.sha256(piece.content())));
            }
            knowledgeChunkRepository.batchSave(chunks);

            document.markReady(text.length(), chunks.size());
            knowledgeDocumentRepository.updateProcessResult(document);
        } catch (RuntimeException e) {
            knowledgeChunkRepository.deleteByDocumentId(documentId);
            document.markFailed(rootMessage(e));
            knowledgeDocumentRepository.updateProcessResult(document);
            log.warn(
                    "文档后台处理失败，document={}, file={}: {}",
                    documentId,
                    document.getFileName(),
                    e.getMessage());
        }
    }

    @Override
    public KnowledgeDocumentEntity retryDocument(String documentId, String userId) {
        KnowledgeDocumentEntity document = requireDocument(documentId, userId);
        document.prepareRetry();
        knowledgeDocumentRepository.updateProcessResult(document);
        try {
            documentTaskPort.submit(() -> processStoredDocument(documentId, userId));
        } catch (RuntimeException e) {
            document.markFailed(rootMessage(e));
            knowledgeDocumentRepository.updateProcessResult(document);
            throw e;
        }
        return document;
    }

    @Override
    public void deleteDocument(String documentId, String userId) {
        KnowledgeDocumentEntity document = requireDocument(documentId, userId);
        if (isActiveDocument(document)) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "文档正在处理中，请等待处理完成后再删除");
        }
        knowledgeChunkRepository.deleteByDocumentId(documentId);
        knowledgeDocumentRepository.deleteById(documentId);
        fileStoragePort.delete(document.getFilePath());
    }

    @Override
    public List<ChunkMatch> search(String knowledgeBaseId, String userId, String query, int topK) {
        return search(knowledgeBaseId, userId, query, topK, 1D, 1D, KeywordSearchPort.MIN_KEYWORD_SCORE);
    }

    @Override
    public List<ChunkMatch> search(
            String knowledgeBaseId,
            String userId,
            String query,
            int topK,
            double vectorWeight,
            double keywordWeight,
            double minKeywordScore) {
        KnowledgeBaseEntity knowledgeBase = requireKnowledgeBase(knowledgeBaseId, userId);
        return search(knowledgeBase, userId, query, topK, vectorWeight, keywordWeight, minKeywordScore);
    }

    @Override
    public List<ChunkMatch> searchForConversation(String knowledgeBaseId, String userId, String query) {
        KnowledgeBaseEntity knowledgeBase = requireKnowledgeBase(knowledgeBaseId, userId);
        return search(knowledgeBase, userId, query, knowledgeBase.effectiveTopK(),
                1D, 1D, KeywordSearchPort.MIN_KEYWORD_SCORE);
    }

    private List<ChunkMatch> search(
            KnowledgeBaseEntity knowledgeBase,
            String userId,
            String query,
            int topK,
            double vectorWeight,
            double keywordWeight,
            double minKeywordScore) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int limit = topK <= 0 ? DEFAULT_TOP_K : topK;
        int candidateLimit = Math.min(50, Math.max(limit * 3, 10));
        float[] queryVector = embedTexts(knowledgeBase, userId, List.of(query.trim())).get(0);
        List<ChunkMatch> vectorMatches = vectorStorePort
                .search(knowledgeBase.getId(), userId, queryVector, candidateLimit)
                .stream()
                .filter(match -> Double.isFinite(match.vectorScore())
                        && match.vectorScore() >= knowledgeBase.effectiveMinScore())
                .toList();
        List<ChunkMatch> keywordMatches = keywordSearchPort
                .search(knowledgeBase.getId(), userId, query.trim(), candidateLimit)
                .stream()
                .filter(match -> Double.isFinite(match.keywordScore())
                        && match.keywordScore() >= minKeywordScore)
                .toList();
        return RrfFusion.fuse(vectorMatches, keywordMatches, limit, vectorWeight, keywordWeight);
    }

    /**
     * 调向量化服务，并校验返回的条数和维度是否和知识库配置对得上。
     *
     * 维度对不上说明模型换了，这时必须停下来，否则新旧向量混在一个库里算相似度是错的。
     */
    private List<float[]> embedTexts(
            KnowledgeBaseEntity knowledgeBase,
            String userId,
            List<String> texts) {
        ApiKeyEntity apiKey = apiKeyDomainService.getApiKey(knowledgeBase.getApiKeyId(), userId);
        if (apiKey == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "知识库绑定的API密钥不存在");
        }
        if (Boolean.FALSE.equals(apiKey.getEnabled())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "知识库绑定的API密钥已停用");
        }
        List<float[]> vectors = embeddingPort.embed(EmbeddingEntity.builder()
                .apiKey(apiKey.getApiKey())
                .baseUrl(apiKey.getBaseUrl())
                .model(knowledgeBase.getEmbeddingModel())
                .texts(texts)
                .build());
        if (vectors == null || vectors.size() != texts.size()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化返回的条数与请求不一致");
        }
        int dimension = vectors.get(0) == null ? 0 : vectors.get(0).length;
        if (knowledgeBase.getDimension() != null && dimension != knowledgeBase.getDimension()) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "向量维度与知识库配置不一致：实际 " + dimension + " 维，配置为 "
                            + knowledgeBase.getDimension() + " 维，请确认向量化模型是否被更换");
        }
        return vectors;
    }

    private KnowledgeBaseEntity requireKnowledgeBase(String id, String userId) {
        KnowledgeBaseEntity knowledgeBase = knowledgeBaseRepository.queryById(id);
        if (knowledgeBase == null || !knowledgeBase.getUserId().equals(userId)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "知识库不存在");
        }
        return knowledgeBase;
    }

    private KnowledgeDocumentEntity requireDocument(String documentId, String userId) {
        KnowledgeDocumentEntity document = knowledgeDocumentRepository.queryById(documentId);
        if (document == null || !userId.equals(document.getUserId())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "文档不存在");
        }
        return document;
    }

    private boolean isActiveDocument(KnowledgeDocumentEntity document) {
        return KnowledgeDocumentEntity.STATUS_PENDING.equals(document.getStatus())
                || KnowledgeDocumentEntity.STATUS_PROCESSING.equals(document.getStatus());
    }

    private String resolveFileType(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String rootMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
