package org.example.domain.rag.adapter.repository;

import org.example.domain.rag.model.entity.KnowledgeChunkEntity;

import java.util.List;

/**
 * 知识库分片仓储端口。
 */
public interface KnowledgeChunkRepository {

    void batchSave(List<KnowledgeChunkEntity> chunks);

    /**
     * 取出某个用户某个知识库下的全部分片，供应用层做相似度计算。
     *
     * 当前量级直查全量即可；真到了需要近似索引的规模，
     * 应收窄成"由向量库直接返回 top-k"，这个端口就是替换点。
     */
    List<KnowledgeChunkEntity> queryByKnowledgeBaseId(String knowledgeBaseId, String userId);

    int countByKnowledgeBaseId(String knowledgeBaseId);

    void deleteByDocumentId(String documentId);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}
