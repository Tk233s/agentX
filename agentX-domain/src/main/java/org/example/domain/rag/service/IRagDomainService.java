package org.example.domain.rag.service;

import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.model.valobj.DocumentIngestResult;

import java.util.List;

/**
 * 知识库领域服务接口。
 *
 * 覆盖三件事：知识库本身的增删查、文档入库（解析-分片-向量化-落库）、检索。
 */
public interface IRagDomainService {

    /** 检索测试接口未显式传 TopK 时使用的默认值。 */
    int DEFAULT_TOP_K = KnowledgeBaseEntity.DEFAULT_TOP_K;

    KnowledgeBaseEntity createKnowledgeBase(KnowledgeBaseEntity knowledgeBase);

    List<KnowledgeBaseEntity> listKnowledgeBases(String userId);

    KnowledgeBaseEntity getKnowledgeBase(String id, String userId);

    /** 更新知识库级别的对话检索阈值和 TopK。 */
    KnowledgeBaseEntity updateRetrievalConfig(
            String id,
            String userId,
            Double minScore,
            Integer topK);

    /** 删除知识库，连同其文档、分片和落盘文件一起清理。 */
    void deleteKnowledgeBase(String id, String userId);

    /**
     * 上传文档并提交后台处理。
     *
     * 文件落盘和 PENDING 记录创建完成后立即返回；解析、分片、向量化由后台任务继续执行。
     * 相同知识库内内容相同的文件会直接返回已有记录。
     */
    DocumentIngestResult ingestDocument(
            String knowledgeBaseId,
            String userId,
            String fileName,
            byte[] content);

    /** 查询单个文档并校验归属。 */
    KnowledgeDocumentEntity getDocument(String documentId, String userId);

    List<KnowledgeDocumentEntity> listDocuments(String knowledgeBaseId, String userId);

    /** 删除文档，连带它的分片和落盘文件。 */
    void deleteDocument(String documentId, String userId);

    /** 后台处理已落盘的文档，只由调度适配器调用。 */
    void processStoredDocument(String documentId, String userId);

    /** 将失败文档重置并重新提交后台处理。 */
    KnowledgeDocumentEntity retryDocument(String documentId, String userId);

    /** 使用向量召回、关键词召回和 RRF 融合检索最相近的分片。 */
    List<ChunkMatch> search(String knowledgeBaseId, String userId, String query, int topK);

    /**
     * 按知识库自身的阈值和 TopK 检索，只返回可以进入对话上下文的结果。
     */
    List<ChunkMatch> searchForConversation(String knowledgeBaseId, String userId, String query);
}
