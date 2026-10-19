package org.example.infrastructure.adapter.repository;

import org.example.domain.rag.adapter.repository.KnowledgeChunkRepository;
import org.example.domain.rag.model.entity.KnowledgeChunkEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库分片仓储的 PostgreSQL 实现。
 *
 * 写入时把 JSON 数组文本 CAST 成 vector；读取时用 embedding::text 还原成同样的文本，
 * 这样上层的 EmbeddingCodec 不用感知存储格式的变化。
 */
@Repository
public class PgKnowledgeChunkRepositoryImpl implements KnowledgeChunkRepository {

    private static final String COLUMNS =
            "id, knowledge_base_id, document_id, user_id, chunk_index, content, embedding::text AS embedding,"
                    + " char_count, created_at";

    private final JdbcTemplate jdbcTemplate;

    public PgKnowledgeChunkRepositoryImpl(@Qualifier("ragJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void batchSave(List<KnowledgeChunkEntity> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(
                "INSERT INTO knowledge_chunk"
                        + " (id, knowledge_base_id, document_id, user_id, chunk_index, content, embedding,"
                        + " char_count, created_at) VALUES (?,?,?,?,?,?, CAST(? AS vector), ?, ?)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        KnowledgeChunkEntity chunk = chunks.get(i);
                        ps.setString(1, chunk.getId());
                        ps.setString(2, chunk.getKnowledgeBaseId());
                        ps.setString(3, chunk.getDocumentId());
                        ps.setString(4, chunk.getUserId());
                        ps.setInt(5, chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex());
                        ps.setString(6, chunk.getContent());
                        ps.setString(7, chunk.getEmbedding());
                        ps.setInt(8, chunk.getCharCount() == null ? 0 : chunk.getCharCount());
                        ps.setTimestamp(9, toTimestamp(chunk.getCreatedAt()));
                    }

                    @Override
                    public int getBatchSize() {
                        return chunks.size();
                    }
                });
    }

    @Override
    public List<KnowledgeChunkEntity> queryByKnowledgeBaseId(String knowledgeBaseId, String userId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM knowledge_chunk"
                        + " WHERE knowledge_base_id = ? AND user_id = ? ORDER BY chunk_index",
                ROW_MAPPER, knowledgeBaseId, userId);
    }

    @Override
    public int countByKnowledgeBaseId(String knowledgeBaseId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunk WHERE knowledge_base_id = ?", Integer.class, knowledgeBaseId);
        return count == null ? 0 : count;
    }

    @Override
    public void deleteByDocumentId(String documentId) {
        jdbcTemplate.update("DELETE FROM knowledge_chunk WHERE document_id = ?", documentId);
    }

    @Override
    public void deleteByKnowledgeBaseId(String knowledgeBaseId) {
        jdbcTemplate.update("DELETE FROM knowledge_chunk WHERE knowledge_base_id = ?", knowledgeBaseId);
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static final RowMapper<KnowledgeChunkEntity> ROW_MAPPER = (rs, rowNum) -> {
        KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
        entity.setId(rs.getString("id"));
        entity.setKnowledgeBaseId(rs.getString("knowledge_base_id"));
        entity.setDocumentId(rs.getString("document_id"));
        entity.setUserId(rs.getString("user_id"));
        entity.setChunkIndex(rs.getInt("chunk_index"));
        entity.setContent(rs.getString("content"));
        entity.setEmbedding(rs.getString("embedding"));
        entity.setCharCount(rs.getInt("char_count"));
        entity.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        return entity;
    };
}
