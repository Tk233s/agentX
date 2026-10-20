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
    public static final double DEFAULT_MIN_SCORE = 0.55D;
    public static final int DEFAULT_TOP_K = 3;

    /** 单个分片的字符数上限，防止把整篇文档塞进一个分片。 */
    private static final int MAX_CHUNK_SIZE = 4000;
    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_TOP_K = 20;

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

    /** 对话检索的最低相似度，低于该值的片段不会进入模型上下文 */
    private Double minScore;

    /** 每个知识库单次最多贡献的检索片段数 */
    private Integer topK;

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
            Integer chunkOverlap,
            Double minScore,
            Integer topK) {
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
        entity.setMinScore(minScore == null ? DEFAULT_MIN_SCORE : minScore);
        entity.setTopK(topK == null ? DEFAULT_TOP_K : topK);
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
        validateRetrievalConfig(minScore, topK);
    }

    /**
     * 更新对话检索配置；历史数据缺字段时回退到默认值。
     */
    public void updateRetrievalConfig(Double minScore, Integer topK) {
        double resolvedMinScore = minScore == null ? DEFAULT_MIN_SCORE : minScore;
        int resolvedTopK = topK == null ? DEFAULT_TOP_K : topK;
        validateRetrievalConfig(resolvedMinScore, resolvedTopK);
        this.minScore = resolvedMinScore;
        this.topK = resolvedTopK;
        this.updatedAt = LocalDateTime.now();
    }

    public double effectiveMinScore() {
        return minScore == null ? DEFAULT_MIN_SCORE : minScore;
    }

    public int effectiveTopK() {
        return topK == null ? DEFAULT_TOP_K : topK;
    }

    private void validateRetrievalConfig(Double minScore, Integer topK) {
        if (minScore == null || !Double.isFinite(minScore) || minScore < 0D || minScore > 1D) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "最低相似度必须在 0 到 1 之间");
        }
        if (topK == null || topK <= 0 || topK > MAX_TOP_K) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "TopK 必须在 1 到 " + MAX_TOP_K + " 之间");
        }
    }
}
