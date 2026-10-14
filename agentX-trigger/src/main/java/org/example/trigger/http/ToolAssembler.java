package org.example.trigger.http;

import org.example.domain.tool.model.entity.ToolCallRecordEntity;
import org.example.domain.tool.model.valobj.ToolDefinition;
import org.example.domain.tool.model.valobj.ToolExecutionResult;
import org.example.domain.tool.model.valobj.ToolMethodDefinition;
import org.example.domain.tool.model.valobj.ToolParameterDefinition;
import org.example.trigger.dto.tool.ToolCallRecordRes;
import org.example.trigger.dto.tool.ToolExecutionRes;
import org.example.trigger.dto.tool.ToolMethodRes;
import org.example.trigger.dto.tool.ToolParameterRes;
import org.example.trigger.dto.tool.ToolRes;

import java.util.List;

/**
 * 工具领域模型与 HTTP DTO 转换器。
 */
public class ToolAssembler {

    public static ToolRes toRes(ToolDefinition definition) {
        ToolRes res = new ToolRes();
        res.setId(definition.id());
        res.setName(definition.name());
        res.setDescription(definition.description());
        res.setMethods(definition.methods().stream()
                .map(ToolAssembler::toMethodRes)
                .toList());
        return res;
    }

    public static ToolExecutionRes toRes(ToolExecutionResult result) {
        ToolExecutionRes res = new ToolExecutionRes();
        res.setToolId(result.toolId());
        res.setMethodName(result.methodName());
        res.setSuccess(result.success());
        res.setResultText(result.resultText());
        res.setErrorMessage(result.errorMessage());
        res.setLatencyMs(result.latencyMs());
        return res;
    }

    public static ToolCallRecordRes toRes(ToolCallRecordEntity entity) {
        ToolCallRecordRes res = new ToolCallRecordRes();
        res.setId(entity.getId());
        res.setToolId(entity.getToolId());
        res.setMethodName(entity.getMethodName());
        res.setSessionId(entity.getSessionId());
        res.setArgumentsJson(entity.getArgumentsJson());
        res.setResultText(entity.getResultText());
        res.setSuccess(Boolean.TRUE.equals(entity.getSuccess()));
        res.setErrorMessage(entity.getErrorMessage());
        res.setLatencyMs(entity.getLatencyMs() == null ? 0L : entity.getLatencyMs());
        res.setCreateTime(entity.getCreatedAt());
        return res;
    }

    private static ToolMethodRes toMethodRes(ToolMethodDefinition definition) {
        ToolMethodRes res = new ToolMethodRes();
        res.setName(definition.name());
        res.setDescription(definition.description());
        res.setParameters(toParameterResList(definition.parameters()));
        return res;
    }

    private static List<ToolParameterRes> toParameterResList(
            List<ToolParameterDefinition> definitions) {
        return definitions.stream().map(definition -> {
            ToolParameterRes res = new ToolParameterRes();
            res.setName(definition.name());
            res.setDescription(definition.description());
            res.setType(definition.type());
            res.setRequired(definition.required());
            return res;
        }).toList();
    }
}
