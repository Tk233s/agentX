package org.example.domain.rag.model.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 一次向量化请求。
 *
 * 与 LLMEntity 同样的思路：领域层只说"用哪个模型、哪把密钥、把哪些文本转成向量"，
 * 具体走什么协议、怎么发 HTTP 由基础设施层决定。
 */
@Data
@Builder
public class EmbeddingEntity {

    /** API 密钥 */
    private String apiKey;

    /** 服务地址，OpenAI 兼容协议下形如 https://api.siliconflow.cn/v1 */
    private String baseUrl;

    /** 向量化模型名，如 BAAI/bge-m3 */
    private String model;

    /** 待向量化的文本，按顺序返回 */
    private List<String> texts;
}
