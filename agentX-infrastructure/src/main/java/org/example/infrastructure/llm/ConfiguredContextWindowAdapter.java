package org.example.infrastructure.llm;

import org.example.domain.conversation.adapter.port.ContextWindowPort;
import org.example.domain.conversation.model.valobj.ContextWindow;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 从应用配置读取当前默认模型的上下文窗口。
 */
@Component
@ConfigurationProperties(prefix = "conversation.context")
public class ConfiguredContextWindowAdapter implements ContextWindowPort {

    private int maxWindowTokens = 131_072;

    private int outputReserveTokens = 8_192;

    @Override
    public ContextWindow getContextWindow(String model) {
        return new ContextWindow(maxWindowTokens, outputReserveTokens);
    }

    public int getMaxWindowTokens() {
        return maxWindowTokens;
    }

    public void setMaxWindowTokens(int maxWindowTokens) {
        this.maxWindowTokens = maxWindowTokens;
    }

    public int getOutputReserveTokens() {
        return outputReserveTokens;
    }

    public void setOutputReserveTokens(int outputReserveTokens) {
        this.outputReserveTokens = outputReserveTokens;
    }
}
