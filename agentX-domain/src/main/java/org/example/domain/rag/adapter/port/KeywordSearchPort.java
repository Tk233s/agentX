package org.example.domain.rag.adapter.port;

import org.example.domain.rag.model.valobj.ChunkMatch;

import java.util.List;

/**
 * 关键词召回端口，当前由 PostgreSQL pg_trgm 实现。
 */
public interface KeywordSearchPort {

    /**
     * 关键词召回的最低分。pg_trgm 的 word_similarity 对中文偏松，0.20 时几乎不返回空集，
     * 只共享几个字的无关分片也会进入候选；实测上调到 0.40 后才具备区分度。
     */
    double MIN_KEYWORD_SCORE = 0.40D;

    List<ChunkMatch> search(String knowledgeBaseId, String userId, String query, int topK);
}
