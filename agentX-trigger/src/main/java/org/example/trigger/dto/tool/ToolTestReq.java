package org.example.trigger.dto.tool;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 工具方法调试请求。
 */
@Data
public class ToolTestReq {

    @NotBlank(message = "工具ID不能为空")
    private String toolId;

    @NotBlank(message = "工具方法不能为空")
    private String methodName;

    private Map<String, Object> arguments;

    private String sessionId;
}
