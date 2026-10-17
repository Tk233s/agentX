package org.example.domain.rag.adapter.port;

import org.example.domain.rag.model.entity.EmbeddingEntity;

import java.util.List;

/**
 * 文本向量化端口。
 *
 * 返回结果与请求的 texts 一一对应，顺序一致。
 * 同一批次内的文本必须用同一个模型处理，返回的向量才在同一坐标系里。
 */
public interface EmbeddingPort {

    List<float[]> embed(EmbeddingEntity entity);
}
