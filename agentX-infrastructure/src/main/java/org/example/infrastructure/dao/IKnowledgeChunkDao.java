package org.example.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.dao.po.KnowledgeChunkPO;

import java.util.List;

/**
 * 知识库分片 DAO。
 */
@Mapper
public interface IKnowledgeChunkDao {

    void batchInsert(@Param("chunks") List<KnowledgeChunkPO> chunks);

    List<KnowledgeChunkPO> queryByKnowledgeBaseId(
            @Param("knowledgeBaseId") String knowledgeBaseId,
            @Param("userId") String userId);

    int countByKnowledgeBaseId(@Param("knowledgeBaseId") String knowledgeBaseId);

    void deleteByDocumentId(@Param("documentId") String documentId);

    void deleteByKnowledgeBaseId(@Param("knowledgeBaseId") String knowledgeBaseId);
}
