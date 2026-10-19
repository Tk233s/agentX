package org.example.domain.rag.adapter.repository;

import org.example.domain.rag.model.entity.KnowledgeChunkEntity;

import java.util.List;

/**
 * 知识库分片仓储端口。
 */
public interface KnowledgeChunkRepository {

    void batchSave(List<KnowledgeChunkEntity> chunks);

    /**
     * 取出某个用户某个知识库下的全部分片。
     *
     * 只服务于入库/删除这类批量操作；topK 相似度检索由 VectorStorePort 负责。
     */
    List<KnowledgeChunkEntity> queryByKnowledgeBaseId(String knowledgeBaseId, String userId);

    int countByKnowledgeBaseId(String knowledgeBaseId);

    void deleteByDocumentId(String documentId);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}
