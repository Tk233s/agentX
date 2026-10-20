package org.example.trigger.http;

import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.trigger.dto.rag.KnowledgeBaseReq;
import org.example.trigger.dto.rag.KnowledgeBaseRes;
import org.example.trigger.dto.rag.KnowledgeDocumentRes;
import org.example.trigger.dto.rag.KnowledgeSearchRes;

/**
 * 知识库 HTTP 层转换器。
 */
public class RagAssembler {

    /** 入参 DTO -> 领域实体（初始状态由领域工厂统一生成） */
    public static KnowledgeBaseEntity toEntity(KnowledgeBaseReq req, String userId) {
        return KnowledgeBaseEntity.createNew(
                userId,
                req.getName(),
                req.getDescription(),
                req.getApiKeyId(),
                req.getEmbeddingModel(),
                req.getDimension(),
                req.getChunkSize(),
                req.getChunkOverlap(),
                req.getMinScore(),
                req.getTopK());
    }

    /** 领域实体 -> 出参 DTO */
    public static KnowledgeBaseRes toRes(KnowledgeBaseEntity entity, int chunkCount) {
        KnowledgeBaseRes res = new KnowledgeBaseRes();
        res.setId(entity.getId());
        res.setName(entity.getName());
        res.setDescription(entity.getDescription());
        res.setApiKeyId(entity.getApiKeyId());
        res.setEmbeddingModel(entity.getEmbeddingModel());
        res.setDimension(entity.getDimension());
        res.setChunkSize(entity.getChunkSize());
        res.setChunkOverlap(entity.getChunkOverlap());
        res.setMinScore(entity.effectiveMinScore());
        res.setTopK(entity.effectiveTopK());
        res.setChunkCount(chunkCount);
        res.setCreateTime(entity.getCreatedAt());
        res.setUpdateTime(entity.getUpdatedAt());
        return res;
    }

    /** 文档领域实体 -> 出参 DTO */
    public static KnowledgeDocumentRes toRes(KnowledgeDocumentEntity entity) {
        KnowledgeDocumentRes res = new KnowledgeDocumentRes();
        res.setId(entity.getId());
        res.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        res.setFileName(entity.getFileName());
        res.setFileSize(entity.getFileSize());
        res.setFileType(entity.getFileType());
        res.setCharCount(entity.getCharCount());
        res.setChunkCount(entity.getChunkCount());
        res.setStatus(entity.getStatus());
        res.setErrorMessage(entity.getErrorMessage());
        res.setCreateTime(entity.getCreatedAt());
        res.setUpdateTime(entity.getUpdatedAt());
        return res;
    }

    /** 检索命中 -> 出参 DTO */
    public static KnowledgeSearchRes toRes(ChunkMatch match) {
        KnowledgeSearchRes res = new KnowledgeSearchRes();
        res.setChunkId(match.chunkId());
        res.setDocumentId(match.documentId());
        res.setDocumentName(match.documentName());
        res.setContent(match.content());
        res.setScore(match.score());
        return res;
    }
}
