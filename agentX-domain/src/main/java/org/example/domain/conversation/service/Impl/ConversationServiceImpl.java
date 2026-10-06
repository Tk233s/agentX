package org.example.domain.conversation.service.Impl;

import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.IAgentDomainService;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.domain.conversation.adapter.port.ContextWindowPort;
import org.example.domain.conversation.adapter.port.LLMPort;
import org.example.domain.conversation.adapter.port.TokenEstimatorPort;
import org.example.domain.conversation.model.entity.ConversationStreamEvent;
import org.example.domain.conversation.model.entity.LLMEntity;
import org.example.domain.conversation.model.entity.LLMResult;
import org.example.domain.conversation.model.entity.LLMStreamChunk;
import org.example.domain.conversation.model.valobj.ContextWindow;
import org.example.domain.conversation.model.valobj.TokenUsage;
import org.example.domain.conversation.service.IConversationService;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.domain.message.service.IMessageDomainService;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.model.valobj.SessionTokenBudget;
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
import java.util.LinkedList;
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

    private static final int MESSAGE_OVERHEAD_TOKENS = 4;

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

    @Autowired
    private ContextWindowPort contextWindowPort;

    @Override
    public String doConversation(String sessionId, String userId, String content) {
        PreparedConversation prepared = prepareConversation(sessionId, userId, content);
        LLMEntity llmEntity = prepared.llmEntity();

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
        sessionDomainService.addUsedTokens(sessionId, userId, tokenDelta(usage));
        return reply;
    }

    @Override
    public Flux<ConversationStreamEvent> streamConversation(String sessionId, String userId, String content) {
        PreparedConversation prepared = prepareConversation(sessionId, userId, content);
        LLMEntity llmEntity = prepared.llmEntity();
        String messageId = UUID.randomUUID().toString().replace("-", "");
        StringBuilder reply = new StringBuilder();
        AtomicBoolean persisted = new AtomicBoolean(false);
        AtomicBoolean usageEventSent = new AtomicBoolean(false);
        AtomicReference<TokenUsage> usageRef = new AtomicReference<>();
        AtomicReference<SessionTokenBudget> tokenBudgetRef = new AtomicReference<>(prepared.tokenBudget());
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
                TokenUsage finalUsage = resolveStreamUsage.get();
                messageDomainService.saveAssistantMessage(MessageEntity.createAssistantMessage(
                        messageId,
                        sessionId,
                        reply.toString(),
                        finalUsage,
                        llmEntity.getModel(),
                        llmEntity.getProvider(),
                        normalizeFinishReason(finishReasonRef.get()),
                        resolveLatency.get()));
                tokenBudgetRef.set(sessionDomainService.addUsedTokens(
                        sessionId,
                        userId,
                        tokenDelta(finalUsage)));
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
                        resolveStreamUsage.get(),
                        tokenBudgetRef.get())))
                .subscribeOn(Schedulers.boundedElastic());

        return Flux.concat(
                        Flux.just(ConversationStreamEvent.start(
                                messageId,
                                llmEntity.getModel(),
                                llmEntity.getProvider(),
                                prepared.tokenBudget())),
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

    private long tokenDelta(TokenUsage usage) {
        if (usage == null || usage.totalTokens() == null) {
            return 0L;
        }
        return Math.max(usage.totalTokens(), 0);
    }

    private String normalizeFinishReason(String finishReason) {
        return finishReason == null || finishReason.isBlank()
                ? "stop"
                : finishReason.toLowerCase(Locale.ROOT);
    }

    /**
     * 装载一次对话所需的会话、Agent、API Key 和历史消息。
     */
    private PreparedConversation prepareConversation(String sessionId, String userId, String content) {
        // 1. 查会话 → 拿到 agentId
        SessionEntity session = sessionDomainService.getSession(sessionId, userId);
        if (session == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话不存在");
        }
        SessionTokenBudget tokenBudget = session.tokenBudget();
        if (tokenBudget.isExhausted()) {
            throw new AppException(
                    ResponseCode.TOKEN_LIMIT_EXCEEDED.getCode(),
                    tokenLimitExceededMessage(tokenBudget));
        }

        // 2. 查 Agent → 拿 systemPrompt、modelId、apiKeyId 等
        AgentEntity agent = agentDomainService.getAgent(session.getAgentId(), userId);
        if (agent == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "智能体不存在");
        }

        // 3. 查 Agent 绑定的 ApiKey → 拿到 apiKey、baseUrl、provider
        ApiKeyEntity apiKey = apiKeyDomainService.getApiKey(agent.getApiKeyId(), userId);
        if (apiKey == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "Agent绑定的API密钥不存在");
        }
        if (Boolean.FALSE.equals(apiKey.getEnabled())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "Agent绑定的API密钥已停用");
        }

        // 4. 先保存当前用户消息。即使后续模型调用失败，用户也能在历史中看到自己发送的内容。
        messageDomainService.saveUserMessage(sessionId, content);

        // 5. 查历史消息（包含刚保存的当前用户消息），按上下文窗口裁剪旧消息
        ContextWindow contextWindow = contextWindowPort.getContextWindow(agent.getModelId());
        List<MessageEntity> messages = selectContextMessages(
                agent.getSystemPrompt(),
                messageDomainService.listMessages(sessionId, userId),
                contextWindow);

        // 6. 组装 LLMEntity（含工具装填：从 Agent 配置取工具名列表传给基础设施层）
        LLMEntity llmEntity = LLMEntity.builder()
                .model(agent.getModelId())
                .apiKey(apiKey.getApiKey())
                .baseUrl(apiKey.getBaseUrl())
                .provider(apiKey.getProvider())
                .systemPrompt(agent.getSystemPrompt())
                .messages(messages)
                .tools(agent.getTools())  // ← 装填工具：Agent 配置的工具名列表
                .build();

        return new PreparedConversation(llmEntity, tokenBudget);
    }

    /**
     * 从最新消息向前保留完整对话组，必要时省略较早的 user/assistant 组。
     */
    private List<MessageEntity> selectContextMessages(
            String systemPrompt,
            List<MessageEntity> messages,
            ContextWindow contextWindow) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        int maxInputTokens = contextWindow.maxInputTokens();
        int usedTokens = tokenEstimatorPort.estimate(systemPrompt);
        if (usedTokens >= maxInputTokens) {
            throw contextWindowExceeded(usedTokens, maxInputTokens);
        }

        List<List<MessageEntity>> groups = groupMessages(messages);
        LinkedList<MessageEntity> selected = new LinkedList<>();

        for (int i = groups.size() - 1; i >= 0; i--) {
            List<MessageEntity> group = groups.get(i);
            int groupTokens = estimateMessages(group);
            if (!selected.isEmpty() && usedTokens + groupTokens > maxInputTokens) {
                break;
            }

            selected.addAll(0, group);
            usedTokens += groupTokens;
        }

        if (usedTokens > maxInputTokens) {
            throw contextWindowExceeded(usedTokens, maxInputTokens);
        }
        return new ArrayList<>(selected);
    }

    private List<List<MessageEntity>> groupMessages(List<MessageEntity> messages) {
        List<List<MessageEntity>> groups = new ArrayList<>();
        List<MessageEntity> currentGroup = null;

        for (MessageEntity message : messages) {
            if (currentGroup == null || "user".equals(message.getRole())) {
                currentGroup = new ArrayList<>();
                groups.add(currentGroup);
            }
            currentGroup.add(message);
        }
        return groups;
    }

    private int estimateMessages(List<MessageEntity> messages) {
        int tokens = 0;
        for (MessageEntity message : messages) {
            tokens += tokenEstimatorPort.estimate(message.getContent()) + MESSAGE_OVERHEAD_TOKENS;
        }
        return tokens;
    }

    private AppException contextWindowExceeded(int usedTokens, int maxInputTokens) {
        return new AppException(
                ResponseCode.ILLEGAL_PARAMETER.getCode(),
                "当前消息超出模型可用上下文（需要 "
                        + usedTokens
                        + " Token，可用 "
                        + maxInputTokens
                        + " Token），请缩短消息或新建会话");
    }

    private String tokenLimitExceededMessage(SessionTokenBudget tokenBudget) {
        return "本会话Token额度已用完（已用 "
                + tokenBudget.usedTokens()
                + " / "
                + tokenBudget.limit()
                + "）";
    }

    private record PreparedConversation(LLMEntity llmEntity, SessionTokenBudget tokenBudget) {
    }
}
