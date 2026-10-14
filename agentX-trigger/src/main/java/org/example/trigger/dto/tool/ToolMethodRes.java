package org.example.trigger.dto.tool;

import lombok.Data;

import java.util.List;

/**
 * 工具方法出参。
 */
@Data
public class ToolMethodRes {

    private String name;
    private String description;
    private List<ToolParameterRes> parameters;
}
