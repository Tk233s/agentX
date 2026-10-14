package org.example.domain.tool.model.valobj;

import java.util.List;

/**
 * 工具内单个可调用方法的描述。
 */
public record ToolMethodDefinition(
        String name,
        String description,
        List<ToolParameterDefinition> parameters) {

    public ToolMethodDefinition {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
    }
}
