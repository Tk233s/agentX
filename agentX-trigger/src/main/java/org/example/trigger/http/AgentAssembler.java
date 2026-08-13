package org.example.trigger.http;

import org.example.domain.agent.model.entity.AgentEntity;
import org.example.trigger.dto.agent.AgentReq;
import org.example.trigger.dto.agent.AgentRes;

/**
 * HTTP 层 DTO 与领域实体之间的转换器
 * 属于入站适配器（trigger）的内部细节，领域层对此无感知。
 * 入参通过领域工厂方法 AgentEntity.createNew 构建，保证初始状态（id/时间戳/enabled）由领域统一。
 */
public class AgentAssembler {

    /** 入参 DTO -> 领域实体 */
    public static AgentEntity toEntity(AgentReq req) {
        return AgentEntity.createNew(
                req.getName(),
                req.getAvatar(),
                req.getDescription(),
                req.getSystemPrompt(),
                req.getWelcomeMessage(),
                req.getUserId());
    }

    /** 领域实体 -> 出参 DTO */
    public static AgentRes toRes(AgentEntity entity) {
        AgentRes res = new AgentRes();
        res.setId(entity.getId());
        res.setName(entity.getName());
        res.setAvatar(entity.getAvatar());
        res.setDescription(entity.getDescription());
        res.setSystemPrompt(entity.getSystemPrompt());
        res.setWelcomeMessage(entity.getWelcomeMessage());
        res.setEnabled(entity.getEnabled());
        res.setCreateTime(entity.getCreatedAt());
        return res;
    }
}
