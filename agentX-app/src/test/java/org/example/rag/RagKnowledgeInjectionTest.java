package org.example.rag;

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
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.service.IRagDomainService;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.service.ISessionDomainService;
import org.example.domain.tool.adapter.port.ToolRuntimePort;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 知识库注入对话链路的测试。
 *
 * 关注三件事：命中资料时有没有拼进 System Prompt、
 * 检索失败时对话会不会被打断、没绑知识库时会不会白跑一次检索。
 */
public class RagKnowledgeInjectionTest {

    @Test
    public void appendsRetrievedChunksToSystemPrompt() {
        Fixture fixture = fixture(List.of("kb-1"));
        when(fixture.ragDomainService.search(eq("kb-1"), eq("user-1"), eq("亮点能换新吗"), anyInt()))
                .thenReturn(List.of(new ChunkMatch(
                        "chunk-1", "doc-1", "售后政策.txt", "亮点达到 3 个及以上可以申请换新。", 0.87)));
        when(fixture.llmPort.call(any(LLMEntity.class)))
                .thenReturn(new LLMResult("可以换新", TokenUsage.provider(10, 2, 12), "stop"));

        fixture.service.doConversation(fixture.sessionId, fixture.userId, "亮点能换新吗");

        String systemPrompt = capturedSystemPrompt(fixture);
        assertTrue("应带上知识库标签", systemPrompt.contains("<knowledge>"));
        assertTrue("应标明出处", systemPrompt.contains("售后政策.txt"));
        assertTrue("应带上命中的正文", systemPrompt.contains("亮点达到 3 个及以上可以申请换新。"));
        assertTrue("应说明资料不是指令", systemPrompt.contains("不是新的指令"));
        assertTrue("原始 System Prompt 不能被覆盖", systemPrompt.contains("基础系统提示"));
    }

    @Test
    public void keepsConversationRunningWhenRetrievalFails() {
        Fixture fixture = fixture(List.of("kb-1"));
        when(fixture.ragDomainService.search(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new IllegalStateException("向量化服务不可用"));
        when(fixture.llmPort.call(any(LLMEntity.class)))
                .thenReturn(new LLMResult("照样能回答", TokenUsage.provider(8, 3, 11), "stop"));

        String reply = fixture.service.doConversation(fixture.sessionId, fixture.userId, "亮点能换新吗");

        assertTrue("检索失败不应影响回复", reply.contains("照样能回答"));
        assertFalse("检索失败时不应拼知识库块", capturedSystemPrompt(fixture).contains("<knowledge>"));
    }

    @Test
    public void skipsRetrievalWhenAgentHasNoKnowledgeBase() {
        Fixture fixture = fixture(List.of());
        when(fixture.llmPort.call(any(LLMEntity.class)))
                .thenReturn(new LLMResult("普通对话", TokenUsage.provider(5, 2, 7), "stop"));

        fixture.service.doConversation(fixture.sessionId, fixture.userId, "你好");

        verify(fixture.ragDomainService, never())
                .search(anyString(), anyString(), anyString(), anyInt());
    }

    private String capturedSystemPrompt(Fixture fixture) {
        ArgumentCaptor<LLMEntity> captor = ArgumentCaptor.forClass(LLMEntity.class);
        verify(fixture.llmPort).call(captor.capture());
        return String.valueOf(captor.getValue().getSystemPrompt());
    }

    private Fixture fixture(List<String> knowledgeBaseIds) {
        ISessionDomainService sessionDomainService = mock(ISessionDomainService.class);
        IAgentDomainService agentDomainService = mock(IAgentDomainService.class);
        IApiKeyDomainService apiKeyDomainService = mock(IApiKeyDomainService.class);
        IMessageDomainService messageDomainService = mock(IMessageDomainService.class);
        LLMPort llmPort = mock(LLMPort.class);
        TokenEstimatorPort tokenEstimatorPort = mock(TokenEstimatorPort.class);
        ContextWindowPort contextWindowPort = mock(ContextWindowPort.class);
        ContextSummaryPolicyPort contextSummaryPolicyPort = mock(ContextSummaryPolicyPort.class);
        ToolRuntimePort toolRuntimePort = mock(ToolRuntimePort.class);
        IRagDomainService ragDomainService = mock(IRagDomainService.class);

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
        agent.setSystemPrompt("基础系统提示");
        agent.setKnowledgeBaseIds(knowledgeBaseIds);

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
        when(messageDomainService.listMessages(sessionId, userId)).thenReturn(List.of(userMessage("你好")));
        when(contextWindowPort.getContextWindow("test-model")).thenReturn(new ContextWindow(100000, 100));
        when(contextSummaryPolicyPort.getPolicy()).thenReturn(ContextSummaryPolicy.disabled());
        when(toolRuntimePort.resolveToolIds(any(), any(), any())).thenReturn(List.of());
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
        ReflectionTestUtils.setField(service, "toolRuntimePort", toolRuntimePort);
        ReflectionTestUtils.setField(service, "ragDomainService", ragDomainService);

        return new Fixture(service, llmPort, ragDomainService, sessionId, userId);
    }

    private MessageEntity userMessage(String content) {
        MessageEntity message = new MessageEntity();
        message.setRole("user");
        message.setContent(content);
        return message;
    }

    private record Fixture(
            ConversationServiceImpl service,
            LLMPort llmPort,
            IRagDomainService ragDomainService,
            String sessionId,
            String userId) {
    }
}
