package org.example.domain.rag.service;

import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.domain.rag.model.valobj.ChunkMatch;

import java.util.List;

/**
 * 知识库领域服务接口。
 *
 * 覆盖三件事：知识库本身的增删查、文档入库（解析-分片-向量化-落库）、检索。
 */
public interface IRagDomainService {

    /** 默认检索条数。 */
    int DEFAULT_TOP_K = 4;

    KnowledgeBaseEntity createKnowledgeBase(KnowledgeBaseEntity knowledgeBase);

    List<KnowledgeBaseEntity> listKnowledgeBases(String userId);

    KnowledgeBaseEntity getKnowledgeBase(String id, String userId);

    /** 删除知识库，连同其文档、分片和落盘文件一起清理。 */
    void deleteKnowledgeBase(String id, String userId);

    /**
     * 上传并处理一份文档。
     *
     * 处理过程是同步的：解析文本、分片、向量化、写库全部走完才返回。
     * 失败时会把文档标记为 FAILED 并保留原因，已写入的分片会被清理掉。
     */
    KnowledgeDocumentEntity ingestDocument(
            String knowledgeBaseId,
            String userId,
            String fileName,
            byte[] content);

    List<KnowledgeDocumentEntity> listDocuments(String knowledgeBaseId, String userId);

    /** 删除文档，连带它的分片和落盘文件。 */
    void deleteDocument(String documentId, String userId);

    /** 用一句话去知识库里检索最相近的分片。 */
    List<ChunkMatch> search(String knowledgeBaseId, String userId, String query, int topK);
}
