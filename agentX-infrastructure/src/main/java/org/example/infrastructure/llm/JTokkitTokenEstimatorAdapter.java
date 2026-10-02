package org.example.infrastructure.llm;

import org.example.domain.conversation.adapter.port.TokenEstimatorPort;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.stereotype.Component;

/**
 * 使用 Spring AI 内置 JTokkit 实现本地 Token 估算。
 */
@Component
public class JTokkitTokenEstimatorAdapter implements TokenEstimatorPort {

    private final TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();

    @Override
    public int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return tokenCountEstimator.estimate(text);
    }
}
