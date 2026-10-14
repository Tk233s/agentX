package org.example.domain.tool.model.valobj;

/**
 * 工具调用所属的用户与会话上下文，用于审计记录。
 */
public record ToolInvocationContext(String userId, String sessionId) {

    public static ToolInvocationContext empty() {
        return new ToolInvocationContext(null, null);
    }
}
