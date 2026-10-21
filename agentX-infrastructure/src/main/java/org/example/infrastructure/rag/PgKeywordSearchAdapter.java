package org.example.infrastructure.rag;

import org.example.domain.rag.adapter.port.KeywordSearchPort;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 基于 PostgreSQL pg_trgm 的关键词召回。
 *
 * 精确包含查询词的分片直接得到 1 分；其余候选使用 word_similarity 评分。
 */
@Repository
public class PgKeywordSearchAdapter implements KeywordSearchPort {

    private static final String SEARCH_SQL = """
            SELECT c.id,
                   c.knowledge_base_id,
                   c.document_id,
                   d.file_name,
                   c.content,
                   c.chunk_index,
                   c.page_no,
                   c.section_title,
                   CASE
                       WHEN c.content ILIKE ? THEN 1.0
                       ELSE word_similarity(?, c.content)
                   END AS keyword_score
            FROM knowledge_chunk c
            JOIN knowledge_document d ON d.id = c.document_id
            WHERE c.knowledge_base_id = ?
              AND c.user_id = ?
              AND d.status = 'READY'
              AND (
                    c.content ILIKE ?
                    OR word_similarity(?, c.content) >= ?
              )
            ORDER BY keyword_score DESC, c.chunk_index
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PgKeywordSearchAdapter(@Qualifier("ragJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ChunkMatch> search(String knowledgeBaseId, String userId, String query, int topK) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        String contains = "%" + normalized + "%";
        int limit = topK <= 0 ? 4 : topK;
        return jdbcTemplate.query(
                SEARCH_SQL,
                (rs, rowNum) -> new ChunkMatch(
                        rs.getString("knowledge_base_id"),
                        rs.getString("id"),
                        rs.getString("document_id"),
                        rs.getString("file_name"),
                        rs.getString("content"),
                        rs.getInt("chunk_index"),
                        (Integer) rs.getObject("page_no"),
                        rs.getString("section_title"),
                        0D,
                        rs.getDouble("keyword_score"),
                        0D,
                        "KEYWORD"),
                contains,
                normalized,
                knowledgeBaseId,
                userId,
                contains,
                normalized,
                KeywordSearchPort.MIN_KEYWORD_SCORE,
                limit);
    }
}
