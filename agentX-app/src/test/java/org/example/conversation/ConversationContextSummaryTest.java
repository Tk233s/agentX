package org.example.conversation;

import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.IAgentDomainService;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.domain.conversation.adapter.port.ContextSummaryPolicyPort;
import org.example.domain.conversation.adapter.port.ContextWindowPort;
import org.example.domain.conversation.adapter.port.LLMPort;
import org.example.domain.conversation.adapter.port.TokenEstimatorPort;
import org.example.domain.conversation.model.entity.LLMEntity;
import org.example.domain.conversation.model.entity.LLMResult;
import org.example.domain.conversation.model.valobj.ContextSummaryPolicy;
import org.example.domain.conversation.model.valobj.ContextWindow;
import org.example.domain.conversation.model.valobj.TokenUsage;
import org.example.domain.conversation.service.Impl.ConversationServiceImpl;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.domain.message.service.IMessageDomainService;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.model.valobj.SessionMemory;
import org.example.domain.session.service.ISessionDomainService;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConversationContextSummaryTest {

    @Test
    public void summarizesOldGroupsAndInjectsSummaryIntoChatContext() {
        List<MessageEntity> messages = messages();
        Fixture fixture = fixture(messages, new ContextSummaryPolicy(true, 0.5, 0.25, 100, 2));
        when(fixture.llmPort.call(any(LLMEntity.class))).thenReturn(
                new LLMResult("压缩后的旧信息", TokenUsage.provider(100, 20, 120), "stop"),
                new LLMResult("ok", TokenUsage.provider(80, 2, 82), "stop"));
        when(fixture.sessionDomainService.saveMemory(
                eq(fixture.sessionId),
                eq(fixture.userId),
                isNull(),
                any(SessionMemory.class))).thenReturn(true);

        String reply = fixture.service.doConversation(fixture.sessionId, fixture.userId, "current");

        assertEquals("ok", reply);
        ArgumentCaptor<LLMEntity> captor = ArgumentCaptor.forClass(LLMEntity.class);
        verify(fixture.llmPort, times(2)).call(captor.capture());

        LLMEntity summaryCall = captor.getAllValues().get(0);
        assertTrue(summaryCall.getSystemPrompt().contains("会话记忆整理器"));
        assertTrue(summaryCall.getTools().isEmpty());

        LLMEntity chatCall = captor.getAllValues().get(1);
        assertTrue(chatCall.getSystemPrompt().contains("压缩后的旧信息"));
        assertEquals(
                List.of("u2", "a2", "u3"),
                chatCall.getMessages().stream().map(MessageEntity::getId).toList());

        ArgumentCaptor<SessionMemory> memoryCaptor = ArgumentCaptor.forClass(SessionMemory.class);
        verify(fixture.sessionDomainService).saveMemory(
                eq(fixture.sessionId),
                eq(fixture.userId),
                isNull(),
                memoryCaptor.capture());
        assertEquals("a1", memoryCaptor.getValue().summarizedThroughMessageId());
    }

    @Test
    public void reusesExistingSummaryAndOnlySendsMessagesAfterWatermark() {
        UserMessages messages = userMessages();
        SessionEntity session = session("已有记忆", "a1");
        Fixture fixture = fixture(messages.all(), session, new ContextSummaryPolicy(true, 0.5, 0.25, 100, 2));
        when(fixture.llmPort.call(any(LLMEntity.class)))
                .thenReturn(new LLMResult("ok", TokenUsage.provider(50, 2, 52), "stop"));

        fixture.service.doConversation(fixture.sessionId, fixture.userId, "current");

        ArgumentCaptor<LLMEntity> captor = ArgumentCaptor.forClass(LLMEntity.class);
        verify(fixture.llmPort).call(captor.capture());
        LLMEntity chatCall = captor.getValue();
        assertTrue(chatCall.getSystemPrompt().contains("已有记忆"));
        assertEquals(
                List.of("u2", "a2", "u3"),
                chatCall.getMessages().stream().map(MessageEntity::getId).toList());
        verify(fixture.sessionDomainService, never()).saveMemory(any(), any(), any(), any());
    }

    @Test
    public void summaryFailureFallsBackToSlidingWindow() {
        List<MessageEntity> messages = messages();
        Fixture fixture = fixture(messages, new ContextSummaryPolicy(true, 0.5, 0.25, 100, 2));
        when(fixture.llmPort.call(any(LLMEntity.class)))
                .thenThrow(new IllegalStateException("summary failed"))
                .thenReturn(new LLMResult("ok", TokenUsage.provider(80, 2, 82), "stop"));

        String reply = fixture.service.doConversation(fixture.sessionId, fixture.userId, "current");

        assertEquals("ok", reply);
        verify(fixture.llmPort, times(2)).call(any(LLMEntity.class));
        verify(fixture.sessionDomainService, never()).saveMemory(any(), any(), any(), any());
    }

    @Test
    public void summaryUsageIsAccumulatedBeforeChatUsage() {
        List<MessageEntity> messages = messages();
        Fixture fixture = fixture(messages, new ContextSummaryPolicy(true, 0.5, 0.25, 100, 2));
        when(fixture.llmPort.call(any(LLMEntity.class))).thenReturn(
                new LLMResult("压缩后的旧信息", TokenUsage.provider(100, 20, 120), "stop"),
                new LLMResult("ok", TokenUsage.provider(80, 2, 82), "stop"));
        when(fixture.sessionDomainService.saveMemory(
                eq(fixture.sessionId),
                eq(fixture.userId),
                isNull(),
                any(SessionMemory.class))).thenReturn(true);

        fixture.service.doConversation(fixture.sessionId, fixture.userId, "current");

        verify(fixture.sessionDomainService).addUsedTokens(fixture.sessionId, fixture.userId, 120L);
        verify(fixture.sessionDomainService).addUsedTokens(fixture.sessionId, fixture.userId, 82L);
    }

    @Test
    public void emptySummaryWithUsageFallsBackAndIsStillCharged() {
        List<MessageEntity> messages = messages();
        Fixture fixture = fixture(messages, new ContextSummaryPolicy(true, 0.5, 0.25, 100, 2));
        when(fixture.llmPort.call(any(LLMEntity.class))).thenReturn(
                new LLMResult("", TokenUsage.provider(100, 20, 120), "length"),
                new LLMResult("ok", TokenUsage.provider(80, 2, 82), "stop"));

        String reply = fixture.service.doConversation(fixture.sessionId, fixture.userId, "current");

        assertEquals("ok", reply);
        verify(fixture.sessionDomainService).addUsedTokens(fixture.sessionId, fixture.userId, 120L);
        verify(fixture.sessionDomainService, never()).saveMemory(any(), any(), any(), any());
    }

    @Test
    public void summaryUsageExhaustingBudgetStopsChatModelCall() {
        List<MessageEntity> messages = messages();
        Fixture fixture = fixture(messages, new ContextSummaryPolicy(true, 0.5, 0.25, 100, 2));
        SessionEntity exhausted = session(null, null);
        exhausted.setId(fixture.sessionId);
        exhausted.setAgentId("agent-1");
        exhausted.setUserId(fixture.userId);
        exhausted.setUsedTokens(100L);
        exhausted.setTokenLimit(100L);

        when(fixture.sessionDomainService.getSession(fixture.sessionId, fixture.userId))
                .thenReturn(fixture.session, exhausted);
        when(fixture.llmPort.call(any(LLMEntity.class)))
                .thenReturn(new LLMResult("压缩后的旧信息", TokenUsage.provider(100, 20, 120), "stop"));
        when(fixture.sessionDomainService.saveMemory(
                eq(fixture.sessionId),
                eq(fixture.userId),
                isNull(),
                any(SessionMemory.class))).thenReturn(true);

        try {
            fixture.service.doConversation(fixture.sessionId, fixture.userId, "current");
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.TOKEN_LIMIT_EXCEEDED.getCode(), e.getCode());
        }

        verify(fixture.llmPort, times(1)).call(any(LLMEntity.class));
    }

    private Fixture fixture(List<MessageEntity> messages, ContextSummaryPolicy policy) {
        return fixture(messages, session(null, null), policy);
    }

    private Fixture fixture(
            List<MessageEntity> messages,
            SessionEntity session,
            ContextSummaryPolicy policy) {
        ISessionDomainService sessionDomainService = mock(ISessionDomainService.class);
        IAgentDomainService agentDomainService = mock(IAgentDomainService.class);
        IApiKeyDomainService apiKeyDomainService = mock(IApiKeyDomainService.class);
        IMessageDomainService messageDomainService = mock(IMessageDomainService.class);
        LLMPort llmPort = mock(LLMPort.class);
        TokenEstimatorPort tokenEstimatorPort = mock(TokenEstimatorPort.class);
        ContextWindowPort contextWindowPort = mock(ContextWindowPort.class);
        ContextSummaryPolicyPort contextSummaryPolicyPort = mock(ContextSummaryPolicyPort.class);

        String sessionId = "session-1";
        String userId = "user-1";
        session.setId(sessionId);
        session.setAgentId("agent-1");
        session.setUserId(userId);
        session.setUsedTokens(0L);

        AgentEntity agent = new AgentEntity();
        agent.setId("agent-1");
        agent.setUserId(userId);
        agent.setApiKeyId("key-1");
        agent.setModelId("test-model");
        agent.setTools(List.of());

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId("key-1");
        apiKey.setUserId(userId);
        apiKey.setProvider("openai");
        apiKey.setApiKey("test-key");
        apiKey.setBaseUrl("https://example.test");
        apiKey.setEnabled(true);

        when(sessionDomainService.getSession(sessionId, userId)).thenReturn(session);
        when(agentDomainService.getAgent("agent-1", userId)).thenReturn(agent);
        when(apiKeyDomainService.getApiKey("key-1", userId)).thenReturn(apiKey);
        when(messageDomainService.listMessages(sessionId, userId)).thenReturn(messages);
        when(contextWindowPort.getContextWindow("test-model")).thenReturn(new ContextWindow(2_000, 0));
        when(contextSummaryPolicyPort.getPolicy()).thenReturn(policy);
        when(tokenEstimatorPort.estimate(any())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            return text == null ? 0 : text.length();
        });

        ConversationServiceImpl service = new ConversationServiceImpl();
        ReflectionTestUtils.setField(service, "sessionDomainService", sessionDomainService);
        ReflectionTestUtils.setField(service, "agentDomainService", agentDomainService);
        ReflectionTestUtils.setField(service, "apiKeyDomainService", apiKeyDomainService);
        ReflectionTestUtils.setField(service, "messageDomainService", messageDomainService);
        ReflectionTestUtils.setField(service, "llmPort", llmPort);
        ReflectionTestUtils.setField(service, "tokenEstimatorPort", tokenEstimatorPort);
        ReflectionTestUtils.setField(service, "contextWindowPort", contextWindowPort);
        ReflectionTestUtils.setField(service, "contextSummaryPolicyPort", contextSummaryPolicyPort);

        return new Fixture(service, sessionDomainService, llmPort, sessionId, userId, session);
    }

    private SessionEntity session(String summary, String throughMessageId) {
        SessionEntity session = new SessionEntity();
        session.setContextSummary(summary);
        session.setSummaryThroughMessageId(throughMessageId);
        session.setSummaryUpdatedAt(summary == null ? null : LocalDateTime.now());
        return session;
    }

    private List<MessageEntity> messages() {
        return userMessages().all();
    }

    private UserMessages userMessages() {
        MessageEntity oldUser = message("u1", "user", "a".repeat(800));
        MessageEntity oldAssistant = message("a1", "assistant", "a".repeat(700));
        MessageEntity recentUser = message("u2", "user", "recent question");
        MessageEntity recentAssistant = message("a2", "assistant", "recent answer");
        MessageEntity current = message("u3", "user", "current");
        return new UserMessages(
                List.of(oldUser, oldAssistant, recentUser, recentAssistant, current),
                oldUser,
                oldAssistant,
                recentUser,
                recentAssistant,
                current);
    }

    private MessageEntity message(String id, String role, String content) {
        MessageEntity message = new MessageEntity();
        message.setId(id);
        message.setSessionId("session-1");
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    private record Fixture(
            ConversationServiceImpl service,
            ISessionDomainService sessionDomainService,
            LLMPort llmPort,
            String sessionId,
            String userId,
            SessionEntity session) {
    }

    private record UserMessages(
            List<MessageEntity> all,
            MessageEntity oldUser,
            MessageEntity oldAssistant,
            MessageEntity recentUser,
            MessageEntity recentAssistant,
            MessageEntity current) {
    }
}
