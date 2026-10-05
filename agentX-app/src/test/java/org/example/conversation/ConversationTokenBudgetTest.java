package org.example.conversation;

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
import org.example.domain.conversation.service.Impl.ConversationServiceImpl;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.domain.message.service.IMessageDomainService;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.model.valobj.SessionTokenBudget;
import org.example.domain.session.service.ISessionDomainService;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConversationTokenBudgetTest {

    @Test
    public void nonStreamConversationAccumulatesProviderUsage() {
        Fixture fixture = fixture(10L, 100L);
        when(fixture.llmPort.call(any(LLMEntity.class)))
                .thenReturn(new LLMResult("ok", TokenUsage.provider(7, 3, 10), "stop"));

        String reply = fixture.service.doConversation(fixture.sessionId, fixture.userId, "hello");

        assertEquals("ok", reply);
        verify(fixture.sessionDomainService).addUsedTokens(fixture.sessionId, fixture.userId, 10L);
    }

    @Test
    public void streamConversationAllowsCurrentAnswerAndMarksBudgetReachedAfterward() {
        Fixture fixture = fixture(95L, 100L);
        when(fixture.llmPort.stream(any(LLMEntity.class)))
                .thenReturn(Flux.just(new LLMStreamChunk("ok", TokenUsage.provider(8, 2, 10), "stop")));
        when(fixture.sessionDomainService.addUsedTokens(fixture.sessionId, fixture.userId, 10L))
                .thenReturn(SessionTokenBudget.of(100L, 105L));

        List<ConversationStreamEvent> events = fixture.service
                .streamConversation(fixture.sessionId, fixture.userId, "hello")
                .collectList()
                .block();

        ConversationStreamEvent done = events.get(events.size() - 1);
        assertEquals(ConversationStreamEvent.EventType.DONE, done.type());
        assertEquals(Long.valueOf(105L), done.usedTokens());
        assertTrue(done.limitReached());
        InOrder persistOrder = inOrder(fixture.messageDomainService);
        persistOrder.verify(fixture.messageDomainService)
                .saveUserMessage(fixture.sessionId, "hello");
        persistOrder.verify(fixture.messageDomainService)
                .saveAssistantMessage(any(MessageEntity.class));
        verify(fixture.sessionDomainService).addUsedTokens(fixture.sessionId, fixture.userId, 10L);
    }

    @Test
    public void streamFailureBeforeFirstChunkKeepsUserMessage() {
        Fixture fixture = fixture(0L, null);
        when(fixture.llmPort.stream(any(LLMEntity.class)))
                .thenReturn(Flux.error(new IllegalStateException("provider failed")));

        try {
            fixture.service.streamConversation(fixture.sessionId, fixture.userId, "hello")
                    .collectList()
                    .block();
            fail("Expected stream failure");
        } catch (IllegalStateException e) {
            assertEquals("provider failed", e.getMessage());
        }

        verify(fixture.messageDomainService).saveUserMessage(fixture.sessionId, "hello");
        verify(fixture.messageDomainService, never()).saveAssistantMessage(any(MessageEntity.class));
    }

    @Test
    public void exhaustedSessionIsRejectedBeforeCallingModel() {
        Fixture fixture = fixture(100L, 100L);

        try {
            fixture.service.doConversation(fixture.sessionId, fixture.userId, "hello");
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.TOKEN_LIMIT_EXCEEDED.getCode(), e.getCode());
        }

        verify(fixture.llmPort, never()).call(any(LLMEntity.class));
        verify(fixture.messageDomainService, never()).saveUserMessage(any(), any());
    }

    private Fixture fixture(long usedTokens, Long tokenLimit) {
        ISessionDomainService sessionDomainService = mock(ISessionDomainService.class);
        IAgentDomainService agentDomainService = mock(IAgentDomainService.class);
        IApiKeyDomainService apiKeyDomainService = mock(IApiKeyDomainService.class);
        IMessageDomainService messageDomainService = mock(IMessageDomainService.class);
        LLMPort llmPort = mock(LLMPort.class);
        TokenEstimatorPort tokenEstimatorPort = mock(TokenEstimatorPort.class);

        String sessionId = "session-1";
        String userId = "user-1";

        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        session.setAgentId("agent-1");
        session.setUserId(userId);
        session.setUsedTokens(usedTokens);
        session.setTokenLimit(tokenLimit);

        AgentEntity agent = new AgentEntity();
        agent.setId("agent-1");
        agent.setUserId(userId);
        agent.setApiKeyId("key-1");
        agent.setModelId("test-model");
        agent.setTools(Collections.emptyList());

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId("key-1");
        apiKey.setUserId(userId);
        apiKey.setName("测试密钥");
        apiKey.setProvider("openai");
        apiKey.setApiKey("test-key");
        apiKey.setBaseUrl("https://example.test");

        when(sessionDomainService.getSession(sessionId, userId)).thenReturn(session);
        when(agentDomainService.getAgent("agent-1", userId)).thenReturn(agent);
        when(apiKeyDomainService.getApiKey("key-1", userId)).thenReturn(apiKey);
        when(messageDomainService.listMessages(sessionId, userId)).thenReturn(Collections.emptyList());

        ConversationServiceImpl service = new ConversationServiceImpl();
        ReflectionTestUtils.setField(service, "sessionDomainService", sessionDomainService);
        ReflectionTestUtils.setField(service, "agentDomainService", agentDomainService);
        ReflectionTestUtils.setField(service, "apiKeyDomainService", apiKeyDomainService);
        ReflectionTestUtils.setField(service, "messageDomainService", messageDomainService);
        ReflectionTestUtils.setField(service, "llmPort", llmPort);
        ReflectionTestUtils.setField(service, "tokenEstimatorPort", tokenEstimatorPort);

        return new Fixture(
                service,
                sessionDomainService,
                messageDomainService,
                llmPort,
                sessionId,
                userId);
    }

    private record Fixture(
            ConversationServiceImpl service,
            ISessionDomainService sessionDomainService,
            IMessageDomainService messageDomainService,
            LLMPort llmPort,
            String sessionId,
            String userId) {
    }
}
