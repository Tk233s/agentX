package org.example.infrastructure.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.infrastructure.dao.po.MessagePO;

import java.util.List;

/**
 * 消息 DAO 接口
 */
@Mapper
public interface IMessageDao {

    void insert(MessagePO messagePO);

    List<MessagePO> queryBySessionId(String sessionId);

    int deleteBySessionId(String sessionId);
}
