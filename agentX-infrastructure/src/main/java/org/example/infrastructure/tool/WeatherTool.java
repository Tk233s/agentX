package org.example.infrastructure.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 天气查询 Tool —— Function Calling 演示
 *
 * 注意：这是一个模拟实现，实际项目中应该调用真实天气 API（如 OpenWeatherMap、和风天气等）。
 * 它的目的是演示 Spring AI 的 Function Calling 机制完整流程。
 *
 * Tool 的工作原理：
 * 1. 这个方法被注册到 ChatClient 的 defaultTools() 中
 * 2. Spring AI 会自动读取 @Tool 注解，生成 JSON Schema 发给 LLM
 * 3. LLM 看到这个工具定义后，如果需要查天气，会在响应中返回 tool_use 指令
 * 4. Spring AI 检测到 tool_use，自动调用这个方法，拿到返回值
 * 5. 返回值再发回 LLM，LLM 基于结果生成最终回复
 */
@Component
public class WeatherTool {

    /**
     * 模拟的天气数据库。
     * 实际项目中替换为真实 HTTP API 调用即可。
     */
    private static final Map<String, String> MOCK_WEATHER = new HashMap<>();

    static {
        MOCK_WEATHER.put("北京", "晴，气温 25°C，湿度 45%，东南风 2级");
        MOCK_WEATHER.put("上海", "多云，气温 28°C，湿度 60%，东风 3级");
        MOCK_WEATHER.put("广州", "雷阵雨，气温 30°C，湿度 80%，南风 2级");
        MOCK_WEATHER.put("深圳", "阴，气温 27°C，湿度 70%，东南风 2级");
        MOCK_WEATHER.put("杭州", "小雨，气温 23°C，湿度 75%，东北风 2级");
        MOCK_WEATHER.put("成都", "多云，气温 22°C，湿度 55%，静风");
    }

    /**
     * 查询指定城市的天气。
     *
     * @Tool 注解告诉 Spring AI：
     * - description：告诉 LLM 这个工具是做什么的，LLM 据此决定何时调用它
     *
     * @ToolParam 注解描述每个参数的含义，帮助 LLM 正确传参。
     *
     * @param city 城市名称，如 "北京"、"上海"
     * @return 天气描述文本
     */
    @org.springframework.ai.tool.annotation.Tool(description = "查询指定城市的实时天气信息，包括天气状况、气温、湿度和风力")
    public String getWeather(
            @org.springframework.ai.tool.annotation.ToolParam(description = "要查询天气的城市名称，如 北京、上海、广州") String city) {

        // 模拟 API 调用延迟
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 查询模拟数据
        String weather = MOCK_WEATHER.get(city);
        if (weather == null) {
            // 未知城市返回一个通用描述（实际项目中应调用真实 API）
            return city + "：暂无该城市天气数据，建议检查城市名称是否正确";
        }

        return city + "今天天气：" + weather;
    }
}
