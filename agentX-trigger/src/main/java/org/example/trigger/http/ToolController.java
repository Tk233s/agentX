package org.example.trigger.http;

import org.example.api.response.Response;
import org.example.domain.tool.adapter.port.ToolRuntimePort;
import org.example.domain.tool.model.entity.ToolCallRecordEntity;
import org.example.domain.tool.model.valobj.ToolDefinition;
import org.example.domain.tool.model.valobj.ToolExecutionResult;
import org.example.domain.tool.model.valobj.ToolInvocationContext;
import org.example.domain.tool.service.IToolCallRecordService;
import org.example.trigger.dto.tool.ToolCallRecordRes;
import org.example.trigger.dto.tool.ToolExecutionRes;
import org.example.trigger.dto.tool.ToolRes;
import org.example.trigger.dto.tool.ToolTestReq;
import org.example.types.context.UserContext;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 工具管理接口。
 */
@RestController
@RequestMapping("/tool")
public class ToolController {

    @Resource
    private ToolRuntimePort toolRuntimePort;

    @Resource
    private IToolCallRecordService toolCallRecordService;

    /**
     * 列出当前可用工具及其方法。
     */
    @GetMapping("/list")
    public Response<List<ToolRes>> listTools() {
        List<ToolRes> tools = toolRuntimePort.listDefinitions().stream()
                .map(ToolAssembler::toRes)
                .toList();
        return Response.success(tools);
    }

    /**
     * 手动执行指定工具方法，用于管理后台调试。
     */
    @PostMapping("/test")
    public Response<ToolExecutionRes> testTool(
            @RequestBody @Validated ToolTestReq req) {
        ToolExecutionResult result = toolRuntimePort.execute(
                req.getToolId(),
                req.getMethodName(),
                req.getArguments(),
                new ToolInvocationContext(
                        UserContext.requireCurrentUserId(),
                        req.getSessionId()));
        return Response.success(ToolAssembler.toRes(result));
    }

    /**
     * 查看某个会话的工具调用记录。
     */
    @GetMapping("/calls")
    public Response<List<ToolCallRecordRes>> listToolCalls(
            @RequestParam("sessionId") String sessionId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        List<ToolCallRecordEntity> records = toolCallRecordService.listCallRecords(
                sessionId,
                UserContext.requireCurrentUserId(),
                limit);
        return Response.success(records.stream()
                .map(ToolAssembler::toRes)
                .toList());
    }
}
