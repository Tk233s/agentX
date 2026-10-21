package org.example.domain.rag.adapter.port;

import org.example.domain.rag.model.valobj.ChunkMatch;

import java.util.List;

/**
 * 关键词召回端口，当前由 PostgreSQL pg_trgm 实现。
 */
public interface KeywordSearchPort {

    double MIN_KEYWORD_SCORE = 0.20D;

    List<ChunkMatch> search(String knowledgeBaseId, String userId, String query, int topK);
}
