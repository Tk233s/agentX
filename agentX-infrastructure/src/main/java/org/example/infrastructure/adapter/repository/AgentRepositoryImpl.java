package org.example.infrastructure.adapter.repository;

import org.example.domain.agent.adapter.repository.AgentRepository;
import org.example.domain.agent.model.entity.AgentEntity;
import org.example.infrastructure.dao.IAgentDao;
import org.example.infrastructure.dao.po.AgentPO;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

/**
 * 智能体仓储实现
 * 实现领域层定义的 AgentRepository 端口
 */
@Repository
public class AgentRepositoryImpl implements AgentRepository {

    @Resource
    private IAgentDao agentDao;

    @Override
    public void save(AgentEntity agent) {
        agentDao.insert(toPO(agent));
    }

    @Override
    public AgentEntity findById(String id) {
        return toEntity(agentDao.queryById(id));
    }

    /** Entity -> PO */
    private AgentPO toPO(AgentEntity agent) {
        AgentPO po = new AgentPO();
        po.setId(agent.getId());
        po.setName(agent.getName());
        po.setAvatar(agent.getAvatar());
        po.setDescription(agent.getDescription());
        po.setSystemPrompt(agent.getSystemPrompt());
        po.setWelcomeMessage(agent.getWelcomeMessage());
        po.setEnabled(agent.getEnabled());
        po.setUserId(agent.getUserId());
        po.setCreatedAt(agent.getCreatedAt());
        po.setUpdatedAt(agent.getUpdatedAt());
        return po;
    }

    /** PO -> Entity */
    private AgentEntity toEntity(AgentPO po) {
        if (po == null) {
            return null;
        }
        AgentEntity agent = new AgentEntity();
        agent.setId(po.getId());
        agent.setName(po.getName());
        agent.setAvatar(po.getAvatar());
        agent.setDescription(po.getDescription());
        agent.setSystemPrompt(po.getSystemPrompt());
        agent.setWelcomeMessage(po.getWelcomeMessage());
        agent.setEnabled(po.getEnabled());
        agent.setUserId(po.getUserId());
        agent.setCreatedAt(po.getCreatedAt());
        agent.setUpdatedAt(po.getUpdatedAt());
        return agent;
    }
}
