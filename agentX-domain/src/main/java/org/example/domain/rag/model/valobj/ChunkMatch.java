package org.example.domain.rag.model.valobj;

/**
 * 一次检索命中的片段。
 *
 * score 是相似度得分，越大越相近；documentName 带上是为了让模型能说明出处。
 */
public record ChunkMatch(
        String chunkId,
        String documentId,
        String documentName,
        String content,
        double score) {
}
