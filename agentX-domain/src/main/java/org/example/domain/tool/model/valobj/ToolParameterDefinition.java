package org.example.domain.tool.model.valobj;

/**
 * 工具方法参数描述。
 */
public record ToolParameterDefinition(
        String name,
        String description,
        String type,
        boolean required) {
}
