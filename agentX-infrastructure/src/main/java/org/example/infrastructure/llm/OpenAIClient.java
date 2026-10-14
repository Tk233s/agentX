package org.example.infrastructure.llm;

import org.example.domain.conversation.adapter.port.LLMPort;
import org.example.domain.conversation.adapter.port.ContextWindowPort;
import org.example.domain.conversation.model.entity.LLMEntity;
import org.example.domain.conversation.model.entity.LLMResult;
import org.example.domain.conversation.model.entity.LLMStreamChunk;
import org.example.domain.conversation.model.valobj.TokenUsage;
import org.example.domain.message.model.entity.MessageEntity;
import org.example.infrastructure.tool.ToolRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAIClient implements LLMPort {

    private final ToolRegistry toolRegistry;
    private final ContextWindowPort contextWindowPort;

    public OpenAIClient(ToolRegistry toolRegistry, ContextWindowPort contextWindowPort) {
        this.toolRegistry = toolRegistry;
        this.contextWindowPort = contextWindowPort;
    }

    @Override
    public LLMResult call(LLMEntity llmEntity) {
        // 构建基础 ChatClient
        ChatClient baseClient = buildChatClient(llmEntity.getModel(),
                                                  llmEntity.getApiKey(),
                                                  llmEntity.getBaseUrl(),
                                                  llmEntity.getMaxTokens());

        // 组装消息并调用
        //构建提示词
        var promptSpec = baseClient.prompt(buildPrompt(llmEntity.getSystemPrompt(), llmEntity.getMessages()));
        promptSpec = attachTools(promptSpec, llmEntity);

        ChatResponse response = promptSpec.call().chatResponse();

        // 第 4 步：提取结果文本
        String content = extractContent(response);
        return new LLMResult(
                content,
                extractUsage(response),
                extractFinishReason(response));
    }

    @Override
    public Flux<LLMStreamChunk> stream(LLMEntity llmEntity) {
        ChatClient baseClient = buildChatClient(llmEntity.getModel(),
                                                  llmEntity.getApiKey(),
                                                  llmEntity.getBaseUrl(),
                                                  llmEntity.getMaxTokens());

        var promptSpec = baseClient.prompt(buildPrompt(llmEntity.getSystemPrompt(), llmEntity.getMessages()));
        promptSpec = attachTools(promptSpec, llmEntity);

        return promptSpec.stream()
                .chatResponse()
                .map(this::toStreamChunk);
    }

    /**
     * 把 Agent 绑定的工具转换为 Spring AI ToolCallback，并附带会话上下文。
     */
    private ChatClient.ChatClientRequestSpec attachTools(
            ChatClient.ChatClientRequestSpec promptSpec,
            LLMEntity llmEntity) {
        List<ToolCallback> toolCallbacks = toolRegistry.getToolCallbacks(llmEntity.getTools());
        if (toolCallbacks.isEmpty()) {
            return promptSpec;
        }

        promptSpec = promptSpec.toolCallbacks(toolCallbacks);
        Map<String, Object> toolContext = new HashMap<>();
        if (llmEntity.getSessionId() != null && !llmEntity.getSessionId().isBlank()) {
            toolContext.put("sessionId", llmEntity.getSessionId());
        }
        if (llmEntity.getUserId() != null && !llmEntity.getUserId().isBlank()) {
            toolContext.put("userId", llmEntity.getUserId());
        }
        return toolContext.isEmpty() ? promptSpec : promptSpec.toolContext(toolContext);
    }

    /**
     * 构建基础 ChatClient（不含工具，工具在调用时通过 .tools() 注册）。     *
     * Spring AI 1.0.0 的工具注册方式：
     * - 调用时 .tools(Object...) 动态注册（推荐，支持每次调用不同工具）
     * - 而不是构建时 .defaultTools()（旧版本方式，1.0.0 已不推荐）
     */
    private ChatClient buildChatClient(String model,
                                       String apiKey,
                                       String baseUrl,
                                       Integer maxTokens) {

        // Step 1: 构建 OpenAiApi（HTTP 连接层）
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(new SimpleApiKey(apiKey))
                .completionsPath("/chat/completions")
                .build();

        // Step 2: 构建 OpenAiChatOptions（模型调用参数层）
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .maxTokens(maxTokens != null
                        ? maxTokens
                        : contextWindowPort.getContextWindow(model).outputReserveTokens())
                .streamUsage(true)
                .build();

        // Step 3: 组合成 ChatModel
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();

        // Step 4: 构建 ChatClient（不含工具）
        return ChatClient.create(chatModel);
    }

    /**
     * 构建 Prompt（消息列表）。
     */
    private Prompt buildPrompt(String systemPrompt, List<MessageEntity> userMessage) {
        List<Message> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 遍历历史消息，按 role 转成 Spring AI 的消息类型
        for (MessageEntity msg : userMessage) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        return new Prompt(messages);
    }

    private LLMStreamChunk toStreamChunk(ChatResponse response) {
        return new LLMStreamChunk(
                extractContent(response),
                extractUsage(response),
                extractFinishReason(response));
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    private String extractFinishReason(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getMetadata() == null) {
            return null;
        }
        return response.getResult().getMetadata().getFinishReason();
    }

    private TokenUsage extractUsage(ChatResponse response) {
        if (response == null) {
            return null;
        }
        ChatResponseMetadata metadata = response.getMetadata();
        if (metadata == null || metadata.getUsage() == null) {
            return null;
        }
        Usage usage = metadata.getUsage();
        if (usage.getPromptTokens() == null
                && usage.getCompletionTokens() == null
                && usage.getTotalTokens() == null) {
            return null;
        }
        return TokenUsage.provider(
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens());
    }
}
