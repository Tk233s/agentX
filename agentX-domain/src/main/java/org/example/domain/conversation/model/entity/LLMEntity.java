package org.example.domain.conversation.model.entity;

import lombok.Builder;
import lombok.Data;
import org.example.domain.message.model.entity.MessageEntity;

import java.util.List;

@Data
@Builder
public class LLMEntity {

    private String model;
    private String apiKey;
    private String baseUrl;

    /**
     * 单次模型生成的安全上限；由基础设施层在为空时使用默认值，不属于用户配置。
     */
    private Integer maxTokens;

    private String provider;

    /**
     * 当前会话与用户上下文，用于工具调用审计。
     */
    private String sessionId;
    private String userId;

    /**
     * 当前会话的权限档（如 READ_ONLY / FULL）。
     *
     * 工具清单已按该权限收窄过一遍，这里再传给工具运行时，
     * 让执行点能独立复核一次：清单层是"模型看不到"，这里是"就算被调用也执行不了"。
     */
    private String permissionMode;

    private String systemPrompt;
    private List<MessageEntity> messages;

    /**
     * 工具名称列表。Agent 配置了哪些工具，这里就传哪些名称，
     * 基础设施层根据名称从 ToolRegistry 取出真正的工具实例注册给 ChatClient。
     * 传 null 或空列表表示不使用工具。
     */
    private List<String> tools;
}
