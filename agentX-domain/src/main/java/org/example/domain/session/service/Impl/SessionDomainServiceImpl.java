package org.example.domain.session.service.Impl;

import org.example.domain.message.adapter.repository.MessageRepository;
import org.example.domain.session.adapter.repository.SessionRepository;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.service.ISessionDomainService;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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

    @Transactional
    public SessionEntity createSession(SessionEntity session) {
        sessionRepository.save(session);
        return session;
    }

    @Transactional
    public SessionEntity updateSessionTitle(String id, String title) {
        SessionEntity existing = sessionRepository.findById(id);
        if (existing == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话不存在");
        }
        existing.setTitle(title);
        existing.setUpdatedAt(LocalDateTime.now());
        sessionRepository.update(existing);
        return existing;
    }

    /**
     * 删除会话：先级联删除该会话下的所有消息，再删除会话本身
     */
    @Transactional
    public void deleteSession(String id) {
        // 先删消息
        messageRepository.deleteBySessionId(id);
        // 再删会话
        int rows = sessionRepository.deleteById(id);
        if (rows == 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "会话不存在");
        }
    }

    public SessionEntity getSession(String id) {
        return sessionRepository.findById(id);
    }

    public List<SessionEntity> listSessions(String userId) {
        return sessionRepository.queryByUserId(userId);
    }
}
