package org.example.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.dao.po.KnowledgeDocumentPO;

import java.util.List;

/**
 * 知识库文档 DAO。
 */
@Mapper
public interface IKnowledgeDocumentDao {

    void insert(KnowledgeDocumentPO documentPO);

    void updateProcessResult(KnowledgeDocumentPO documentPO);

    KnowledgeDocumentPO queryById(@Param("id") String id);

    List<KnowledgeDocumentPO> queryByKnowledgeBaseId(@Param("knowledgeBaseId") String knowledgeBaseId);

    void deleteById(@Param("id") String id);

    void deleteByKnowledgeBaseId(@Param("knowledgeBaseId") String knowledgeBaseId);
}
