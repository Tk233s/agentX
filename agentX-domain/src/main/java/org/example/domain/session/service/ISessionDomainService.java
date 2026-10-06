package org.example.domain.session.service;

import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.model.valobj.SessionMemory;
import org.example.domain.session.model.valobj.SessionTokenBudget;

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
    SessionEntity updateSessionTitle(String id, String userId, String title);

    /**
     * 删除会话（级联删除消息由Message仓储负责）
     */
    void deleteSession(String id, String userId);

    /**
     * 根据ID查询会话
     */
    SessionEntity getSession(String id, String userId);

    /**
     * 根据用户ID查询所有会话
     */
    List<SessionEntity> listSessions(String userId);

    /**
     * 累加会话已使用Token，并返回累加后的预算状态。
     */
    SessionTokenBudget addUsedTokens(String id, String userId, long tokens);

    /**
     * 保存会话摘要；expectedThroughMessageId 为本次更新前的水位。
     */
    boolean saveMemory(String id, String userId, String expectedThroughMessageId, SessionMemory memory);
}
