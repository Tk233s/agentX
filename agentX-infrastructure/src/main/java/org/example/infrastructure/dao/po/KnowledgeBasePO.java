package org.example.infrastructure.dao.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库持久化对象。
 */
@Data
public class KnowledgeBasePO {

    private String id;
    private String userId;
    private String name;
    private String description;
    private String apiKeyId;
    private String embeddingModel;
    private Integer dimension;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
