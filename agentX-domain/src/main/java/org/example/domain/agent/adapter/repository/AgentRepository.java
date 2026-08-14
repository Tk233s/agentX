package org.example.domain.agent.adapter.repository;

import org.example.domain.agent.model.entity.AgentEntity;

import java.util.List;

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
     * 更新智能体
     * @param agent 智能体实体（需携带ID）
     */
    void update(AgentEntity agent);

    /**
     * 根据ID删除智能体
     * @param id 智能体ID
     * @return 受影响行数，0表示不存在
     */
    int deleteById(String id);

    /**
     * 根据ID查询智能体
     * @param id 智能体ID
     * @return 智能体实体，不存在返回null
     */
    AgentEntity findById(String id);

    /**
     * 根据创建者用户ID查询智能体列表
     * @param userId 创建者用户ID
     * @return 智能体实体列表，可能为空
     */
    List<AgentEntity> queryByUserId(String userId);
}
