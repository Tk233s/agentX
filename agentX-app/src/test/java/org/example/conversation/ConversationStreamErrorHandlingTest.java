package org.example.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.conversation.service.IConversationService;
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
