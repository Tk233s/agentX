package org.example.domain.rag.adapter.repository;

import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库文档仓储端口。
 */
public interface KnowledgeDocumentRepository {

    void save(KnowledgeDocumentEntity document);

    /** 更新处理状态、字符数、分片数与失败原因。 */
    void updateProcessResult(KnowledgeDocumentEntity document);

    KnowledgeDocumentEntity queryById(String id);

    /** 在同一个知识库内按原文件指纹查找，用于上传去重。 */
    KnowledgeDocumentEntity queryByContentHash(String knowledgeBaseId, String contentHash);

    List<KnowledgeDocumentEntity> queryByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * 查询指定状态、且更新时间早于给定时刻的文档。
     *
     * 供启动时回收被进程中断留下的处理中文档使用，因此不带知识库或用户归属条件。
     */
    List<KnowledgeDocumentEntity> queryByStatusBefore(String status, LocalDateTime before);

    void deleteById(String id);

    void deleteByKnowledgeBaseId(String knowledgeBaseId);
}
