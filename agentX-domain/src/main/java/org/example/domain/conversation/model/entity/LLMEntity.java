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

    private String systemPrompt;
    private List<MessageEntity> messages;

    /**
     * 工具名称列表。Agent 配置了哪些工具，这里就传哪些名称，
     * 基础设施层根据名称从 ToolRegistry 取出真正的工具实例注册给 ChatClient。
     * 传 null 或空列表表示不使用工具。
     */
    private List<String> tools;
}
