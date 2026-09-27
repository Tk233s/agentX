package org.example.domain.conversation.service.Impl;

import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.IAgentDomainService;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.domain.conversation.adapter.port.LLMPort;
import org.example.domain.conversation.model.entity.LLMEntity;
import org.example.domain.conversation.service.IConversationService;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.domain.message.service.IMessageDomainService;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.service.ISessionDomainService;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 对话领域服务实现
 * 编排 Session / Agent / ApiKey / Message / LLM 五个模块，完成一次完整对话。
 */
@Service
public class ConversationServiceImpl implements IConversationService {

    @Autowired
    private ISessionDomainService sessionDomainService;

    @Autowired
    private IAgentDomainService agentDomainService;

    @Autowired
    private IApiKeyDomainService apiKeyDomainService;

    @Autowired
    private IMessageDomainService messageDomainService;

    @Resource(name = "LLMClientFactory")
    private LLMPort llmPort;

    @Override
    public String doConversation(String sessionId, String userId, String content) {

        // 1. 查会话 → 拿到 agentId
        SessionEntity session = sessionDomainService.getSession(sessionId, userId);
        if (session == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话不存在");
        }

        // 2. 查 Agent → 拿 systemPrompt、modelId、provider、temperature 等
        AgentEntity agent = agentDomainService.getAgent(session.getAgentId(), userId);
        if (agent == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "智能体不存在");
        }

        // 3. 查 ApiKey → 用 userId + provider 拿到 apiKey、baseUrl
        ApiKeyEntity apiKey = apiKeyDomainService.getApiKeyByProvider(userId, agent.getProvider());
        if (apiKey == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "未配置服务商[" + agent.getProvider() + "]的API密钥");
        }

        // 4. 存用户消息到 message 表
        messageDomainService.saveUserMessage(sessionId, content);

        // 5. 查历史消息（包含刚存的用户消息）
        List<MessageEntity> messages = messageDomainService.listMessages(sessionId, userId);

        // 6. 组装 LLMEntity（含工具装填：从 Agent 配置取工具名列表传给基础设施层）
        LLMEntity llmEntity = LLMEntity.builder()
                .model(agent.getModelId())
                .apiKey(apiKey.getApiKey())
                .baseUrl(apiKey.getBaseUrl())
                .temperature(agent.getTemperature())
                .maxTokens(agent.getMaxTokens())
                .provider(agent.getProvider())
                .systemPrompt(agent.getSystemPrompt())
                .messages(messages)
                .tools(agent.getTools())  // ← 装填工具：Agent 配置的工具名列表
                .build();

        // 7. 调用 LLM，拿到回复
        String reply = llmPort.call(llmEntity);

        // 8. 存 AI 回复到 message 表（tokens 暂时存 0，后续再算）
        messageDomainService.saveAssistantMessage(sessionId, reply, 0);

        // 9. 返回回复
        return reply;
    }
}
