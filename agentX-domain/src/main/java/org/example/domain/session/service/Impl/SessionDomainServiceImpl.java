package org.example.domain.session.service.Impl;

import org.example.domain.agent.adapter.repository.AgentRepository;
import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.message.adapter.repository.MessageRepository;
import org.example.domain.session.adapter.repository.SessionRepository;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.model.valobj.SessionMemory;
import org.example.domain.session.model.valobj.SessionTokenBudget;
import org.example.domain.session.service.ISessionDomainService;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话领域服务实现
 */
@Service
public class SessionDomainServiceImpl implements ISessionDomainService {

    @Resource
    private SessionRepository sessionRepository;

    @Resource
    private MessageRepository messageRepository;

    @Resource
    private AgentRepository agentRepository;

    @Transactional
    public SessionEntity createSession(SessionEntity session) {
        if (session.getUserId() == null || session.getUserId().isBlank()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户ID不能为空");
        }
        AgentEntity agent = agentRepository.findById(session.getAgentId());
        if (agent == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "智能体不存在");
        }
        if (!session.getUserId().equals(agent.getUserId())) {
            throw new AppException(ResponseCode.FORBIDDEN.getCode(), "无权使用该智能体");
        }
        sessionRepository.save(session);
        return session;
    }

    @Transactional
    public SessionEntity updateSessionTitle(String id, String userId, String title) {
        SessionEntity existing = requireOwnedSession(id, userId);
        existing.setTitle(title);
        existing.setUpdatedAt(LocalDateTime.now());
        sessionRepository.update(existing);
        return existing;
    }

    /**
     * 删除会话：先级联删除该会话下的所有消息，再删除会话本身
     */
    @Transactional
    public void deleteSession(String id, String userId) {
        requireOwnedSession(id, userId);
        // 先删消息
        messageRepository.deleteBySessionId(id);
        // 再删会话
        int rows = sessionRepository.deleteById(id);
        if (rows == 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话不存在");
        }
    }

    public SessionEntity getSession(String id, String userId) {
        SessionEntity session = sessionRepository.findById(id);
        if (session == null) {
            return null;
        }
        checkOwner(session, userId);
        return session;
    }

    public List<SessionEntity> listSessions(String userId) {
        return sessionRepository.queryByUserId(userId);
    }

    @Transactional
    public SessionTokenBudget addUsedTokens(String id, String userId, long tokens) {
        if (tokens < 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "Token使用量不能为负数");
        }
        SessionEntity session = requireOwnedSession(id, userId);
        if (tokens > 0) {
            int rows = sessionRepository.incrementUsedTokens(id, tokens);
            if (rows == 0) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话不存在");
            }
            session = sessionRepository.findById(id);
        }
        return session.tokenBudget();
    }

    @Transactional
    public boolean saveMemory(
            String id,
            String userId,
            String expectedThroughMessageId,
            SessionMemory memory) {
        requireOwnedSession(id, userId);
        SessionMemory target = memory == null ? SessionMemory.empty() : memory;
        return sessionRepository.updateMemory(id, expectedThroughMessageId, target) > 0;
    }

    private SessionEntity requireOwnedSession(String id, String userId) {
        SessionEntity session = sessionRepository.findById(id);
        if (session == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话不存在");
        }
        checkOwner(session, userId);
        return session;
    }

    private void checkOwner(SessionEntity session, String userId) {
        if (userId == null || !userId.equals(session.getUserId())) {
            throw new AppException(ResponseCode.FORBIDDEN.getCode(), "无权访问该会话");
        }
    }
}
