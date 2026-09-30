package org.example.trigger.http;

import org.example.api.response.Response;
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
                .map(content -> ServerSentEvent.builder(content)
                        .event("delta")
                        .build())
                .concatWithValues(ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build())
                .onErrorResume(error -> Flux.just(ServerSentEvent.<String>builder()
                        .event("error")
                        .data(error.getMessage() == null ? "流式对话失败" : error.getMessage())
                        .build()));
    }
}
