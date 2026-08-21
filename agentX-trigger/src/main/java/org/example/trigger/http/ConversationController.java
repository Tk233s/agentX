package org.example.trigger.http;

import org.example.api.response.Response;
import org.example.domain.conversation.service.IConversationService;
import org.example.trigger.dto.conversation.ConversationReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                req.getUserId(),
                req.getContent());
        return Response.success(reply);
    }
}
