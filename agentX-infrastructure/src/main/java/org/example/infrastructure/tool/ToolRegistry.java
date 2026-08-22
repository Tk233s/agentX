package org.example.infrastructure.tool;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册中心
 *
 * 职责：
 * 1. 在应用启动时扫描所有 @Tool 注解的 Spring Bean
 * 2. 按工具名称 → 工具实例 的映射存储起来
 * 3. 提供按名称或按分组查询工具的能力
 *
 * 这是工具系统的"目录"——你知道有哪些工具可用，按名字就能取到。
 * 后期做 MCP 市场时，动态下载的工具也会注册到这里。
 */
@Component
public class ToolRegistry {

    /**
     * 工具存储：key = 工具名称（小写），value = 工具实例（Spring Bean）
     *
     * 工具名称规范：与 @Tool 注解所在类的用途对应，全小写下划线命名。
     * 如：weather_tool、file_tool、database_tool
     */
    private final Map<String, Object> tools = new HashMap<>();

    /**
     * 启动时把所有工具 Bean 注册进来。
     *
     * 注意：这里的 key 统一用类名转小写（去掉 Tool 后缀），
     * 如 WeatherTool → "weather"、FileTool → "file"。
     *
     * 后期接入 MCP 市场时，下载的 MCP Server 也会动态注册到这里。
     */
    private final WeatherTool weatherTool;
    private final FileTool fileTool;

    public ToolRegistry(WeatherTool weatherTool, FileTool fileTool) {
        this.weatherTool = weatherTool;
        this.fileTool = fileTool;
    }

    @PostConstruct
    public void init() {
        // 注册内置工具
        register("weather", weatherTool);
        register("file", fileTool);

        System.out.println("[DEBUG] ToolRegistry 初始化完成，已注册工具：" + tools.keySet());

        // TODO 后期：从数据库加载用户已安装的 MCP 工具，动态注册
    }

    /**
     * 注册单个工具。
     *
     * @param name 工具名称（唯一标识）
     * @param tool 工具实例（方法上有 @Tool 注解的 Spring Bean）
     */
    public void register(String name, Object tool) {
        tools.put(name.toLowerCase(), tool);
    }

    /**
     * 根据名称获取工具实例。
     *
     * @param name 工具名称
     * @return 工具实例，不存在返回 null
     */
    public Object getTool(String name) {
        return tools.get(name.toLowerCase());
    }

    /**
     * 根据名称列表批量获取工具。
     *
     * @param names 工具名称列表（如 ["weather", "file"]）
     * @return 工具实例列表（不存在的名称自动跳过）
     */
    public List<Object> getTools(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return names.stream()
                .map(this::getTool)
                .filter(tool -> tool != null)
                .toList();
    }

    /**
     * 列出所有已注册的工具名称。
     * 用于管理后台展示"已安装工具列表"。
     */
    public List<String> listToolNames() {
        return tools.keySet().stream().sorted().toList();
    }
}
