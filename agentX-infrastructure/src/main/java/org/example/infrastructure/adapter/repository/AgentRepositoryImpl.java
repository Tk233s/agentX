package org.example.infrastructure.adapter.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.domain.agent.adapter.repository.AgentRepository;
import org.example.domain.agent.model.entity.AgentEntity;
import org.example.infrastructure.dao.IAgentDao;
import org.example.infrastructure.dao.po.AgentPO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 智能体仓储实现
 * 实现领域层定义的 AgentRepository 端口
 */
@Repository
public class AgentRepositoryImpl implements AgentRepository {

    private static final Gson GSON = new Gson();

    @Resource
    private IAgentDao agentDao;

    @Override
    public void save(AgentEntity agent) {
        agentDao.insert(toPO(agent));
    }

    @Override
    public void update(AgentEntity agent) {
        agentDao.update(toPO(agent));
    }

    @Override
    public int deleteById(String id) {
        return agentDao.deleteById(id);
    }

    @Override
    public AgentEntity findById(String id) {
        return toEntity(agentDao.queryById(id));
    }

    @Override
    public List<AgentEntity> queryByUserId(String userId) {
        List<AgentPO> pos = agentDao.queryByUserId(userId);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }
        return pos.stream().map(this::toEntity).collect(Collectors.toList());
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
        po.setProvider(agent.getProvider());
        po.setModelId(agent.getModelId());
        po.setMaxTokens(agent.getMaxTokens());
        // List<String> → JSON 字符串存入数据库
        po.setToolsJson(agent.getTools() != null ? GSON.toJson(agent.getTools()) : null);
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
        agent.setProvider(po.getProvider());
        agent.setModelId(po.getModelId());
        agent.setMaxTokens(po.getMaxTokens());
        // JSON 字符串 → List<String>
        if (po.getToolsJson() != null && !po.getToolsJson().isEmpty()) {
            agent.setTools(GSON.fromJson(po.getToolsJson(), new TypeToken<List<String>>() {}.getType()));
        }
        agent.setEnabled(po.getEnabled());
        agent.setUserId(po.getUserId());
        agent.setCreatedAt(po.getCreatedAt());
        agent.setUpdatedAt(po.getUpdatedAt());
        return agent;
    }
}
