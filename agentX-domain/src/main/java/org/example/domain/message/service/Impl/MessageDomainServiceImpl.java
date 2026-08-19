package org.example.domain.message.service.Impl;

import org.example.domain.message.adapter.repository.MessageRepository;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.domain.message.service.IMessageDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 消息领域服务实现
 */
@Service
public class MessageDomainServiceImpl implements IMessageDomainService {

    @Resource
    private MessageRepository messageRepository;

    @Transactional
    public MessageEntity saveUserMessage(String sessionId, String content) {
        MessageEntity message = MessageEntity.createUserMessage(sessionId, content);
        messageRepository.save(message);
        return message;
    }

    @Transactional
    public MessageEntity saveAssistantMessage(String sessionId, String content, Integer tokens) {
        MessageEntity message = MessageEntity.createAssistantMessage(sessionId, content, tokens);
        messageRepository.save(message);
        return message;
    }

    public List<MessageEntity> listMessages(String sessionId) {
        return messageRepository.queryBySessionId(sessionId);
    }

    @Transactional
    public void deleteMessagesBySessionId(String sessionId) {
        messageRepository.deleteBySessionId(sessionId);
    }
}
