package org.example.infrastructure.llm;

import org.example.domain.conversation.adapter.port.ContextSummaryPolicyPort;
import org.example.domain.conversation.model.valobj.ContextSummaryPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 从应用配置读取上下文摘要策略。
 */
@Component
@ConfigurationProperties(prefix = "conversation.context.summary")
public class ConfiguredContextSummaryPolicyAdapter implements ContextSummaryPolicyPort {

    private boolean enabled = true;

    private double triggerRatio = 0.80;

    private double targetRatio = 0.65;

    /**
     * 摘要模型单次调用的总输出上限，包含推理内容与摘要正文。
     */
    private int maxSummaryTokens = 8192;

    private int minRecentGroups = 4;

    @Override
    public ContextSummaryPolicy getPolicy() {
        return new ContextSummaryPolicy(
                enabled,
                triggerRatio,
                targetRatio,
                maxSummaryTokens,
                minRecentGroups);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getTriggerRatio() {
        return triggerRatio;
    }

    public void setTriggerRatio(double triggerRatio) {
        this.triggerRatio = triggerRatio;
    }

    public double getTargetRatio() {
        return targetRatio;
    }

    public void setTargetRatio(double targetRatio) {
        this.targetRatio = targetRatio;
    }

    public int getMaxSummaryTokens() {
        return maxSummaryTokens;
    }

    public void setMaxSummaryTokens(int maxSummaryTokens) {
        this.maxSummaryTokens = maxSummaryTokens;
    }

    public int getMinRecentGroups() {
        return minRecentGroups;
    }

    public void setMinRecentGroups(int minRecentGroups) {
        this.minRecentGroups = minRecentGroups;
    }
}
