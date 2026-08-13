package org.example.domain.agent.service.Impl;

import org.example.domain.agent.adapter.repository.AgentRepository;
import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.IAgentDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 智能体领域服务实现
 */
@Service
public class AgentDomainServiceImpl implements IAgentDomainService {

    @Resource
    private AgentRepository agentRepository;

    /**
     * 创建智能体
     * @param agent 待创建的智能体实体
     * @return 已落库的智能体实体（含生成的主键ID）
     */
    @Transactional
    public AgentEntity createAgent(AgentEntity agent) {
        // 1. 业务规则校验
        agent.validate();
        // 2. 落库
        agentRepository.save(agent);
        return agent;
    }

    /**
     * 根据ID查询智能体
     * @param id 智能体ID
     * @return 智能体实体，不存在返回null
     */
    public AgentEntity getAgent(String id) {
        return agentRepository.findById(id);
    }
}
