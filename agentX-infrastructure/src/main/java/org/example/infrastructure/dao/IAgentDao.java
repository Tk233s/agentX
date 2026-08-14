package org.example.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.dao.po.AgentPO;

import java.util.List;

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
     * 更新智能体
     * @param agentPO 智能体持久化对象（需携带ID）
     * @return 受影响行数
     */
    int update(AgentPO agentPO);

    /**
     * 根据ID删除智能体
     * @param id 智能体ID
     * @return 受影响行数，0表示不存在
     */
    int deleteById(String id);

    /**
     * 根据ID查询智能体
     * @param id 智能体ID
     * @return 智能体持久化对象，不存在返回null
     */
    AgentPO queryById(String id);

    /**
     * 根据创建者用户ID查询智能体列表
     * @param userId 创建者用户ID
     * @return 智能体持久化对象列表
     */
    List<AgentPO> queryByUserId(String userId);
}
