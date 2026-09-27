package org.example.domain.agent.service.Impl;

import org.example.domain.agent.adapter.repository.AgentRepository;
import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.IAgentDomainService;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

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
     * 更新智能体
     * @param agent 待更新的智能体实体（需携带ID）
     * @return 已落库的智能体实体
     */
    @Transactional
    public AgentEntity updateAgent(AgentEntity agent, String userId) {
        // 1. 业务字段校验（id 必填由 trigger 层分组校验保证）
        agent.validate();
        // 2. 存在性与归属校验
        AgentEntity existing = requireOwnedAgent(agent.getId(), userId);
        // 3. 保留入参不可覆盖的字段，并刷新更新时间
        agent.setUserId(existing.getUserId());
        agent.setEnabled(existing.getEnabled());
        agent.setCreatedAt(existing.getCreatedAt());
        agent.setUpdatedAt(LocalDateTime.now());
        // 4. 落库更新
        agentRepository.update(agent);
        return agent;
    }

    /**
     * 根据ID删除智能体
     * @param id 智能体ID
     * @param userId 当前登录用户ID
     */
    @Transactional
    public void deleteAgent(String id, String userId) {
        requireOwnedAgent(id, userId);
        int rows = agentRepository.deleteById(id);
        if (rows == 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "智能体不存在");
        }
    }

    /**
     * 根据ID查询智能体
     * @param id 智能体ID
     * @param userId 当前登录用户ID
     * @return 智能体实体，不存在返回null
     */
    public AgentEntity getAgent(String id, String userId) {
        AgentEntity agent = agentRepository.findById(id);
        if (agent == null) {
            return null;
        }
        checkOwner(agent, userId);
        return agent;
    }

    /**
     * 根据创建者用户ID查询智能体列表
     * @param userId 创建者用户ID
     * @return 智能体实体列表
     */
    public List<AgentEntity> listAgents(String userId) {
        return agentRepository.queryByUserId(userId);
    }

    private AgentEntity requireOwnedAgent(String id, String userId) {
        AgentEntity agent = agentRepository.findById(id);
        if (agent == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "智能体不存在");
        }
        checkOwner(agent, userId);
        return agent;
    }

    private void checkOwner(AgentEntity agent, String userId) {
        if (userId == null || !userId.equals(agent.getUserId())) {
            throw new AppException(ResponseCode.FORBIDDEN.getCode(), "无权访问该智能体");
        }
    }
}
