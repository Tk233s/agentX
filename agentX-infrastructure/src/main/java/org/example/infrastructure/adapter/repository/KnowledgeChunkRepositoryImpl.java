package org.example.infrastructure.adapter.repository;

import org.example.domain.rag.adapter.repository.KnowledgeChunkRepository;
import org.example.domain.rag.model.entity.KnowledgeChunkEntity;
import org.example.infrastructure.dao.IKnowledgeChunkDao;
import org.example.infrastructure.dao.po.KnowledgeChunkPO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 知识库分片仓储实现。
 */
@Repository
public class KnowledgeChunkRepositoryImpl implements KnowledgeChunkRepository {

    @Resource
    private IKnowledgeChunkDao knowledgeChunkDao;

    @Override
    public void batchSave(List<KnowledgeChunkEntity> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        knowledgeChunkDao.batchInsert(chunks.stream().map(this::toPO).toList());
    }

    @Override
    public List<KnowledgeChunkEntity> queryByKnowledgeBaseId(String knowledgeBaseId, String userId) {
        List<KnowledgeChunkPO> records = knowledgeChunkDao.queryByKnowledgeBaseId(knowledgeBaseId, userId);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(this::toEntity).toList();
    }

    @Override
    public int countByKnowledgeBaseId(String knowledgeBaseId) {
        return knowledgeChunkDao.countByKnowledgeBaseId(knowledgeBaseId);
    }

    @Override
    public void deleteByDocumentId(String documentId) {
        knowledgeChunkDao.deleteByDocumentId(documentId);
    }

    @Override
    public void deleteByKnowledgeBaseId(String knowledgeBaseId) {
        knowledgeChunkDao.deleteByKnowledgeBaseId(knowledgeBaseId);
    }

    private KnowledgeChunkPO toPO(KnowledgeChunkEntity entity) {
        KnowledgeChunkPO po = new KnowledgeChunkPO();
        po.setId(entity.getId());
        po.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        po.setDocumentId(entity.getDocumentId());
        po.setUserId(entity.getUserId());
        po.setChunkIndex(entity.getChunkIndex());
        po.setContent(entity.getContent());
        po.setEmbedding(entity.getEmbedding());
        po.setCharCount(entity.getCharCount());
        po.setCreatedAt(entity.getCreatedAt());
        return po;
    }

    private KnowledgeChunkEntity toEntity(KnowledgeChunkPO po) {
        if (po == null) {
            return null;
        }
        KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
        entity.setId(po.getId());
        entity.setKnowledgeBaseId(po.getKnowledgeBaseId());
        entity.setDocumentId(po.getDocumentId());
        entity.setUserId(po.getUserId());
        entity.setChunkIndex(po.getChunkIndex());
        entity.setContent(po.getContent());
        entity.setEmbedding(po.getEmbedding());
        entity.setCharCount(po.getCharCount());
        entity.setCreatedAt(po.getCreatedAt());
        return entity;
    }
}
