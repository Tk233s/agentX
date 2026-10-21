package org.example.message;

import com.google.gson.Gson;
import org.example.domain.conversation.model.valobj.KnowledgeCitation;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.infrastructure.adapter.repository.MessageRepositoryImpl;
import org.example.infrastructure.dao.IMessageDao;
import org.example.infrastructure.dao.po.MessagePO;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证知识库引用与消息一起持久化和读取。
 */
public class MessageCitationPersistenceTest {

    @Test
    public void serializesCitationsWhenSavingMessage() {
        IMessageDao messageDao = mock(IMessageDao.class);
        MessageRepositoryImpl repository = new MessageRepositoryImpl();
        ReflectionTestUtils.setField(repository, "messageDao", messageDao);

        MessageEntity message = MessageEntity.createAssistantMessage("session-1", "回答", 10);
        message.setCitations(List.of(new KnowledgeCitation(
                "kb-1",
                "售后政策.txt",
                "满足条件可以换新",
                0.87D)));

        repository.save(message);

        org.mockito.ArgumentCaptor<MessagePO> captor =
                org.mockito.ArgumentCaptor.forClass(MessagePO.class);
        verify(messageDao).insert(captor.capture());
        KnowledgeCitation[] saved = new Gson().fromJson(
                captor.getValue().getCitationsJson(),
                KnowledgeCitation[].class);
        assertEquals(1, saved.length);
        assertEquals("kb-1", saved[0].knowledgeBaseId());
        assertEquals("售后政策.txt", saved[0].documentName());
        assertEquals(0.87D, saved[0].score(), 0.000001);
    }

    @Test
    public void restoresCitationsWhenLoadingMessage() {
        IMessageDao messageDao = mock(IMessageDao.class);
        MessageRepositoryImpl repository = new MessageRepositoryImpl();
        ReflectionTestUtils.setField(repository, "messageDao", messageDao);

        MessagePO po = new MessagePO();
        po.setId("message-1");
        po.setSessionId("session-1");
        po.setRole("assistant");
        po.setContent("回答");
        po.setCitationsJson(
                "[{\"knowledgeBaseId\":\"kb-1\",\"documentName\":\"售后政策.txt\","
                        + "\"snippet\":\"满足条件可以换新\",\"score\":0.87}]");
        when(messageDao.queryBySessionId("session-1")).thenReturn(List.of(po));

        List<MessageEntity> messages = repository.queryBySessionId("session-1");

        assertEquals(1, messages.size());
        assertEquals(1, messages.get(0).getCitations().size());
        assertEquals("售后政策.txt", messages.get(0).getCitations().get(0).documentName());
        assertEquals(0.87D, messages.get(0).getCitations().get(0).score(), 0.000001);
        assertEquals(null, messages.get(0).getCitations().get(0).matchedBy());
    }
}
