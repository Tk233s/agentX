package org.example.domain.rag.service.Impl;

import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.domain.rag.adapter.port.DocumentParserPort;
import org.example.domain.rag.adapter.port.EmbeddingPort;
import org.example.domain.rag.adapter.port.FileStoragePort;
import org.example.domain.rag.adapter.port.VectorStorePort;
import org.example.domain.rag.adapter.repository.KnowledgeBaseRepository;
import org.example.domain.rag.adapter.repository.KnowledgeChunkRepository;
import org.example.domain.rag.adapter.repository.KnowledgeDocumentRepository;
import org.example.domain.rag.model.entity.EmbeddingEntity;
import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.example.domain.rag.model.entity.KnowledgeChunkEntity;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.model.valobj.EmbeddingCodec;
import org.example.domain.rag.service.IRagDomainService;
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
    @Transactional
    public void deleteKnowledgeBase(String id, String userId) {
        requireKnowledgeBase(id, userId);
        for (KnowledgeDocumentEntity document : knowledgeDocumentRepository.queryByKnowledgeBaseId(id)) {
            fileStoragePort.delete(document.getFilePath());
        }
        knowledgeChunkRepository.deleteByKnowledgeBaseId(id);
        knowledgeDocumentRepository.deleteByKnowledgeBaseId(id);
        knowledgeBaseRepository.deleteById(id);
    }

    @Override
    public KnowledgeDocumentEntity ingestDocument(
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

        String filePath = fileStoragePort.store(fileName, content);
        KnowledgeDocumentEntity document = KnowledgeDocumentEntity.createPending(
                knowledgeBaseId,
                userId,
                fileName,
                filePath,
                (long) content.length,
                fileType);
        knowledgeDocumentRepository.save(document);

        try {
            document.markProcessing();
            knowledgeDocumentRepository.updateProcessResult(document);

            String text = documentParserPort.parse(filePath, fileType);
            List<String> pieces = TextChunker.chunk(
                    text,
                    knowledgeBase.getChunkSize(),
                    knowledgeBase.getChunkOverlap());
            if (pieces.isEmpty()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "文档里没有可入库的文本");
            }

            List<float[]> vectors = embedTexts(knowledgeBase, userId, pieces);
            List<KnowledgeChunkEntity> chunks = new ArrayList<>(pieces.size());
            for (int i = 0; i < pieces.size(); i++) {
                chunks.add(KnowledgeChunkEntity.create(
                        knowledgeBaseId,
                        document.getId(),
                        userId,
                        i,
                        pieces.get(i),
                        EmbeddingCodec.encode(vectors.get(i))));
            }
            knowledgeChunkRepository.batchSave(chunks);

            document.markReady(text.length(), chunks.size());
            knowledgeDocumentRepository.updateProcessResult(document);
            return document;
        } catch (RuntimeException e) {
            // 半途写进去的分片要清掉，否则重新上传会留下重复数据
            knowledgeChunkRepository.deleteByDocumentId(document.getId());
            document.markFailed(rootMessage(e));
            knowledgeDocumentRepository.updateProcessResult(document);
            log.warn("文档入库失败，document={}, file={}: {}", document.getId(), fileName, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<KnowledgeDocumentEntity> listDocuments(String knowledgeBaseId, String userId) {
        requireKnowledgeBase(knowledgeBaseId, userId);
        return knowledgeDocumentRepository.queryByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    public void deleteDocument(String documentId, String userId) {
        KnowledgeDocumentEntity document = knowledgeDocumentRepository.queryById(documentId);
        if (document == null || !userId.equals(document.getUserId())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "文档不存在");
        }
        knowledgeChunkRepository.deleteByDocumentId(documentId);
        knowledgeDocumentRepository.deleteById(documentId);
        fileStoragePort.delete(document.getFilePath());
    }

    @Override
    public List<ChunkMatch> search(String knowledgeBaseId, String userId, String query, int topK) {
        KnowledgeBaseEntity knowledgeBase = requireKnowledgeBase(knowledgeBaseId, userId);
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int limit = topK <= 0 ? DEFAULT_TOP_K : topK;
        float[] queryVector = embedTexts(knowledgeBase, userId, List.of(query.trim())).get(0);
        return vectorStorePort.search(knowledgeBaseId, userId, queryVector, limit);
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
