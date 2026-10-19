package org.example.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.conversation.model.entity.ConversationStreamEvent;
import org.example.domain.conversation.service.IConversationService;
import org.example.domain.conversation.model.valobj.KnowledgeCitation;
import org.example.domain.conversation.model.valobj.TokenUsage;
import org.example.domain.session.model.valobj.SessionTokenBudget;
import org.example.trigger.dto.conversation.ConversationReq;
import org.example.trigger.http.ConversationController;
import org.example.types.context.UserContext;
import org.example.types.exception.AppException;
import org.junit.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConversationStreamErrorHandlingTest {

    @Test
    public void convertsSynchronousServiceFailureToSseErrorEvent() {
        IConversationService service = mock(IConversationService.class);
        when(service.streamConversation(anyString(), anyString(), anyString()))
                .thenThrow(new AppException("0002", "服务商暂不支持流式对话"));
        ConversationController controller = controller(service);
        UserContext.setCurrentUserId("user-1");

        try {
            List<ServerSentEvent<String>> events = controller.stream(request())
                    .collectList()
                    .block();

            assertEquals(1, events.size());
            assertEquals("error", events.get(0).event());
            assertEquals("服务商暂不支持流式对话", events.get(0).data());
        } finally {
            UserContext.clear();
        }
    }

    @Test
    public void convertsAsynchronousServiceFailureToSseErrorEvent() {
        IConversationService service = mock(IConversationService.class);
        when(service.streamConversation(anyString(), anyString(), anyString()))
                .thenReturn(Flux.error(new AppException("0002", "模型调用失败")));
        ConversationController controller = controller(service);
        UserContext.setCurrentUserId("user-1");

        try {
            List<ServerSentEvent<String>> events = controller.stream(request())
                    .collectList()
                    .block();

            assertEquals(1, events.size());
            assertEquals("error", events.get(0).event());
            assertEquals("模型调用失败", events.get(0).data());
        } finally {
            UserContext.clear();
        }
    }

    @Test
    public void serializesKnowledgeCitationsInDoneEvent() throws Exception {
        IConversationService service = mock(IConversationService.class);
        when(service.streamConversation(anyString(), anyString(), anyString()))
                .thenReturn(Flux.just(ConversationStreamEvent.done(
                        "message-1",
                        "stop",
                        120L,
                        TokenUsage.provider(10, 4, 14),
                        SessionTokenBudget.of(null, 14L),
                        List.of(new KnowledgeCitation(
                                "kb-1",
                                "售后政策.txt",
                                "亮点达到 3 个及以上可以申请换新。",
                                0.87)))));
        ConversationController controller = controller(service);
        UserContext.setCurrentUserId("user-1");

        try {
            List<ServerSentEvent<String>> events = controller.stream(request())
                    .collectList()
                    .block();

            assertEquals(1, events.size());
            assertEquals("done", events.get(0).event());
            var payload = new ObjectMapper().readTree(events.get(0).data());
            assertEquals("kb-1", payload.path("citations").get(0).path("knowledgeBaseId").asText());
            assertEquals("售后政策.txt", payload.path("citations").get(0).path("documentName").asText());
            assertEquals(0.87, payload.path("citations").get(0).path("score").asDouble(), 0.000001);
        } finally {
            UserContext.clear();
        }
    }

    private ConversationController controller(IConversationService service) {
        ConversationController controller = new ConversationController();
        ReflectionTestUtils.setField(controller, "conversationService", service);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        return controller;
    }

    private ConversationReq request() {
        ConversationReq request = new ConversationReq();
        request.setSessionId("session-1");
        request.setContent("hello");
        return request;
    }
}
