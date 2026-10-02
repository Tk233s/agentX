package org.example.domain.conversation.service;

import org.example.domain.conversation.model.entity.ConversationStreamEvent;
import reactor.core.publisher.Flux;

/**
 * 对话领域服务接口
 */
public interface IConversationService {

    /**
     * 同步对话：用户发消息，等LLM完整回复后返回
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param content   用户消息内容
     * @return AI回复内容
     */
    String doConversation(String sessionId, String userId, String content);

    /**
     * 流式对话：用户发消息，按增量持续返回 LLM 回复
     *
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param content   用户消息内容
     * @return AI回复内容流
     */
    Flux<ConversationStreamEvent> streamConversation(String sessionId, String userId, String content);
}
