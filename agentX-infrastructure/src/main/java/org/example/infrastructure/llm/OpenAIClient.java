package org.example.infrastructure.llm;

import org.example.domain.llm.model.entity.LLMEntity;
import org.example.domain.message.model.entity.MessageEntity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAIClient implements LLMClient {

    @Override
    public String call(LLMEntity llmEntity) {

        // 第 1 步：根据参数动态构建 ChatClient
        ChatClient client = buildChatClient(llmEntity.getModel(),
                                            llmEntity.getApiKey(),
                                            llmEntity.getBaseUrl(),
                                            llmEntity.getTemperature(),
                                            llmEntity.getMaxTokens());

        // 第 2 步：组装消息并调用
        ChatResponse response = client
                .prompt(buildPrompt(llmEntity.getSystemPrompt(), llmEntity.getMessages()))
                .call()
                .chatResponse();

        // 第 3 步：提取结果文本
        return response.getResults().get(0).getOutput().getText();
    }

    @Override
    public Flux<String> stream(LLMEntity llmEntity) {

        ChatClient client = buildChatClient(llmEntity.getModel(),
                                            llmEntity.getApiKey(),
                                            llmEntity.getBaseUrl(),
                                            llmEntity.getTemperature(),
                                            llmEntity.getMaxTokens());

        return client.prompt(buildPrompt(llmEntity.getSystemPrompt(), llmEntity.getMessages()))
                .stream()
                .content();
    }

    private ChatClient buildChatClient(String model,
                                       String apiKey,
                                       String baseUrl,
                                       Double temperature,
                                       Integer maxTokens) {

        // Step 1: 构建 OpenAiApi（HTTP 连接层）
        //         baseUrl 可以是 OpenAI 官方、DeepSeek、智谱、Moonshot 等任意兼容服务
        //         SimpleApiKey 是 Spring AI 对 API Key 的封装（本质就是包装一个 String）
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(new SimpleApiKey(apiKey))
                .completionsPath("/chat/completions")
                .build();

        // Step 2: 构建 OpenAiChatOptions（模型调用参数层）
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature != null ? temperature : 0.7)
                .maxTokens(maxTokens != null ? maxTokens : 2048)
                .build();

        // Step 3: 组合成 ChatModel
        //         注意：new OpenAiChatModel 的构造需要多个依赖（api, options, retryTemplate, ...）
        //         因此更推荐使用 Builder：
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();

        // Step 4: 用 ChatModel 创建 ChatClient
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
}
