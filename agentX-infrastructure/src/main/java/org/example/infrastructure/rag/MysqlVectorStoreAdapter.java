package org.example.infrastructure.rag;

import org.example.domain.rag.adapter.port.VectorStorePort;
import org.example.domain.rag.adapter.repository.KnowledgeChunkRepository;
import org.example.domain.rag.adapter.repository.KnowledgeDocumentRepository;
import org.example.domain.rag.model.entity.KnowledgeChunkEntity;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.model.valobj.EmbeddingCodec;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 MySQL 的向量检索实现。
 *
 * 存储层不认识"向量"这个概念，它只存了一串 JSON 数组文本；
 * 真正把文本还原成向量、算余弦、排序，全部发生在应用层这里。
 *
 * 这是"零部署"路线的实现：不需要 pgvector、Milvus 那类外部组件，
 * 代价是每次检索要把整库分片捞进内存——数据量上来之后，
 * 换实现只需要再写一个 VectorStorePort 的实现类，检索调用方不用动。
 */
@Repository
public class MysqlVectorStoreAdapter implements VectorStorePort {

    private static final Logger log = LoggerFactory.getLogger(MysqlVectorStoreAdapter.class);

    @Resource
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Resource
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Override
    public List<ChunkMatch> search(String knowledgeBaseId, String userId, float[] queryVector, int topK) {
        if (queryVector == null || queryVector.length == 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "检索向量不能为空");
        }
        List<KnowledgeChunkEntity> chunks =
                knowledgeChunkRepository.queryByKnowledgeBaseId(knowledgeBaseId, userId);
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        // 文档名一次性查回来做成 Map，避免每条分片都回表一次
        Map<String, String> documentNames = loadDocumentNames(knowledgeBaseId);

        List<ChunkMatch> matches = new ArrayList<>(chunks.size());
        int skipped = 0;
        for (KnowledgeChunkEntity chunk : chunks) {
            float[] chunkVector;
            try {
                chunkVector = EmbeddingCodec.decode(chunk.getEmbedding());
            } catch (RuntimeException e) {
                skipped++;
                continue;
            }
            if (chunkVector.length != queryVector.length) {
                skipped++;
                continue;
            }
            matches.add(new ChunkMatch(
                    chunk.getId(),
                    chunk.getDocumentId(),
                    documentNames.getOrDefault(chunk.getDocumentId(), "未知文档"),
                    chunk.getContent(),
                    VectorMath.cosine(queryVector, chunkVector)));
        }
        if (skipped > 0) {
            log.warn("检索时跳过了 {} 个分片（向量缺失或维度与查询不一致），knowledgeBaseId={}",
                    skipped, knowledgeBaseId);
        }

        matches.sort((left, right) -> Double.compare(right.score(), left.score()));
        if (topK > 0 && matches.size() > topK) {
            return new ArrayList<>(matches.subList(0, topK));
        }
        return matches;
    }

    private Map<String, String> loadDocumentNames(String knowledgeBaseId) {
        List<KnowledgeDocumentEntity> documents =
                knowledgeDocumentRepository.queryByKnowledgeBaseId(knowledgeBaseId);
        Map<String, String> names = new HashMap<>();
        if (documents == null) {
            return names;
        }
        for (KnowledgeDocumentEntity document : documents) {
            names.put(document.getId(), document.getFileName());
        }
        return names;
    }
}
