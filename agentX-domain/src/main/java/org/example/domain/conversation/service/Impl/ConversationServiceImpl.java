package org.example.domain.conversation.service.Impl;

import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.IAgentDomainService;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.domain.conversation.adapter.port.LLMPort;
import org.example.domain.conversation.adapter.port.TokenEstimatorPort;
import org.example.domain.conversation.model.entity.ConversationStreamEvent;
import org.example.domain.conversation.model.entity.LLMEntity;
import org.example.domain.conversation.model.entity.LLMResult;
import org.example.domain.conversation.model.entity.LLMStreamChunk;
import org.example.domain.conversation.model.valobj.TokenUsage;
import org.example.domain.conversation.service.IConversationService;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.domain.message.service.IMessageDomainService;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.service.ISessionDomainService;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

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

    @Autowired
    private TokenEstimatorPort tokenEstimatorPort;

    @Override
    public String doConversation(String sessionId, String userId, String content) {
        LLMEntity llmEntity = prepareConversation(sessionId, userId, content);

        long startedAt = System.nanoTime();
        LLMResult result = llmPort.call(llmEntity);
        if (result == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "服务商[" + llmEntity.getProvider() + "]暂不支持对话");
        }

        String reply = result.content() == null ? "" : result.content();
        TokenUsage usage = resolveUsage(result.usage(), llmEntity, reply);
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;

        MessageEntity message = MessageEntity.createAssistantMessage(
                UUID.randomUUID().toString().replace("-", ""),
                sessionId,
                reply,
                usage,
                llmEntity.getModel(),
                llmEntity.getProvider(),
                normalizeFinishReason(result.finishReason()),
                latencyMs);
        messageDomainService.saveAssistantMessage(message);
        return reply;
    }

    @Override
    public Flux<ConversationStreamEvent> streamConversation(String sessionId, String userId, String content) {
        LLMEntity llmEntity = prepareConversation(sessionId, userId, content);
        String messageId = UUID.randomUUID().toString().replace("-", "");
        StringBuilder reply = new StringBuilder();
        AtomicBoolean persisted = new AtomicBoolean(false);
        AtomicBoolean usageEventSent = new AtomicBoolean(false);
        AtomicReference<TokenUsage> usageRef = new AtomicReference<>();
        AtomicReference<String> finishReasonRef = new AtomicReference<>("stop");
        AtomicLong finishedAt = new AtomicLong(0L);
        long startedAt = System.nanoTime();
        int estimatedPromptTokens = estimatePromptTokens(llmEntity);

        Flux<LLMStreamChunk> stream = llmPort.stream(llmEntity);
        if (stream == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "服务商[" + llmEntity.getProvider() + "]暂不支持流式对话");
        }

        Supplier<TokenUsage> resolveStreamUsage = () -> {
            TokenUsage usage = usageRef.get();
            if (usage != null && usage.hasUsage()) {
                return usage;
            }
            return TokenUsage.estimated(
                    estimatedPromptTokens,
                    tokenEstimatorPort.estimate(reply.toString()));
        };

        Supplier<Long> resolveLatency = () -> {
            long finished = finishedAt.get();
            if (finished == 0L) {
                long now = System.nanoTime();
                if (finishedAt.compareAndSet(0L, now)) {
                    finished = now;
                } else {
                    finished = finishedAt.get();
                }
            }
            return (finished - startedAt) / 1_000_000;
        };

        Runnable persistReply = () -> {
            if (!reply.isEmpty() && persisted.compareAndSet(false, true)) {
                messageDomainService.saveAssistantMessage(MessageEntity.createAssistantMessage(
                        messageId,
                        sessionId,
                        reply.toString(),
                        resolveStreamUsage.get(),
                        llmEntity.getModel(),
                        llmEntity.getProvider(),
                        normalizeFinishReason(finishReasonRef.get()),
                        resolveLatency.get()));
            }
        };

        Flux<ConversationStreamEvent> chunkEvents = stream
                .doOnNext(chunk -> {
                    if (chunk.hasContent()) {
                        reply.append(chunk.content());
                    }
                    if (chunk.usage() != null && chunk.usage().hasUsage()) {
                        usageRef.set(chunk.usage());
                    }
                    if (chunk.finishReason() != null && !chunk.finishReason().isBlank()) {
                        finishReasonRef.set(normalizeFinishReason(chunk.finishReason()));
                    }
                })
                .flatMapIterable(chunk -> toEvents(chunk, usageEventSent));

        Flux<ConversationStreamEvent> finalEvents = Mono.fromCallable(() -> {
                    persistReply.run();
                    if (usageEventSent.compareAndSet(false, true)) {
                        return ConversationStreamEvent.usage(resolveStreamUsage.get());
                    }
                    return null;
                })
                .concatWith(Mono.fromCallable(() -> ConversationStreamEvent.done(
                        messageId,
                        normalizeFinishReason(finishReasonRef.get()),
                        resolveLatency.get(),
                        resolveStreamUsage.get())))
                .subscribeOn(Schedulers.boundedElastic());

        return Flux.concat(
                        Flux.just(ConversationStreamEvent.start(
                                messageId,
                                llmEntity.getModel(),
                                llmEntity.getProvider())),
                        chunkEvents,
                        finalEvents)
                .doFinally(signalType -> {
                    if (signalType == SignalType.CANCEL) {
                        finishReasonRef.set("cancelled");
                        Schedulers.boundedElastic().schedule(persistReply);
                    } else if (signalType == SignalType.ON_ERROR) {
                        finishReasonRef.set("error");
                        Schedulers.boundedElastic().schedule(persistReply);
                    }
                });
    }

    private List<ConversationStreamEvent> toEvents(LLMStreamChunk chunk, AtomicBoolean usageEventSent) {
        List<ConversationStreamEvent> events = new ArrayList<>(2);
        if (chunk.hasContent()) {
            events.add(ConversationStreamEvent.delta(chunk.content()));
        }
        if (chunk.usage() != null
                && chunk.usage().hasUsage()
                && usageEventSent.compareAndSet(false, true)) {
            events.add(ConversationStreamEvent.usage(chunk.usage()));
        }
        return events;
    }

    private TokenUsage resolveUsage(TokenUsage usage, LLMEntity llmEntity, String content) {
        if (usage != null && usage.hasUsage()) {
            return usage;
        }
        return TokenUsage.estimated(
                estimatePromptTokens(llmEntity),
                tokenEstimatorPort.estimate(content));
    }

    private int estimatePromptTokens(LLMEntity llmEntity) {
        StringBuilder promptText = new StringBuilder();
        if (llmEntity.getSystemPrompt() != null) {
            promptText.append(llmEntity.getSystemPrompt());
        }
        if (llmEntity.getMessages() != null) {
            for (MessageEntity message : llmEntity.getMessages()) {
                if (message.getContent() != null) {
                    promptText.append('\n').append(message.getContent());
                }
            }
        }
        return tokenEstimatorPort.estimate(promptText.toString());
    }

    private String normalizeFinishReason(String finishReason) {
        return finishReason == null || finishReason.isBlank()
                ? "stop"
                : finishReason.toLowerCase(Locale.ROOT);
    }

    /**
     * 装载一次对话所需的会话、Agent、API Key 和历史消息。
     */
    private LLMEntity prepareConversation(String sessionId, String userId, String content) {
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

        return llmEntity;
    }
}
