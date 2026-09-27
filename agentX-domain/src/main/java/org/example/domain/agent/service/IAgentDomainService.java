package org.example.domain.agent.service;

import org.example.domain.agent.model.entity.AgentEntity;

import java.util.List;

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
     * 更新智能体
     * @param agent 待更新的智能体实体（需携带ID）
     * @param userId 当前登录用户ID
     * @return 已落库的智能体实体
     */
    public AgentEntity updateAgent(AgentEntity agent, String userId);

    /**
     * 根据ID删除智能体
     * @param id 智能体ID
     * @param userId 当前登录用户ID
     */
    public void deleteAgent(String id, String userId);

    /**
     * 根据ID查询智能体
     * @param id 智能体ID
     * @param userId 当前登录用户ID
     * @return 智能体实体，不存在返回null
     */
    public AgentEntity getAgent(String id, String userId);

    /**
     * 根据创建者用户ID查询智能体列表
     * @param userId 创建者用户ID
     * @return 智能体实体列表
     */
    public List<AgentEntity> listAgents(String userId);
}
