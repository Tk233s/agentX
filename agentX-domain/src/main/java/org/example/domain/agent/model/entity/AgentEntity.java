package org.example.domain.agent.model.entity;

import lombok.Data;
import org.example.types.enums.AgentCapabilityProfile;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

    /** 绑定的API密钥ID */
    private String apiKeyId;

    /** 模型ID */
    private String modelId;

    /**
     * Agent 能力模式。
     * 工具由运行时根据该模式自动提供，tools 仅作为高级覆盖项。
     */
    private String capabilityProfile;

    /**
     * 高级工具覆盖列表。
     * 运行模式提供的工具会与该列表合并，普通 Agent 不需要手工配置。
     * 数据库存储为 JSON 数组字符串。
     */
    private List<String> tools;

    /**
     * 绑定的知识库 ID 列表。
     * 对话时按最后一条用户消息去这些库里检索，命中的片段会拼进 System Prompt。
     * 数据库存储为 JSON 数组字符串，与 tools 同一模式。
     */
    private List<String> knowledgeBaseIds;

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
     */
    public static AgentEntity createNew(String name, String avatar, String description,
                                        String systemPrompt, String welcomeMessage, String userId,
                                        String apiKeyId, String modelId,
                                        String capabilityProfile,
                                        List<String> tools,
                                        List<String> knowledgeBaseIds) {
        AgentEntity agent = new AgentEntity();
        agent.setId(UUID.randomUUID().toString().replace("-", ""));
        agent.setName(name);
        agent.setAvatar(avatar);
        agent.setDescription(description);
        agent.setSystemPrompt(systemPrompt);
        agent.setWelcomeMessage(welcomeMessage);
        agent.setApiKeyId(apiKeyId);
        agent.setModelId(modelId);
        agent.setCapabilityProfile(capabilityProfile);
        agent.setTools(tools);
        agent.setKnowledgeBaseIds(knowledgeBaseIds);
        agent.setEnabled(true);
        agent.setUserId(userId);
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        return agent;
    }

    /**
     * 兼容旧调用方：未指定能力模式时按自定义模式处理。
     */
    public static AgentEntity createNew(String name, String avatar, String description,
                                        String systemPrompt, String welcomeMessage, String userId,
                                        String apiKeyId, String modelId,
                                        List<String> tools) {
        return createNew(
                name,
                avatar,
                description,
                systemPrompt,
                welcomeMessage,
                userId,
                apiKeyId,
                modelId,
                AgentCapabilityProfile.CUSTOM.name(),
                tools,
                null);
    }

    /**
     * 业务规则校验：创建前校验核心字段的合法性
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "智能体名称不能为空");
        }
        if (apiKeyId == null || apiKeyId.trim().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "API密钥不能为空");
        }
    }

    /**
     * 统一清理工具绑定：去空、去重、转小写。
     */
    public void normalizeTools() {
        if (tools == null || tools.isEmpty()) {
            tools = new ArrayList<>();
            return;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tool : tools) {
            if (tool != null && !tool.isBlank()) {
                normalized.add(tool.trim().toLowerCase(Locale.ROOT));
            }
        }
        tools = new ArrayList<>(normalized);
    }

    /**
     * 统一清理知识库绑定：去空、去重。
     *
     * 与工具不同，知识库 ID 是大小写敏感的主键，所以这里不做小写转换。
     */
    public void normalizeKnowledgeBaseIds() {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            knowledgeBaseIds = new ArrayList<>();
            return;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String knowledgeBaseId : knowledgeBaseIds) {
            if (knowledgeBaseId != null && !knowledgeBaseId.isBlank()) {
                normalized.add(knowledgeBaseId.trim());
            }
        }
        knowledgeBaseIds = new ArrayList<>(normalized);
    }

    /**
     * 统一能力模式：空值默认普通对话，非法值由领域校验统一拦截。
     */
    public void normalizeCapabilityProfile() {
        if (capabilityProfile == null || capabilityProfile.isBlank()) {
            capabilityProfile = AgentCapabilityProfile.CHAT.name();
            return;
        }
        capabilityProfile = AgentCapabilityProfile.parse(capabilityProfile).name();
    }
}
