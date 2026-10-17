package org.example.domain.rag.adapter.repository;

import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;

import java.util.List;

/**
 * 知识库文档仓储端口。
 */
public interface KnowledgeDocumentRepository {

    void save(KnowledgeDocumentEntity document);

    /** 更新处理状态、字符数、分片数与失败原因。 */
    void updateProcessResult(KnowledgeDocumentEntity document);

    KnowledgeDocumentEntity queryById(String id);

    List<KnowledgeDocumentEntity> queryByKnowledgeBaseId(String knowledgeBaseId);

    void deleteById(String id);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}
