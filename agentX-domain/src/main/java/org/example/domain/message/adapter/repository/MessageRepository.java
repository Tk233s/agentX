package org.example.domain.message.adapter.repository;

import org.example.domain.message.model.entity.MessageEntity;

import java.util.List;

/**
 * 消息仓储接口（端口）
 */
public interface MessageRepository {

    /**
     * 新增消息
     */
    void save(MessageEntity message);

    /**
     * 根据会话ID查询所有消息（按创建时间正序）
     */
    List<MessageEntity> queryBySessionId(String sessionId);

    /**
     * 根据会话ID删除所有消息（删除会话时级联调用）
     */
    int deleteBySessionId(String sessionId);
}
