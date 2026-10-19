package org.example.infrastructure.adapter.repository;

import org.example.domain.rag.adapter.repository.KnowledgeDocumentRepository;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库文档仓储的 PostgreSQL 实现。
 */
@Repository
public class PgKnowledgeDocumentRepositoryImpl implements KnowledgeDocumentRepository {

    private static final String COLUMNS = "id, knowledge_base_id, user_id, file_name, file_path, file_size, "
            + "file_type, char_count, chunk_count, status, error_message, created_at, updated_at";

    private final JdbcTemplate jdbcTemplate;

    public PgKnowledgeDocumentRepositoryImpl(@Qualifier("ragJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(KnowledgeDocumentEntity document) {
        jdbcTemplate.update(
                "INSERT INTO knowledge_document (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                document.getId(),
                document.getKnowledgeBaseId(),
                document.getUserId(),
                document.getFileName(),
                document.getFilePath(),
                document.getFileSize(),
                document.getFileType(),
                document.getCharCount(),
                document.getChunkCount(),
                document.getStatus(),
                document.getErrorMessage(),
                toTimestamp(document.getCreatedAt()),
                toTimestamp(document.getUpdatedAt()));
    }

    @Override
    public void updateProcessResult(KnowledgeDocumentEntity document) {
        jdbcTemplate.update(
                "UPDATE knowledge_document SET char_count = ?, chunk_count = ?, status = ?,"
                        + " error_message = ?, updated_at = ? WHERE id = ?",
                document.getCharCount(),
                document.getChunkCount(),
                document.getStatus(),
                document.getErrorMessage(),
                toTimestamp(document.getUpdatedAt()),
                document.getId());
    }

    @Override
    public KnowledgeDocumentEntity queryById(String id) {
        List<KnowledgeDocumentEntity> records =
                jdbcTemplate.query("SELECT " + COLUMNS + " FROM knowledge_document WHERE id = ?", ROW_MAPPER, id);
        return records.isEmpty() ? null : records.get(0);
    }

    @Override
    public List<KnowledgeDocumentEntity> queryByKnowledgeBaseId(String knowledgeBaseId) {
        return jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM knowledge_document WHERE knowledge_base_id = ?"
                        + " ORDER BY created_at DESC",
                ROW_MAPPER, knowledgeBaseId);
    }

    @Override
    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM knowledge_document WHERE id = ?", id);
    }

    @Override
    public void deleteByKnowledgeBaseId(String knowledgeBaseId) {
        jdbcTemplate.update("DELETE FROM knowledge_document WHERE knowledge_base_id = ?", knowledgeBaseId);
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static final RowMapper<KnowledgeDocumentEntity> ROW_MAPPER = (rs, rowNum) -> {
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setId(rs.getString("id"));
        entity.setKnowledgeBaseId(rs.getString("knowledge_base_id"));
        entity.setUserId(rs.getString("user_id"));
        entity.setFileName(rs.getString("file_name"));
        entity.setFilePath(rs.getString("file_path"));
        entity.setFileSize(rs.getLong("file_size"));
        entity.setFileType(rs.getString("file_type"));
        entity.setCharCount(rs.getInt("char_count"));
        entity.setChunkCount(rs.getInt("chunk_count"));
        entity.setStatus(rs.getString("status"));
        entity.setErrorMessage(rs.getString("error_message"));
        entity.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        entity.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return entity;
    };
}
