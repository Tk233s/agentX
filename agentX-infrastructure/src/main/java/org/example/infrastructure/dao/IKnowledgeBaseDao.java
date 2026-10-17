package org.example.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.infrastructure.dao.po.KnowledgeBasePO;

import java.util.List;

/**
 * 知识库 DAO。
 */
@Mapper
public interface IKnowledgeBaseDao {

    void insert(KnowledgeBasePO knowledgeBasePO);

    KnowledgeBasePO queryById(@Param("id") String id);

    List<KnowledgeBasePO> queryByUserId(@Param("userId") String userId);

    List<KnowledgeBasePO> queryByIdsAndUserId(
            @Param("ids") List<String> ids,
            @Param("userId") String userId);

    void deleteById(@Param("id") String id);
}
