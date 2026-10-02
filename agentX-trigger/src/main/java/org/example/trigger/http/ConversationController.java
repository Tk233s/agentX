package org.example.trigger.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.api.response.Response;
import org.example.domain.conversation.model.entity.ConversationStreamEvent;
import org.example.domain.conversation.service.IConversationService;
import org.example.trigger.dto.conversation.ConversationReq;
import org.example.types.context.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 对话接口
 */
@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @Autowired
    private IConversationService conversationService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 同步对话
     */
    @PostMapping("/chat")
    public Response<String> chat(@RequestBody @Validated ConversationReq req) {
        String reply = conversationService.doConversation(
                req.getSessionId(),
                UserContext.requireCurrentUserId(),
                req.getContent());
        return Response.success(reply);
    }

    /**
     * 流式对话
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestBody @Validated ConversationReq req) {
        String userId = UserContext.requireCurrentUserId();

        return conversationService.streamConversation(req.getSessionId(), userId, req.getContent())
                .map(this::toServerSentEvent)
                .onErrorResume(error -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(error.getMessage() == null ? "流式对话失败" : error.getMessage())
                        .build()));
    }

    private ServerSentEvent<String> toServerSentEvent(ConversationStreamEvent event) {
        return switch (event.type()) {
            case DELTA -> ServerSentEvent.builder(event.content())
                    .event("delta")
                    .build();
            case ERROR -> ServerSentEvent.builder(event.content())
                    .event("error")
                    .build();
            default -> ServerSentEvent.builder(toJson(event))
                    .event(event.type().name().toLowerCase())
                    .build();
        };
    }

    private String toJson(ConversationStreamEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
