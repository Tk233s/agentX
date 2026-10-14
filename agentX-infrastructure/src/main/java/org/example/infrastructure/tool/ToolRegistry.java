package org.example.infrastructure.tool;

import com.google.gson.Gson;
import org.example.domain.tool.adapter.port.ToolRuntimePort;
import org.example.domain.tool.adapter.repository.ToolCallRecordRepository;
import org.example.domain.tool.model.entity.ToolCallRecordEntity;
import org.example.domain.tool.model.valobj.ToolDefinition;
import org.example.domain.tool.model.valobj.ToolExecutionResult;
import org.example.domain.tool.model.valobj.ToolInvocationContext;
import org.example.domain.tool.model.valobj.ToolMethodDefinition;
import org.example.domain.tool.model.valobj.ToolParameterDefinition;
import org.example.types.enums.AgentCapabilityProfile;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 工具注册与运行时。
 *
 * 启动时扫描 {@link AgentTool} Bean，按 {@link Tool} 注解生成工具元数据，
 * 同时提供工具展示、Agent 绑定校验、手动执行和模型调用审计能力。
 */
@Component
public class ToolRegistry implements ToolRuntimePort {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);
    private static final int MAX_RESULT_LENGTH = 20_000;
    private static final int MAX_ERROR_LENGTH = 1_000;
    private static final Gson GSON = new Gson();

    private final Map<String, RegisteredTool> tools = new LinkedHashMap<>();
    private final ApplicationContext applicationContext;
    private final ToolCallRecordRepository toolCallRecordRepository;

    public ToolRegistry(
            ApplicationContext applicationContext,
            ToolCallRecordRepository toolCallRecordRepository) {
        this.applicationContext = applicationContext;
        this.toolCallRecordRepository = toolCallRecordRepository;
    }

    @PostConstruct
    public void init() {
        Map<String, Object> toolBeans = applicationContext.getBeansWithAnnotation(AgentTool.class);
        toolBeans.values().stream()
                .sorted(Comparator.comparing(bean -> bean.getClass().getName()))
                .forEach(this::registerByAnnotation);
        log.info("工具注册完成，已注册工具：{}", tools.keySet());
    }

    /**
     * 列出所有已注册工具，供 Agent 编辑器和工具管理页使用。
     */
    @Override
    public List<ToolDefinition> listDefinitions() {
        return tools.values().stream()
                .map(RegisteredTool::definition)
                .toList();
    }

    /**
     * 能力模式到默认工具的映射。
     *
     * 这里只声明"模式自带什么工具"，不涉及 Agent 具体配置；
     * CODE_READONLY 暂时不提供 file，因为当前 file 工具包含写入方法，
     * 等文件工具拆分成只读/写入两组后再接入。
     * RESEARCH 等知识库检索工具落地后再补充 knowledge_search。
     */
    private static List<String> defaultToolIds(AgentCapabilityProfile profile) {
        return switch (profile) {
            case CHAT, RESEARCH, CUSTOM -> List.of();
            case CODE -> List.of("file");
            case CODE_READONLY -> List.of();
        };
    }

    /**
     * 解析生效工具：模式默认工具在前，显式覆盖项在后，去重且忽略空值。
     */
    @Override
    public List<String> resolveToolIds(String capabilityProfile, List<String> explicitToolIds) {
        AgentCapabilityProfile profile = AgentCapabilityProfile.parse(capabilityProfile);
        Set<String> resolved = new LinkedHashSet<>(defaultToolIds(profile));
        resolved.addAll(normalizeToolIds(explicitToolIds));
        return List.copyOf(resolved);
    }

    /**
     * 校验工具 ID；空列表合法，不存在的 ID 会返回明确业务异常。
     */
    @Override
    public void validateBindings(List<String> toolIds) {
        List<String> normalized = normalizeToolIds(toolIds);
        List<String> unknown = normalized.stream()
                .filter(toolId -> !tools.containsKey(toolId))
                .toList();
        if (!unknown.isEmpty()) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "工具不存在：" + String.join("、", unknown));
        }
    }

    /**
     * 手动执行指定工具方法，并把结果写入审计记录。
     */
    @Override
    public ToolExecutionResult execute(
            String toolId,
            String methodName,
            Map<String, Object> arguments,
            ToolInvocationContext context) {
        String normalizedToolId = normalizeToolId(toolId);
        RegisteredTool registeredTool = tools.get(normalizedToolId);
        if (registeredTool == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "工具不存在：" + toolId);
        }

        Method method = registeredTool.methodsByName().get(methodName);
        if (method == null) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "工具方法不存在：" + methodName);
        }

        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        String argumentsJson = serializeArguments(safeArguments);
        long startedAt = System.nanoTime();

        try {
            Object[] methodArguments = bindArguments(method, safeArguments);
            Object result = method.invoke(registeredTool.bean(), methodArguments);
            long latencyMs = elapsedMillis(startedAt);
            String resultText = truncate(result == null ? "" : String.valueOf(result), MAX_RESULT_LENGTH);
            saveRecord(
                    normalizedToolId,
                    method.getName(),
                    context,
                    argumentsJson,
                    resultText,
                    true,
                    null,
                    latencyMs);
            return new ToolExecutionResult(
                    normalizedToolId,
                    method.getName(),
                    true,
                    resultText,
                    null,
                    latencyMs);
        } catch (RuntimeException | ReflectiveOperationException e) {
            long latencyMs = elapsedMillis(startedAt);
            String errorMessage = rootMessage(e);
            saveRecord(
                    normalizedToolId,
                    method.getName(),
                    context,
                    argumentsJson,
                    null,
                    false,
                    truncate(errorMessage, MAX_ERROR_LENGTH),
                    latencyMs);
            return new ToolExecutionResult(
                    normalizedToolId,
                    method.getName(),
                    false,
                    null,
                    errorMessage,
                    latencyMs);
        }
    }

    /**
     * 获取可直接传给 Spring AI ChatClient 的工具回调。
     *
     * 每个回调都会包装一层审计逻辑，模型实际触发工具时也能记录参数、结果和耗时。
     */
    public List<ToolCallback> getToolCallbacks(List<String> toolIds) {
        List<ToolCallback> callbacks = new ArrayList<>();
        for (String toolId : normalizeToolIds(toolIds)) {
            RegisteredTool registeredTool = tools.get(toolId);
            if (registeredTool == null) {
                continue;
            }
            for (ToolCallback callback : registeredTool.callbacks()) {
                callbacks.add(new RecordingToolCallback(toolId, callback));
            }
        }
        return callbacks;
    }

    private void registerByAnnotation(Object bean) {
        AgentTool annotation = bean.getClass().getAnnotation(AgentTool.class);
        if (annotation == null || annotation.value().isBlank()) {
            return;
        }

        String toolId = normalizeToolId(annotation.value());
        List<Method> methods = Arrays.stream(bean.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Tool.class))
                .sorted(Comparator.comparing(Method::getName))
                .toList();
        if (methods.isEmpty()) {
            throw new IllegalStateException(
                    "工具[" + toolId + "]没有声明 @Tool 方法：" + bean.getClass().getName());
        }

        Map<String, Method> methodsByName = new LinkedHashMap<>();
        List<ToolMethodDefinition> methodDefinitions = new ArrayList<>();
        for (Method method : methods) {
            methodsByName.put(method.getName(), method);
            methodDefinitions.add(toMethodDefinition(method));
        }

        String name = annotation.name().isBlank()
                ? displayName(toolId)
                : annotation.name().trim();
        String description = annotation.description().isBlank()
                ? methodDefinitions.stream()
                        .map(ToolMethodDefinition::description)
                        .filter(value -> value != null && !value.isBlank())
                        .reduce((left, right) -> left + "；" + right)
                        .orElse("")
                : annotation.description().trim();
        ToolDefinition definition = new ToolDefinition(
                toolId,
                name,
                description,
                methodDefinitions);
        ToolCallback[] callbacks = ToolCallbacks.from(bean);
        tools.put(toolId, new RegisteredTool(
                definition,
                bean,
                methodsByName,
                List.of(callbacks)));
    }

    private ToolMethodDefinition toMethodDefinition(Method method) {
        Tool tool = method.getAnnotation(Tool.class);
        List<ToolParameterDefinition> parameters = Arrays.stream(method.getParameters())
                .map(this::toParameterDefinition)
                .toList();
        return new ToolMethodDefinition(
                method.getName(),
                tool == null ? "" : tool.description(),
                parameters);
    }

    private ToolParameterDefinition toParameterDefinition(Parameter parameter) {
        ToolParam toolParam = parameter.getAnnotation(ToolParam.class);
        String description = toolParam == null ? "" : toolParam.description();
        boolean required = toolParam == null || toolParam.required();
        return new ToolParameterDefinition(
                parameter.getName(),
                description,
                parameter.getType().getSimpleName(),
                required);
    }

    private Object[] bindArguments(Method method, Map<String, Object> arguments) {
        Parameter[] parameters = method.getParameters();
        Object[] values = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object rawValue = arguments.get(parameter.getName());
            if (rawValue == null && !arguments.containsKey(parameter.getName())) {
                ToolParam toolParam = parameter.getAnnotation(ToolParam.class);
                boolean required = toolParam == null || toolParam.required();
                if (required) {
                    throw new IllegalArgumentException("缺少必填参数：" + parameter.getName());
                }
                values[i] = defaultValue(parameter.getType());
                continue;
            }
            values[i] = convertArgument(rawValue, parameter);
        }
        return values;
    }

    private Object convertArgument(Object value, Parameter parameter) {
        Class<?> targetType = parameter.getType();
        if (value == null) {
            if (targetType.isPrimitive()) {
                throw new IllegalArgumentException("参数不能为空：" + parameter.getName());
            }
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType == Integer.class || targetType == int.class) {
            return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
        }
        if (targetType == Long.class || targetType == long.class) {
            return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
        }
        if (targetType == Double.class || targetType == double.class) {
            return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value));
        }
        if (targetType == Float.class || targetType == float.class) {
            return value instanceof Number number ? number.floatValue() : Float.parseFloat(String.valueOf(value));
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
        }
        return GSON.fromJson(GSON.toJson(value), targetType);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return null;
    }

    private void saveRecord(
            String toolId,
            String methodName,
            ToolInvocationContext context,
            String argumentsJson,
            String resultText,
            boolean success,
            String errorMessage,
            long latencyMs) {
        ToolInvocationContext safeContext = context == null ? ToolInvocationContext.empty() : context;
        try {
            toolCallRecordRepository.save(ToolCallRecordEntity.create(
                    toolId,
                    methodName,
                    safeContext.userId(),
                    safeContext.sessionId(),
                    argumentsJson,
                    resultText,
                    success,
                    errorMessage,
                    latencyMs));
        } catch (RuntimeException e) {
            log.warn("工具调用记录保存失败，tool={}, method={}: {}", toolId, methodName, e.getMessage());
        }
    }

    private List<String> normalizeToolIds(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String toolId : toolIds) {
            if (toolId != null && !toolId.isBlank()) {
                normalized.add(normalizeToolId(toolId));
            }
        }
        return List.copyOf(normalized);
    }

    private String normalizeToolId(String toolId) {
        if (toolId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "工具ID不能为空");
        }
        return toolId.trim().toLowerCase(Locale.ROOT);
    }

    private String displayName(String toolId) {
        String[] parts = toolId.split("_");
        StringBuilder name = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                name.append(part.substring(1));
            }
        }
        return name.isEmpty() ? toolId : name.toString();
    }

    private String serializeArguments(Map<String, Object> arguments) {
        try {
            return GSON.toJson(arguments);
        } catch (RuntimeException e) {
            return "{}";
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        if (current instanceof InvocationTargetException invocationTargetException
                && invocationTargetException.getTargetException() != null) {
            current = invocationTargetException.getTargetException();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private final class RecordingToolCallback implements ToolCallback {

        private final String toolId;
        private final ToolCallback delegate;

        private RecordingToolCallback(String toolId, ToolCallback delegate) {
            this.toolId = toolId;
            this.delegate = delegate;
        }

        @Override
        public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
            return delegate.getToolDefinition();
        }

        @Override
        public ToolMetadata getToolMetadata() {
            return delegate.getToolMetadata();
        }

        @Override
        public String call(String input) {
            long startedAt = System.nanoTime();
            try {
                String result = delegate.call(input);
                saveRecord(
                        toolId,
                        getToolDefinition().name(),
                        ToolInvocationContext.empty(),
                        input,
                        truncate(result, MAX_RESULT_LENGTH),
                        true,
                        null,
                        elapsedMillis(startedAt));
                return result;
            } catch (RuntimeException e) {
                saveRecord(
                        toolId,
                        getToolDefinition().name(),
                        ToolInvocationContext.empty(),
                        input,
                        null,
                        false,
                        truncate(rootMessage(e), MAX_ERROR_LENGTH),
                        elapsedMillis(startedAt));
                throw e;
            }
        }

        @Override
        public String call(String input, ToolContext context) {
            long startedAt = System.nanoTime();
            ToolInvocationContext invocationContext = toInvocationContext(context);
            try {
                String result = delegate.call(input, context);
                saveRecord(
                        toolId,
                        getToolDefinition().name(),
                        invocationContext,
                        input,
                        truncate(result, MAX_RESULT_LENGTH),
                        true,
                        null,
                        elapsedMillis(startedAt));
                return result;
            } catch (RuntimeException e) {
                saveRecord(
                        toolId,
                        getToolDefinition().name(),
                        invocationContext,
                        input,
                        null,
                        false,
                        truncate(rootMessage(e), MAX_ERROR_LENGTH),
                        elapsedMillis(startedAt));
                throw e;
            }
        }

        private ToolInvocationContext toInvocationContext(ToolContext context) {
            if (context == null || context.getContext() == null) {
                return ToolInvocationContext.empty();
            }
            Map<String, Object> values = context.getContext();
            return new ToolInvocationContext(
                    stringValue(values.get("userId")),
                    stringValue(values.get("sessionId")));
        }

        private String stringValue(Object value) {
            return value == null ? null : String.valueOf(value);
        }
    }

    private record RegisteredTool(
            ToolDefinition definition,
            Object bean,
            Map<String, Method> methodsByName,
            List<ToolCallback> callbacks) {
    }
}
