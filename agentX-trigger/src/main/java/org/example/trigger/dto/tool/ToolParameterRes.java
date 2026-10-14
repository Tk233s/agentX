package org.example.trigger.dto.tool;

import lombok.Data;

/**
 * 工具参数出参。
 */
@Data
public class ToolParameterRes {

    private String name;
    private String description;
    private String type;
    private boolean required;
}
