package org.example.agent;

import org.example.domain.agent.model.entity.AgentEntity;
import org.example.types.enums.AgentCapabilityProfile;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * 验证已废弃 RESEARCH 模式的兼容迁移。
 */
public class AgentCapabilityProfileTest {

    @Test
    public void legacyResearchWithoutToolsBecomesChat() {
        AgentEntity agent = legacyResearchAgent(List.of());

        agent.normalizeCapabilityProfile();

        assertEquals(AgentCapabilityProfile.CHAT.name(), agent.getCapabilityProfile());
    }

    @Test
    public void legacyResearchWithToolsBecomesCustom() {
        AgentEntity agent = legacyResearchAgent(List.of("file-read"));

        agent.normalizeCapabilityProfile();

        assertEquals(AgentCapabilityProfile.CUSTOM.name(), agent.getCapabilityProfile());
    }

    private AgentEntity legacyResearchAgent(List<String> tools) {
        AgentEntity agent = new AgentEntity();
        agent.setCapabilityProfile("RESEARCH");
        agent.setTools(tools);
        return agent;
    }
}
