package org.example.domain.agent.model.entity;

import lombok.Data;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 智能体领域实体
 */
@Data
public class AgentEntity {

    /** 智能体唯一ID */
    private String id;

    /** 智能体名称 */
    private String name;

    /** 智能体头像URL */
    private String avatar;

    /** 智能体描述 */
    private String description;

    /** 智能体系统提示词 */
    private String systemPrompt;

    /** 欢迎消息 */
    private String welcomeMessage;

    /** 服务商 openai/anthropic */
    private String provider;

    /** 模型ID */
    private String modelId;

    /** 最大Token数 */
    private Integer maxTokens;

    /**
     * 工具名称列表。该 Agent 配置启用的工具，如 ["weather", "file"]。
     * 对话时会传给基础设施层，基础设施根据名称从 ToolRegistry 取出工具实例注册给 LLM。
     * 数据库存储为 JSON 数组字符串。
     */
    private java.util.List<String> tools;

    /** 智能体状态：true-启用，false-禁用 */
    private Boolean enabled;

    /** 创建者用户ID */
    private String userId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 工厂方法：创建新的智能体
     * 由领域层统一负责初始状态（id、enabled、时间戳），外部只提供业务字段。
     * maxTokens 为空时由基础设施层使用默认值。
     */
    public static AgentEntity createNew(String name, String avatar, String description,
                                        String systemPrompt, String welcomeMessage, String userId,
                                        String provider, String modelId,
                                        Integer maxTokens,
                                        java.util.List<String> tools) {
        AgentEntity agent = new AgentEntity();
        agent.setId(UUID.randomUUID().toString().replace("-", ""));
        agent.setName(name);
        agent.setAvatar(avatar);
        agent.setDescription(description);
        agent.setSystemPrompt(systemPrompt);
        agent.setWelcomeMessage(welcomeMessage);
        agent.setProvider(provider);
        agent.setModelId(modelId);
        agent.setMaxTokens(maxTokens);
        agent.setTools(tools);
        agent.setEnabled(true);
        agent.setUserId(userId);
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        return agent;
    }

    /**
     * 业务规则校验：创建前校验核心字段的合法性
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "智能体名称不能为空");
        }
    }
}
