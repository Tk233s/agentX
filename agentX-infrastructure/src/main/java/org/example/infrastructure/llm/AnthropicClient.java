package org.example.infrastructure.llm;

import org.example.domain.conversation.adapter.port.LLMPort;
import org.example.domain.conversation.model.entity.LLMEntity;
import org.example.domain.conversation.model.entity.LLMResult;
import org.example.domain.conversation.model.entity.LLMStreamChunk;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AnthropicClient implements LLMPort {

    @Override
    public LLMResult call(LLMEntity llmEntity) {
        return null;
    }

    @Override
    public Flux<LLMStreamChunk> stream(LLMEntity llmEntity) {
        return null;
    }
}
