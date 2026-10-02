package org.example.infrastructure.llm;

import org.example.domain.conversation.adapter.port.LLMPort;
import org.example.domain.conversation.model.entity.LLMEntity;
import org.example.domain.conversation.model.entity.LLMResult;
import org.example.domain.conversation.model.entity.LLMStreamChunk;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class LLMClientFactory implements LLMPort {

    private final OpenAIClient openAIClient;
    private final AnthropicClient anthropicClient;

    public LLMClientFactory(OpenAIClient openAIClient, AnthropicClient anthropicClient) {
        this.openAIClient = openAIClient;
        this.anthropicClient = anthropicClient;
    }

    @Override
    public LLMResult call(LLMEntity llmEntity) {
        return getClient(llmEntity.getProvider()).call(llmEntity);
    }

    @Override
    public Flux<LLMStreamChunk> stream(LLMEntity llmEntity) {
        return getClient(llmEntity.getProvider()).stream(llmEntity);
    }

    //根据供应商选择不同的服务
    private LLMPort getClient(String provider) {
        if ("openai".equals(provider)) {
            return openAIClient;
        }
        if ("anthropic".equals(provider)) {
            return anthropicClient;
        }
        throw new IllegalArgumentException("Unsupported provider: " + provider);
    }
}
