package org.example.domain.rag.model.entity;

import lombok.Data;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 知识库领域实体。
 *
 * 一个知识库固定用同一个向量化模型，仓库里的分片才可比；
 * 换了模型等于换了坐标系，历史向量必须重新生成才有意义。
 */
@Data
public class KnowledgeBaseEntity {

    public static final int DEFAULT_DIMENSION = 1024;
    public static final int DEFAULT_CHUNK_SIZE = 500;
    public static final int DEFAULT_CHUNK_OVERLAP = 50;

    /** 单个分片的字符数上限，防止把整篇文档塞进一个分片。 */
    private static final int MAX_CHUNK_SIZE = 4000;
    private static final int MAX_NAME_LENGTH = 128;

    /** 主键ID */
    private String id;

    /** 所属用户ID */
    private String userId;

    /** 知识库名称 */
    private String name;

    /** 说明 */
    private String description;

    /** 用于向量化的 API 密钥ID，复用 apiKey 模块，provider 走 openai 兼容协议 */
    private String apiKeyId;

    /** 向量化模型名，如 BAAI/bge-m3 */
    private String embeddingModel;

    /** 向量维度，需与模型一致 */
    private Integer dimension;

    /** 分片长度（字符） */
    private Integer chunkSize;

    /** 相邻分片重叠长度（字符），用于避免语义被切断 */
    private Integer chunkOverlap;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KnowledgeBaseEntity createNew(
            String userId,
            String name,
            String description,
            String apiKeyId,
            String embeddingModel,
            Integer dimension,
            Integer chunkSize,
            Integer chunkOverlap) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setUserId(userId);
        entity.setName(name);
        entity.setDescription(description);
        entity.setApiKeyId(apiKeyId);
        entity.setEmbeddingModel(embeddingModel);
        entity.setDimension(dimension == null ? DEFAULT_DIMENSION : dimension);
        entity.setChunkSize(chunkSize == null ? DEFAULT_CHUNK_SIZE : chunkSize);
        entity.setChunkOverlap(chunkOverlap == null ? DEFAULT_CHUNK_OVERLAP : chunkOverlap);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    /**
     * 业务规则校验，同时把名称和说明去掉首尾空白。
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "知识库名称不能为空");
        }
        name = name.trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "知识库名称不能超过 " + MAX_NAME_LENGTH + " 个字符");
        }
        if (apiKeyId == null || apiKeyId.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "请选择用于向量化的API密钥");
        }
        apiKeyId = apiKeyId.trim();
        if (embeddingModel == null || embeddingModel.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化模型不能为空");
        }
        embeddingModel = embeddingModel.trim();
        if (description != null) {
            description = description.trim();
            if (description.isEmpty()) {
                description = null;
            }
        }
        if (dimension == null || dimension <= 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量维度必须大于0");
        }
        if (chunkSize == null || chunkSize <= 0 || chunkSize > MAX_CHUNK_SIZE) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "分片长度必须在 1 到 " + MAX_CHUNK_SIZE + " 之间");
        }
        if (chunkOverlap == null || chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "分片重叠长度必须大于等于0且小于分片长度");
        }
    }
}
