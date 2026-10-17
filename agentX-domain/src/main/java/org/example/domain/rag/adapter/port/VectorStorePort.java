package org.example.domain.rag.adapter.port;

import org.example.domain.rag.model.valobj.ChunkMatch;

import java.util.List;

/**
 * 向量检索端口。
 *
 * 当前实现是"取出全量分片在应用层算相似度"，
 * 换成 pgvector、Milvus 这类专用向量库时，只需要换掉这个端口的实现，
 * 检索调用方和入库链路都不用动。
 */
public interface VectorStorePort {

    /**
     * 返回与查询向量最相近的 topK 个分片，按相似度从高到低排列。
     *
     * @param queryVector 查询向量，维度需与入库时一致
     */
    List<ChunkMatch> search(String knowledgeBaseId, String userId, float[] queryVector, int topK);
}
