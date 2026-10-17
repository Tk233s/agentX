package org.example.infrastructure.dao.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库分片持久化对象。
 */
@Data
public class KnowledgeChunkPO {

    private String id;
    private String knowledgeBaseId;
    private String documentId;
    private String userId;
    private Integer chunkIndex;
    private String content;
    private String embedding;
    private Integer charCount;
    private LocalDateTime createdAt;
}
