package org.example.domain.rag.adapter.repository;

import org.example.domain.rag.model.entity.KnowledgeBaseEntity;

import java.util.List;

/**
 * 知识库仓储端口。
 */
public interface KnowledgeBaseRepository {

    void save(KnowledgeBaseEntity knowledgeBase);

    KnowledgeBaseEntity queryById(String id);

    List<KnowledgeBaseEntity> queryByUserId(String userId);

    /** 按用户批量取知识库，用于把 Agent 绑定的 ID 还原成可读信息。 */
    List<KnowledgeBaseEntity> queryByIdsAndUserId(List<String> ids, String userId);

    /** 只更新会影响对话检索行为的配置，不触碰已入库的分片和向量。 */
    void updateRetrievalConfig(String id, String userId, double minScore, int topK);

    void deleteById(String id);
}
