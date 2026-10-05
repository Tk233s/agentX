package org.example.auth;

import org.example.domain.agent.adapter.repository.AgentRepository;
import org.example.domain.agent.model.entity.AgentEntity;
import org.example.domain.agent.service.Impl.AgentDomainServiceImpl;
import org.example.domain.apikey.adapter.repository.ApiKeyRepository;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.apikey.service.Impl.ApiKeyDomainServiceImpl;
import org.example.domain.message.adapter.repository.MessageRepository;
import org.example.domain.session.adapter.repository.SessionRepository;
import org.example.domain.session.model.entity.SessionEntity;
import org.example.domain.session.service.Impl.SessionDomainServiceImpl;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceOwnershipTest {

    @Test
    public void agentCannotBeReadByAnotherUser() {
        AgentRepository repository = mock(AgentRepository.class);
        AgentDomainServiceImpl service = new AgentDomainServiceImpl();
        ReflectionTestUtils.setField(service, "agentRepository", repository);

        AgentEntity agent = new AgentEntity();
        agent.setId("agent-1");
        agent.setUserId("user-a");
        when(repository.findById("agent-1")).thenReturn(agent);

        try {
            service.getAgent("agent-1", "user-b");
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.FORBIDDEN.getCode(), e.getCode());
        }
    }

    @Test
    public void apiKeyCannotBeUpdatedByAnotherUser() {
        ApiKeyRepository repository = mock(ApiKeyRepository.class);
        ApiKeyDomainServiceImpl service = new ApiKeyDomainServiceImpl();
        ReflectionTestUtils.setField(service, "apiKeyRepository", repository);

        ApiKeyEntity existing = new ApiKeyEntity();
        existing.setId("key-1");
        existing.setUserId("user-a");
        when(repository.findById("key-1")).thenReturn(existing);

        ApiKeyEntity update = new ApiKeyEntity();
        update.setId("key-1");
        update.setName("OpenAI 主账号");
        update.setProvider("openai");
        update.setApiKey("sk-test");

        try {
            service.updateApiKey(update, "user-b");
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.FORBIDDEN.getCode(), e.getCode());
        }
    }

    @Test
    public void sessionCannotReferenceAnotherUsersAgent() {
        SessionRepository sessionRepository = mock(SessionRepository.class);
        MessageRepository messageRepository = mock(MessageRepository.class);
        AgentRepository agentRepository = mock(AgentRepository.class);
        SessionDomainServiceImpl service = new SessionDomainServiceImpl();
        ReflectionTestUtils.setField(service, "sessionRepository", sessionRepository);
        ReflectionTestUtils.setField(service, "messageRepository", messageRepository);
        ReflectionTestUtils.setField(service, "agentRepository", agentRepository);

        AgentEntity agent = new AgentEntity();
        agent.setId("agent-1");
        agent.setUserId("user-a");
        when(agentRepository.findById("agent-1")).thenReturn(agent);

        SessionEntity session = SessionEntity.createNew("agent-1", "user-b", "title", null);

        try {
            service.createSession(session);
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.FORBIDDEN.getCode(), e.getCode());
        }
    }
}
