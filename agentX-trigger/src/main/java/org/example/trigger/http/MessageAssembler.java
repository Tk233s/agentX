package org.example.trigger.http;

import org.example.domain.message.model.entity.MessageEntity;
import org.example.trigger.dto.message.MessageRes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息 DTO 转换器
 */
public class MessageAssembler {

    /** 领域实体 -> 出参 */
    public static MessageRes toRes(MessageEntity entity) {
        MessageRes res = new MessageRes();
        res.setId(entity.getId());
        res.setSessionId(entity.getSessionId());
        res.setRole(entity.getRole());
        res.setContent(entity.getContent());
        res.setTokens(entity.getTokens());
        res.setPromptTokens(entity.getPromptTokens());
        res.setCompletionTokens(entity.getCompletionTokens());
        res.setTotalTokens(entity.getTotalTokens());
        res.setModel(entity.getModel());
        res.setProvider(entity.getProvider());
        res.setFinishReason(entity.getFinishReason());
        res.setLatencyMs(entity.getLatencyMs());
        res.setUsageSource(entity.getUsageSource());
        res.setCreateTime(entity.getCreatedAt());
        return res;
    }

    /** 领域实体列表 -> 出参列表 */
    public static List<MessageRes> toResList(List<MessageEntity> entities) {
        return entities.stream().map(MessageAssembler::toRes).collect(Collectors.toList());
    }
}
