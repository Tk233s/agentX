package org.example.domain.rag.model.entity;

import lombok.Data;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 知识库文档领域实体。
 *
 * 状态流转：PENDING -> PROCESSING -> READY / FAILED。
 * 只有 READY 的文档才会被检索到，失败原因保留下来方便排查。
 */
@Data
public class KnowledgeDocumentEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_FAILED = "FAILED";

    private static final int MAX_ERROR_LENGTH = 400;

    /** 主键ID */
    private String id;

    /** 所属知识库ID */
    private String knowledgeBaseId;

    /** 所属用户ID */
    private String userId;

    /** 原始文件名 */
    private String fileName;

    /** 落盘路径 */
    private String filePath;

    /** 文件字节数 */
    private Long fileSize;

    /** 扩展名，如 txt、md */
    private String fileType;

    /** 抽取出的字符数 */
    private Integer charCount;

    /** 分片数量 */
    private Integer chunkCount;

    /** 处理状态 */
    private String status;

    /** 失败原因 */
    private String errorMessage;

    /** 原文件 SHA-256，用于同一知识库内的内容去重 */
    private String contentHash;

    /** 已尝试处理的次数 */
    private Integer retryCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KnowledgeDocumentEntity createPending(
            String knowledgeBaseId,
            String userId,
            String fileName,
            String filePath,
            Long fileSize,
            String fileType) {
        return createPending(
                knowledgeBaseId,
                userId,
                fileName,
                filePath,
                fileSize,
                fileType,
                null);
    }

    public static KnowledgeDocumentEntity createPending(
            String knowledgeBaseId,
            String userId,
            String fileName,
            String filePath,
            Long fileSize,
            String fileType,
            String contentHash) {
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setKnowledgeBaseId(knowledgeBaseId);
        entity.setUserId(userId);
        entity.setFileName(fileName);
        entity.setFilePath(filePath);
        entity.setFileSize(fileSize == null ? 0L : fileSize);
        entity.setFileType(fileType);
        entity.setCharCount(0);
        entity.setChunkCount(0);
        entity.setStatus(STATUS_PENDING);
        entity.setContentHash(contentHash);
        entity.setRetryCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    public void markProcessing() {
        this.status = STATUS_PROCESSING;
        this.errorMessage = null;
        this.retryCount = (retryCount == null ? 0 : retryCount) + 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void prepareRetry() {
        if (!STATUS_FAILED.equals(status)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "只有失败的文档可以重试");
        }
        this.status = STATUS_PENDING;
        this.errorMessage = null;
        this.charCount = 0;
        this.chunkCount = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void markReady(int charCount, int chunkCount) {
        if (chunkCount <= 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "文档没有产生任何分片");
        }
        this.charCount = charCount;
        this.chunkCount = chunkCount;
        this.status = STATUS_READY;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String message) {
        this.status = STATUS_FAILED;
        this.errorMessage = truncate(message);
        this.updatedAt = LocalDateTime.now();
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "处理失败";
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
