package org.example.domain.rag.model.valobj;

/**
 * 一次检索命中的片段。
 *
 * vectorScore、keywordScore 是两个通道各自的分值；fusedScore 才是 RRF 最终排序依据。
 * score 作为兼容字段保留，值等于 fusedScore。
 */
public record ChunkMatch(
        String knowledgeBaseId,
        String chunkId,
        String documentId,
        String documentName,
        String content,
        int chunkIndex,
        Integer pageNo,
        String sectionTitle,
        double vectorScore,
        double keywordScore,
        double fusedScore,
        String matchedBy) {

    /**
     * 兼容 v1.1 的纯向量检索结果。
     */
    public ChunkMatch(
            String knowledgeBaseId,
            String chunkId,
            String documentId,
            String documentName,
            String content,
            double score) {
        this(
                knowledgeBaseId,
                chunkId,
                documentId,
                documentName,
                content,
                0,
                null,
                null,
                score,
                0D,
                score,
                "VECTOR");
    }

    public double score() {
        return fusedScore;
    }
}
