package org.example.infrastructure.llm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LLMClientFactory {

    @Autowired
    private OpenAIClient openAIClient;

    @Autowired
    private AnthropicClient anthropicClient;

    public LLMClient getCLient(String provider){
        if ("openai".equals(provider)) {
            return openAIClient;
        }

        if ("anthropic".equals(provider)) {
            return anthropicClient;
        }

        throw new IllegalArgumentException(
                "Unsupported provider: " + provider
        );
    }

}
