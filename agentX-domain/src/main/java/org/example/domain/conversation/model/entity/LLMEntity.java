package org.example.domain.conversation.model.entity;

import lombok.Builder;
import lombok.Data;
import org.example.domain.message.model.entity.MessageEntity;

import java.util.List;

@Data
@Builder
public class LLMEntity {

    private String model;
    private String apiKey;
    private String baseUrl;
    private Double temperature;
    private Integer maxTokens;

    private String provider;

    private String systemPrompt;
    private List<MessageEntity> messages;
}
