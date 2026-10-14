package org.example.trigger.dto.tool;

import lombok.Data;

import java.util.List;

/**
 * 工具定义出参。
 */
@Data
public class ToolRes {

    private String id;
    private String name;
    private String description;
    private List<ToolMethodRes> methods;
}
