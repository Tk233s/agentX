package org.example.trigger.dto.rag;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库 HTTP 出参 DTO
 */
@Data
public class KnowledgeBaseRes {

    private String id;

    private String name;

    private String description;

    /** 向量化使用的 API 密钥ID */
    private String apiKeyId;

    private String embeddingModel;

    private Integer dimension;

    private Integer chunkSize;

    private Integer chunkOverlap;

    /** 已入库的分片总数，列表页直接展示，避免前端再查一次 */
    private Integer chunkCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
