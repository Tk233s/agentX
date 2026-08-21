package org.example.infrastructure.llm;

import org.example.domain.conversation.adapter.port.LLMPort;
import org.example.domain.conversation.model.entity.LLMEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AnthropicClient implements LLMPort {

    @Override
    public String call(LLMEntity llmEntity) {
        return null;
    }

    @Override
    public Flux<String> stream(LLMEntity llmEntity) {
        return null;
    }
}
