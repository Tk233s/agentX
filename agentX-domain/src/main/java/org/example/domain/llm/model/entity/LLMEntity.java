package org.example.domain.llm.model.entity;

import lombok.Data;
import org.example.domain.message.model.entity.MessageEntity;

import java.util.List;

@Data
public class LLMEntity {

    private String model;
    private String apiKey;
    private String baseUrl;
    private Double temperature;
    private Integer maxTokens;


    private String systemPrompt;
    private List<MessageEntity> messages;
}
