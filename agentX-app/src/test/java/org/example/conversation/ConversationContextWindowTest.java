package org.example.conversation;

import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.IAgentDomainService;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.IApiKeyDomainService;
import org.example.domain.conversation.adapter.port.ContextWindowPort;
import org.example.domain.conversation.adapter.port.ContextSummaryPolicyPort;
import org.example.domain.conversation.adapter.port.LLMPort;
import org.example.domain.conversation.adapter.port.TokenEstimatorPort;
import org.example.domain.conversation.model.entity.LLMEntity;
import org.example.domain.conversation.model.entity.LLMResult;
import org.example.domain.conversation.model.valobj.ContextWindow;
import org.example.domain.conversation.model.valobj.ContextSummaryPolicy;
import org.example.domain.conversation.model.valobj.TokenUsage;
import org.example.domain.conversation.service.Impl.ConversationServiceImpl;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.domain.message.service.IMessageDomainService;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.service.ISessionDomainService;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConversationContextWindowTest {

    @Test
    public void keepsRecentCompleteGroupsAndDropsOldestGroup() {
        List<MessageEntity> messages = List.of(
                message("user", "old question"),
                message("assistant", "old answer"),
                message("user", "recent question"),
                message("assistant", "recent answer"),
                message("user", "current question"));
        Fixture fixture = fixture(messages, new ContextWindow(70, 5), "system");
        when(fixture.llmPort.call(any(LLMEntity.class)))
                .thenReturn(new LLMResult("ok", TokenUsage.provider(10, 2, 12), "stop"));

        fixture.service.doConversation(fixture.sessionId, fixture.userId, "current question");

        ArgumentCaptor<LLMEntity> captor = ArgumentCaptor.forClass(LLMEntity.class);
        verify(fixture.llmPort).call(captor.capture());
        assertEquals(
                List.of("recent question", "recent answer", "current question"),
                captor.getValue().getMessages().stream().map(MessageEntity::getContent).toList());
    }

    @Test
    public void keepsCurrentUserMessageWhenOlderGroupsDoNotFit() {
        List<MessageEntity> messages = List.of(
                message("user", "old question"),
                message("assistant", "old answer"),
                message("user", "current question"));
        Fixture fixture = fixture(messages, new ContextWindow(30, 5), null);
        when(fixture.llmPort.call(any(LLMEntity.class)))
                .thenReturn(new LLMResult("ok", TokenUsage.provider(5, 1, 6), "stop"));

        fixture.service.doConversation(fixture.sessionId, fixture.userId, "current question");

        ArgumentCaptor<LLMEntity> captor = ArgumentCaptor.forClass(LLMEntity.class);
        verify(fixture.llmPort).call(captor.capture());
        assertEquals(
                List.of("current question"),
                captor.getValue().getMessages().stream().map(MessageEntity::getContent).toList());
    }

    @Test
    public void rejectsCurrentMessageThatAloneExceedsInputWindow() {
        List<MessageEntity> messages = List.of(message("user", "current question"));
        Fixture fixture = fixture(messages, new ContextWindow(20, 5), null);

        try {
            fixture.service.doConversation(fixture.sessionId, fixture.userId, "current question");
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), e.getCode());
        }

        verify(fixture.llmPort, never()).call(any(LLMEntity.class));
    }

    private Fixture fixture(
            List<MessageEntity> messages,
            ContextWindow contextWindow,
            String systemPrompt) {
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

        SessionEntity session = new SessionEntity();
        session.setId(sessionId);
        session.setAgentId("agent-1");
        session.setUserId(userId);
        session.setUsedTokens(0L);

        AgentEntity agent = new AgentEntity();
        agent.setId("agent-1");
        agent.setUserId(userId);
        agent.setApiKeyId("key-1");
        agent.setModelId("test-model");
        agent.setSystemPrompt(systemPrompt);

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
        when(contextWindowPort.getContextWindow("test-model")).thenReturn(contextWindow);
        when(contextSummaryPolicyPort.getPolicy()).thenReturn(ContextSummaryPolicy.disabled());
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

        return new Fixture(service, llmPort, sessionId, userId);
    }

    private MessageEntity message(String role, String content) {
        MessageEntity message = new MessageEntity();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private record Fixture(
            ConversationServiceImpl service,
            LLMPort llmPort,
            String sessionId,
            String userId) {
    }
}
