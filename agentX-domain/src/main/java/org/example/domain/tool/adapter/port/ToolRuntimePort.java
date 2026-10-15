package org.example.domain.tool.adapter.port;

import org.example.domain.tool.model.valobj.ToolDefinition;
import org.example.domain.tool.model.valobj.ToolExecutionResult;
import org.example.domain.tool.model.valobj.ToolInvocationContext;

import java.util.List;
import java.util.Map;

/**
 * 工具运行时端口。
 *
 * 领域层只依赖工具定义、绑定校验和标准化执行结果，
 * 不感知 Spring AI、反射或具体工具实现。
 */
public interface ToolRuntimePort {

    /**
     * 返回当前运行时可以绑定的全部工具。
     */
    List<ToolDefinition> listDefinitions();

    /**
     * 解析某次对话实际生效的工具列表。
     *
     * 能力模式决定默认工具，explicitToolIds 作为高级覆盖项叠加，
     * 两者合并去重后再按会话权限收窄；返回结果可直接用于 {@link #validateBindings(List)}。
     *
     * permissionMode 为空表示不按会话权限收窄（例如校验 Agent 自身配置时）。
     * 权限只会剔除工具，不会新增工具，因此会话无法绕过 Agent 的能力边界。
     */
    List<String> resolveToolIds(
            String capabilityProfile,
            List<String> explicitToolIds,
            String permissionMode);

    /**
     * 校验 Agent 绑定的工具 ID 是否全部存在。
     */
    void validateBindings(List<String> toolIds);

    /**
     * 手动执行一次工具方法，主要用于管理后台调试。
     */
    ToolExecutionResult execute(
            String toolId,
            String methodName,
            Map<String, Object> arguments,
            ToolInvocationContext context);
}
