package org.example.domain.conversation.adapter.port;

import org.example.domain.conversation.model.entity.LLMEntity;
import org.example.domain.conversation.model.entity.LLMResult;
import org.example.domain.conversation.model.entity.LLMStreamChunk;
import reactor.core.publisher.Flux;


public interface LLMPort {

    LLMResult call(LLMEntity llmEntity);

    Flux<LLMStreamChunk> stream(LLMEntity llmEntity);
}
