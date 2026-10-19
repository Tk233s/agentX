package org.example.domain.rag.model.valobj;

/**
 * 一次检索命中的片段。
 *
 * score 是相似度得分，越大越相近；knowledgeBaseId 与 documentName 带上是为了区分多库同名文档，
 * 也让模型和前端都能说明出处。
 */
public record ChunkMatch(
        String knowledgeBaseId,
        String chunkId,
        String documentId,
        String documentName,
        String content,
        double score) {
}
