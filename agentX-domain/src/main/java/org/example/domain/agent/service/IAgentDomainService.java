package org.example.domain.agent.service;

import org.example.domain.agent.model.entity.AgentEntity;

/**
 * 智能体领域服务实接口
 */
public interface IAgentDomainService {

    /**
     * 创建智能体
     * @param agent 待创建的智能体实体
     * @return 已落库的智能体实体（含生成的主键ID）
     */
    public AgentEntity createAgent(AgentEntity agent);

    /**
     * 根据ID查询智能体
     * @param id 智能体ID
     * @return 智能体实体，不存在返回null
     */
    public AgentEntity getAgent(String id);
}
