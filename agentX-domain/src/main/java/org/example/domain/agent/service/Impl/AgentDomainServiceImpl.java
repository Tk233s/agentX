package org.example.domain.agent.service.Impl;

import org.example.domain.agent.adapter.repository.AgentRepository;
import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.IAgentDomainService;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.domain.tool.adapter.port.ToolRuntimePort;
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

    @Resource
    private IApiKeyDomainService apiKeyDomainService;

    @Resource
    private ToolRuntimePort toolRuntimePort;

    /**
     * 创建智能体
     * @param agent 待创建的智能体实体
     * @return 已落库的智能体实体（含生成的主键ID）
     */
    @Transactional
    public AgentEntity createAgent(AgentEntity agent) {
        // 1. 业务规则校验
        agent.validate();
        // 2. 校验工具绑定并统一工具标识
        validateTools(agent);
        // 3. 校验密钥存在、归属当前用户且已启用
        validateApiKey(agent.getApiKeyId(), agent.getUserId());
        // 4. 落库
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
        // 1. 存在性与归属校验
        AgentEntity existing = requireOwnedAgent(agent.getId(), userId);
        // 2. 请求未显式传能力模式时沿用原配置，避免编辑其他字段时模式被重置
        if (agent.getCapabilityProfile() == null || agent.getCapabilityProfile().isBlank()) {
            agent.setCapabilityProfile(existing.getCapabilityProfile());
        }
        // 3. 业务字段校验（id 必填由 trigger 层分组校验保证）
        agent.validate();
        // 4. 校验能力模式与工具绑定，并统一工具标识
        validateTools(agent);
        // 5. 校验密钥存在、归属当前用户且已启用
        validateApiKey(agent.getApiKeyId(), userId);
        // 6. 保留入参不可覆盖的字段，并刷新更新时间
        agent.setUserId(existing.getUserId());
        agent.setEnabled(existing.getEnabled());
        agent.setCreatedAt(existing.getCreatedAt());
        agent.setUpdatedAt(LocalDateTime.now());
        // 7. 落库更新
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

    private void validateApiKey(String apiKeyId, String userId) {
        ApiKeyEntity apiKey = apiKeyDomainService.getApiKey(apiKeyId, userId);
        if (apiKey == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "API密钥不存在");
        }
        if (Boolean.FALSE.equals(apiKey.getEnabled())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "API密钥已停用");
        }
    }

    private void validateTools(AgentEntity agent) {
        agent.normalizeTools();
        agent.normalizeCapabilityProfile();
        // 生效工具 = 能力模式默认工具 + 显式高级覆盖项；这里校验的是 Agent 自身配置，
        // 不传会话权限，避免把某个会话的只读限制误判成 Agent 配置错误
        List<String> effectiveToolIds = toolRuntimePort.resolveToolIds(
                agent.getCapabilityProfile(),
                agent.getTools(),
                null);
        toolRuntimePort.validateBindings(effectiveToolIds);
    }
}
