package org.example.infrastructure.llm;

import org.example.domain.llm.model.entity.LLMEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AnthropicClient implements LLMClient{

    @Override
    public String call(LLMEntity llmEntity) {
        return null;
    }

    @Override
    public Flux<String> stream(LLMEntity llmEntity) {
        return null;
    }
}
