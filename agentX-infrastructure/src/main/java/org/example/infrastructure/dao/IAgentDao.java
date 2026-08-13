package org.example.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.dao.po.AgentPO;

/**
 * 智能体 DAO 接口
 */
@Mapper
public interface IAgentDao {

    /**
     * 新增智能体
     * @param agentPO 智能体持久化对象
     */
    void insert(AgentPO agentPO);

    /**
     * 根据ID查询智能体
     * @param id 智能体ID
     * @return 智能体持久化对象，不存在返回null
     */
    AgentPO queryById(String id);
}
