package org.example.infrastructure.adapter.repository;

import org.example.domain.message.adapter.repository.MessageRepository;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.infrastructure.dao.IMessageDao;
import org.example.infrastructure.dao.po.MessagePO;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息仓储实现
 */
@Repository
public class MessageRepositoryImpl implements MessageRepository {

    @Resource
    private IMessageDao messageDao;

    @Override
    public void save(MessageEntity message) {
        messageDao.insert(toPO(message));
    }

    @Override
    public List<MessageEntity> queryBySessionId(String sessionId) {
        List<MessagePO> pos = messageDao.queryBySessionId(sessionId);
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }
        return pos.stream().map(this::toEntity).collect(Collectors.toList());
    }

    @Override
    public int deleteBySessionId(String sessionId) {
        return messageDao.deleteBySessionId(sessionId);
    }

    private MessagePO toPO(MessageEntity entity) {
        MessagePO po = new MessagePO();
        po.setId(entity.getId());
        po.setSessionId(entity.getSessionId());
        po.setRole(entity.getRole());
        po.setContent(entity.getContent());
        po.setTokens(entity.getTokens());
        po.setCreatedAt(entity.getCreatedAt());
        return po;
    }

    private MessageEntity toEntity(MessagePO po) {
        if (po == null) {
            return null;
        }
        MessageEntity entity = new MessageEntity();
        entity.setId(po.getId());
        entity.setSessionId(po.getSessionId());
        entity.setRole(po.getRole());
        entity.setContent(po.getContent());
        entity.setTokens(po.getTokens());
        entity.setCreatedAt(po.getCreatedAt());
        return entity;
    }
}
