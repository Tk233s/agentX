package org.example.domain.agent.adapter.repository;

import org.example.domain.agent.model.entity.AgentEntity;

/**
 * 智能体仓储接口
 */
public interface AgentRepository {

    /**
     * 保存智能体（新增）
     * @param agent 智能体实体
     */
    void save(AgentEntity agent);

    /**
     * 根据ID查询智能体
     * @param id 智能体ID
     * @return 智能体实体，不存在返回null
     */
    AgentEntity findById(String id);
}
