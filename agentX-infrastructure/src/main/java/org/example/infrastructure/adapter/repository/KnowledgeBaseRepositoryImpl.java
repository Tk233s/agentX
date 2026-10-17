package org.example.infrastructure.adapter.repository;

import org.example.domain.rag.adapter.repository.KnowledgeBaseRepository;
import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.example.infrastructure.dao.IKnowledgeBaseDao;
import org.example.infrastructure.dao.po.KnowledgeBasePO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 知识库仓储实现。
 */
@Repository
public class KnowledgeBaseRepositoryImpl implements KnowledgeBaseRepository {

    @Resource
    private IKnowledgeBaseDao knowledgeBaseDao;

    @Override
    public void save(KnowledgeBaseEntity knowledgeBase) {
        knowledgeBaseDao.insert(toPO(knowledgeBase));
    }

    @Override
    public KnowledgeBaseEntity queryById(String id) {
        return toEntity(knowledgeBaseDao.queryById(id));
    }

    @Override
    public List<KnowledgeBaseEntity> queryByUserId(String userId) {
        return toEntityList(knowledgeBaseDao.queryByUserId(userId));
    }

    @Override
    public List<KnowledgeBaseEntity> queryByIdsAndUserId(List<String> ids, String userId) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return toEntityList(knowledgeBaseDao.queryByIdsAndUserId(ids, userId));
    }

    @Override
    public void deleteById(String id) {
        knowledgeBaseDao.deleteById(id);
    }

    private List<KnowledgeBaseEntity> toEntityList(List<KnowledgeBasePO> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(this::toEntity).toList();
    }

    private KnowledgeBasePO toPO(KnowledgeBaseEntity entity) {
        KnowledgeBasePO po = new KnowledgeBasePO();
        po.setId(entity.getId());
        po.setUserId(entity.getUserId());
        po.setName(entity.getName());
        po.setDescription(entity.getDescription());
        po.setApiKeyId(entity.getApiKeyId());
        po.setEmbeddingModel(entity.getEmbeddingModel());
        po.setDimension(entity.getDimension());
        po.setChunkSize(entity.getChunkSize());
        po.setChunkOverlap(entity.getChunkOverlap());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        return po;
    }

    private KnowledgeBaseEntity toEntity(KnowledgeBasePO po) {
        if (po == null) {
            return null;
        }
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setId(po.getId());
        entity.setUserId(po.getUserId());
        entity.setName(po.getName());
        entity.setDescription(po.getDescription());
        entity.setApiKeyId(po.getApiKeyId());
        entity.setEmbeddingModel(po.getEmbeddingModel());
        entity.setDimension(po.getDimension());
        entity.setChunkSize(po.getChunkSize());
        entity.setChunkOverlap(po.getChunkOverlap());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        return entity;
    }
}
