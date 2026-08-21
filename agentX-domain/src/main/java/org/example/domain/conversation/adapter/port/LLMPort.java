package org.example.domain.conversation.adapter.port;

import org.example.domain.conversation.model.entity.LLMEntity;
import reactor.core.publisher.Flux;


public interface LLMPort {

    String call(LLMEntity llmEntity);

    Flux<String> stream(LLMEntity llmEntity);
}
