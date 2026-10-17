package org.example.domain.rag.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 知识库分片领域实体。
 *
 * embedding 保存的是向量文本形态，检索时由向量端口解析成数值参与相似度计算。
 */
@Data
public class KnowledgeChunkEntity {

    /** 主键ID */
    private String id;

    /** 所属知识库ID，冗余保存便于按库检索而不必回表文档 */
    private String knowledgeBaseId;

    /** 所属文档ID */
    private String documentId;

    /** 所属用户ID */
    private String userId;

    /** 在文档内的序号，从 0 开始 */
    private Integer chunkIndex;

    /** 分片正文 */
    private String content;

    /** 向量文本 */
    private String embedding;

    /** 分片字符数 */
    private Integer charCount;

    private LocalDateTime createdAt;

    public static KnowledgeChunkEntity create(
            String knowledgeBaseId,
            String documentId,
            String userId,
            int chunkIndex,
            String content,
            String embedding) {
        KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setKnowledgeBaseId(knowledgeBaseId);
        entity.setDocumentId(documentId);
        entity.setUserId(userId);
        entity.setChunkIndex(chunkIndex);
        entity.setContent(content);
        entity.setEmbedding(embedding);
        entity.setCharCount(content == null ? 0 : content.length());
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
