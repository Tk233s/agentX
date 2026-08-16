package org.example.trigger.http;

import org.example.api.response.Response;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.domain.message.service.IMessageDomainService;
import org.example.trigger.dto.message.MessageRes;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 消息管理（只读：查询历史消息）
 * 消息的创建由对话流程内部自动产生，不暴露给用户手动创建。
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    @Resource
    private IMessageDomainService messageDomainService;

    /**
     * 查询会话历史消息
     */
    @GetMapping("/list")
    public Response<List<MessageRes>> listMessages(@RequestParam String sessionId) {
        List<MessageEntity> messages = messageDomainService.listMessages(sessionId);
        return Response.success(MessageAssembler.toResList(messages));
    }
}
