package org.example.infrastructure.rag;

import org.example.domain.rag.adapter.port.VectorStorePort;
import org.example.domain.rag.model.valobj.EmbeddingCodec;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 基于 pgvector 的向量检索实现。
 *
 * 相似度计算下沉到数据库：&lt;=&gt; 返回余弦距离（0~2，越小越相近），
 * 所以分数取 1 - distance，排序仍按距离升序，与 MySQL 版的语义保持一致。
 */
@Repository
public class PgVectorStoreAdapter implements VectorStorePort {

    private static final String SEARCH_SQL = """
            SELECT c.id,
                   c.knowledge_base_id,
                   c.document_id,
                   d.file_name,
                   c.content,
                   1 - (c.embedding <=> CAST(? AS vector)) AS score
            FROM knowledge_chunk c
            JOIN knowledge_document d ON d.id = c.document_id
            WHERE c.knowledge_base_id = ? AND c.user_id = ?
            ORDER BY c.embedding <=> CAST(? AS vector)
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PgVectorStoreAdapter(@Qualifier("ragJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ChunkMatch> search(String knowledgeBaseId, String userId, float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "检索向量不能为空");
        }
        // 与落库时保持同一种字面量格式，pgvector 直接按 vector 解析
        String literal = EmbeddingCodec.encode(queryVector);
        int limit = topK <= 0 ? 4 : topK;
        return jdbcTemplate.query(
                SEARCH_SQL,
                (rs, rowNum) -> new ChunkMatch(
                        rs.getString("knowledge_base_id"),
                        rs.getString("id"),
                        rs.getString("document_id"),
                        rs.getString("file_name"),
                        rs.getString("content"),
                        rs.getDouble("score")),
                literal, knowledgeBaseId, userId, literal, limit);
    }
}
