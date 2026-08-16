package org.example.domain.session.service;

import org.example.domain.session.model.entity.SessionEntity;

import java.util.List;

/**
 * 会话领域服务接口
 */
public interface ISessionDomainService {

    /**
     * 创建会话
     */
    SessionEntity createSession(SessionEntity session);

    /**
     * 重命名会话
     */
    SessionEntity updateSessionTitle(String id, String title);

    /**
     * 删除会话（级联删除消息由Message仓储负责）
     */
    void deleteSession(String id);

    /**
     * 根据ID查询会话
     */
    SessionEntity getSession(String id);

    /**
     * 根据用户ID查询所有会话
     */
    List<SessionEntity> listSessions(String userId);
}
