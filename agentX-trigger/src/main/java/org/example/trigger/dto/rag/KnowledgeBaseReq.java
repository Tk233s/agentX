package org.example.trigger.dto.rag;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 知识库 HTTP 入参 DTO（创建）
 *
 * 分片参数留空时由领域实体填默认值，前端不需要理解这些细节。
 */
@Data
public class KnowledgeBaseReq {

    /** 知识库名称 */
    @NotBlank(message = "知识库名称不能为空")
    private String name;

    /** 说明 */
    private String description;

    /** 用于向量化的 API 密钥ID（复用 apiKey 模块） */
    @NotBlank(message = "请选择用于向量化的API密钥")
    private String apiKeyId;

    /** 向量化模型名，如 BAAI/bge-m3 */
    @NotBlank(message = "向量化模型不能为空")
    private String embeddingModel;

    /** 向量维度；留空按 1024 处理 */
    private Integer dimension;

    /** 分片长度（字符）；留空按 500 处理 */
    private Integer chunkSize;

    /** 相邻分片重叠长度（字符）；留空按 50 处理 */
    private Integer chunkOverlap;
}
