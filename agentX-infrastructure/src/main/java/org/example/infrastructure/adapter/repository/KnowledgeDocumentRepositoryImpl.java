package org.example.infrastructure.adapter.repository;

import org.example.domain.rag.adapter.repository.KnowledgeDocumentRepository;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.infrastructure.dao.IKnowledgeDocumentDao;
import org.example.infrastructure.dao.po.KnowledgeDocumentPO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 知识库文档仓储实现。
 */
@Repository
public class KnowledgeDocumentRepositoryImpl implements KnowledgeDocumentRepository {

    @Resource
    private IKnowledgeDocumentDao knowledgeDocumentDao;

    @Override
    public void save(KnowledgeDocumentEntity document) {
        knowledgeDocumentDao.insert(toPO(document));
    }

    @Override
    public void updateProcessResult(KnowledgeDocumentEntity document) {
        knowledgeDocumentDao.updateProcessResult(toPO(document));
    }

    @Override
    public KnowledgeDocumentEntity queryById(String id) {
        return toEntity(knowledgeDocumentDao.queryById(id));
    }

    @Override
    public List<KnowledgeDocumentEntity> queryByKnowledgeBaseId(String knowledgeBaseId) {
        List<KnowledgeDocumentPO> records = knowledgeDocumentDao.queryByKnowledgeBaseId(knowledgeBaseId);
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteById(String id) {
        knowledgeDocumentDao.deleteById(id);
    }

    @Override
    public void deleteByKnowledgeBaseId(String knowledgeBaseId) {
        knowledgeDocumentDao.deleteByKnowledgeBaseId(knowledgeBaseId);
    }

    private KnowledgeDocumentPO toPO(KnowledgeDocumentEntity entity) {
        KnowledgeDocumentPO po = new KnowledgeDocumentPO();
        po.setId(entity.getId());
        po.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        po.setUserId(entity.getUserId());
        po.setFileName(entity.getFileName());
        po.setFilePath(entity.getFilePath());
        po.setFileSize(entity.getFileSize());
        po.setFileType(entity.getFileType());
        po.setCharCount(entity.getCharCount());
        po.setChunkCount(entity.getChunkCount());
        po.setStatus(entity.getStatus());
        po.setErrorMessage(entity.getErrorMessage());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        return po;
    }

    private KnowledgeDocumentEntity toEntity(KnowledgeDocumentPO po) {
        if (po == null) {
            return null;
        }
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setId(po.getId());
        entity.setKnowledgeBaseId(po.getKnowledgeBaseId());
        entity.setUserId(po.getUserId());
        entity.setFileName(po.getFileName());
        entity.setFilePath(po.getFilePath());
        entity.setFileSize(po.getFileSize());
        entity.setFileType(po.getFileType());
        entity.setCharCount(po.getCharCount());
        entity.setChunkCount(po.getChunkCount());
        entity.setStatus(po.getStatus());
        entity.setErrorMessage(po.getErrorMessage());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        return entity;
    }
}
