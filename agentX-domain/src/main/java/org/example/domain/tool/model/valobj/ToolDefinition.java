package org.example.domain.tool.model.valobj;

import java.util.List;

/**
 * 可绑定到 Agent 的完整工具定义。
 */
public record ToolDefinition(
        String id,
        String name,
        String description,
        List<ToolMethodDefinition> methods) {

    public ToolDefinition {
        methods = methods == null ? List.of() : List.copyOf(methods);
    }
}
