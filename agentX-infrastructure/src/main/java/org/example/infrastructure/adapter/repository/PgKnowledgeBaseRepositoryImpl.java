package org.example.infrastructure.adapter.repository;

import org.example.domain.rag.adapter.repository.KnowledgeBaseRepository;
import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 知识库仓储的 PostgreSQL 实现。
 */
@Repository
public class PgKnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {

    private static final String COLUMNS = "id, user_id, name, description, api_key_id, embedding_model, "
            + "dimension, chunk_size, chunk_overlap, min_score, top_k, created_at, updated_at";

    private final JdbcTemplate jdbcTemplate;

    public PgKnowledgeBaseRepositoryImpl(@Qualifier("ragJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(KnowledgeBaseEntity knowledgeBase) {
        jdbcTemplate.update(
                "INSERT INTO knowledge_base (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                knowledgeBase.getId(),
                knowledgeBase.getUserId(),
                knowledgeBase.getName(),
                knowledgeBase.getDescription(),
                knowledgeBase.getApiKeyId(),
                knowledgeBase.getEmbeddingModel(),
                knowledgeBase.getDimension(),
                knowledgeBase.getChunkSize(),
                knowledgeBase.getChunkOverlap(),
                knowledgeBase.effectiveMinScore(),
                knowledgeBase.effectiveTopK(),
                toTimestamp(knowledgeBase.getCreatedAt()),
                toTimestamp(knowledgeBase.getUpdatedAt()));
    }

    @Override
    public KnowledgeBaseEntity queryById(String id) {
        List<KnowledgeBaseEntity> records =
                jdbcTemplate.query("SELECT " + COLUMNS + " FROM knowledge_base WHERE id = ?", ROW_MAPPER, id);
        return records.isEmpty() ? null : records.get(0);
    }

    @Override
    public List<KnowledgeBaseEntity> queryByUserId(String userId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM knowledge_base WHERE user_id = ? ORDER BY created_at DESC",
                ROW_MAPPER, userId);
    }

    @Override
    public List<KnowledgeBaseEntity> queryByIdsAndUserId(List<String> ids, String userId) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Object> args = new ArrayList<>(ids.size() + 1);
        args.add(userId);
        args.addAll(ids);
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM knowledge_base WHERE user_id = ? AND id IN (" + placeholders + ")"
                        + " ORDER BY created_at DESC",
                ROW_MAPPER, args.toArray());
    }

    @Override
    public void updateRetrievalConfig(String id, String userId, double minScore, int topK) {
        jdbcTemplate.update(
                "UPDATE knowledge_base SET min_score = ?, top_k = ?, updated_at = ?"
                        + " WHERE id = ? AND user_id = ?",
                minScore,
                topK,
                toTimestamp(java.time.LocalDateTime.now()),
                id,
                userId);
    }

    @Override
    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM knowledge_base WHERE id = ?", id);
    }

    private static Timestamp toTimestamp(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static final RowMapper<KnowledgeBaseEntity> ROW_MAPPER = (rs, rowNum) -> {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(rs.getString("id"));
        entity.setUserId(rs.getString("user_id"));
        entity.setName(rs.getString("name"));
        entity.setDescription(rs.getString("description"));
        entity.setApiKeyId(rs.getString("api_key_id"));
        entity.setEmbeddingModel(rs.getString("embedding_model"));
        entity.setDimension(rs.getInt("dimension"));
        entity.setChunkSize(rs.getInt("chunk_size"));
        entity.setChunkOverlap(rs.getInt("chunk_overlap"));
        entity.setMinScore(rs.getDouble("min_score"));
        entity.setTopK(rs.getInt("top_k"));
        entity.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        entity.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return entity;
    };
}
