package org.example.infrastructure.dao.po;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档持久化对象。
 */
@Data
public class KnowledgeDocumentPO {

    private String id;
    private String knowledgeBaseId;
    private String userId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private Integer charCount;
    private Integer chunkCount;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
