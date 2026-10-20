package org.example.infrastructure.adapter.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.domain.conversation.model.valobj.KnowledgeCitation;
import org.example.domain.message.adapter.repository.MessageRepository;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.infrastructure.dao.IMessageDao;
import org.example.infrastructure.dao.po.MessagePO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息仓储实现
 */
@Repository
public class MessageRepositoryImpl implements MessageRepository {

    private static final Gson GSON = new Gson();
    private static final Type CITATION_LIST_TYPE = new TypeToken<List<KnowledgeCitation>>() {}.getType();

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
        po.setPromptTokens(entity.getPromptTokens());
        po.setCompletionTokens(entity.getCompletionTokens());
        po.setTotalTokens(entity.getTotalTokens());
        po.setModel(entity.getModel());
        po.setProvider(entity.getProvider());
        po.setFinishReason(entity.getFinishReason());
        po.setLatencyMs(entity.getLatencyMs());
        po.setUsageSource(entity.getUsageSource());
        po.setCitationsJson(entity.getCitations() == null || entity.getCitations().isEmpty()
                ? null
                : GSON.toJson(entity.getCitations()));
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
        entity.setPromptTokens(po.getPromptTokens());
        entity.setCompletionTokens(po.getCompletionTokens());
        entity.setTotalTokens(po.getTotalTokens());
        entity.setModel(po.getModel());
        entity.setProvider(po.getProvider());
        entity.setFinishReason(po.getFinishReason());
        entity.setLatencyMs(po.getLatencyMs());
        entity.setUsageSource(po.getUsageSource());
        entity.setCitations(parseCitations(po.getCitationsJson()));
        entity.setCreatedAt(po.getCreatedAt());
        return entity;
    }

    private List<KnowledgeCitation> parseCitations(String citationsJson) {
        if (citationsJson == null || citationsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<KnowledgeCitation> citations = GSON.fromJson(citationsJson, CITATION_LIST_TYPE);
            return citations == null ? Collections.emptyList() : citations;
        } catch (RuntimeException ignored) {
            // 历史脏数据不应导致整段会话无法加载。
            return Collections.emptyList();
        }
    }
}
