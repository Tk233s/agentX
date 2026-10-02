package org.example.domain.message.service.Impl;

import org.example.domain.message.adapter.repository.MessageRepository;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.domain.message.service.IMessageDomainService;
import org.example.domain.session.adapter.repository.SessionRepository;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
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

    @Resource
    private SessionRepository sessionRepository;

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

    @Transactional
    public MessageEntity saveAssistantMessage(MessageEntity message) {
        messageRepository.save(message);
        return message;
    }

    public List<MessageEntity> listMessages(String sessionId, String userId) {
        SessionEntity session = sessionRepository.findById(sessionId);
        if (session == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话不存在");
        }
        if (userId == null || !userId.equals(session.getUserId())) {
            throw new AppException(ResponseCode.FORBIDDEN.getCode(), "无权访问该会话");
        }
        return messageRepository.queryBySessionId(sessionId);
    }

    @Transactional
    public void deleteMessagesBySessionId(String sessionId) {
        messageRepository.deleteBySessionId(sessionId);
    }
}
