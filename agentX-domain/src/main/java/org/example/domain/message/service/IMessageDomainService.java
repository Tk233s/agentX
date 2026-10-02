package org.example.domain.message.service;

import org.example.domain.message.model.entity.MessageEntity;

import java.util.List;

/**
 * 消息领域服务接口
 */
public interface IMessageDomainService {

    /**
     * 保存用户消息
     */
    MessageEntity saveUserMessage(String sessionId, String content);

    /**
     * 保存AI回复消息
     */
    MessageEntity saveAssistantMessage(String sessionId, String content, Integer tokens);

    /**
     * 保存已经组装好元数据的消息。
     */
    MessageEntity saveAssistantMessage(MessageEntity message);

    /**
     * 查询会话历史消息
     */
    List<MessageEntity> listMessages(String sessionId, String userId);

    /**
     * 删除会话所有消息
     */
    void deleteMessagesBySessionId(String sessionId);
}
