package org.example.infrastructure.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.domain.rag.adapter.port.EmbeddingPort;
import org.example.domain.rag.model.entity.EmbeddingEntity;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议的向量化客户端。
 *
 * 走的是通用的 /embeddings 接口，因此不止能接 bge-m3，
 * 把 baseUrl 和 model 换成别的兼容服务同样可用。
 *
 * 没有复用 Spring AI 的 embedding 模型，是因为这里只需要一个 POST，
 * 手写反而更透明：请求怎么拼、分批怎么切、返回怎么对齐顺序，全部看得见。
 */
@Component
public class BgeM3EmbeddingClient implements EmbeddingPort {

    private static final Logger log = LoggerFactory.getLogger(BgeM3EmbeddingClient.class);

    /** 单次请求携带的文本条数，服务商对 batch 有上限，取一个保守值。 */
    private static final int BATCH_SIZE = 16;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private static final Gson GSON = new Gson();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    @Override
    public List<float[]> embed(EmbeddingEntity entity) {
        validate(entity);
        List<String> texts = entity.getTexts();
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, texts.size());
            vectors.addAll(requestOnce(entity, texts.subList(start, end)));
        }
        return vectors;
    }

    private void validate(EmbeddingEntity entity) {
        if (entity == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化请求不能为空");
        }
        if (entity.getApiKey() == null || entity.getApiKey().isBlank()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化缺少API密钥");
        }
        if (entity.getModel() == null || entity.getModel().isBlank()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化缺少模型名");
        }
        if (entity.getTexts() == null || entity.getTexts().isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "待向量化的文本不能为空");
        }
    }

    private List<float[]> requestOnce(EmbeddingEntity entity, List<String> batch) {
        String body = GSON.toJson(Map.of(
                "model", entity.getModel(),
                "input", batch));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint(entity.getBaseUrl())))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + entity.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化请求被中断");
        } catch (Exception e) {
            log.warn("向量化请求失败，model={}", entity.getModel(), e);
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "调用向量化服务失败：" + rootMessage(e));
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "向量化服务返回异常状态 " + response.statusCode() + "：" + abbreviate(response.body()));
        }
        return parse(response.body(), batch.size());
    }

    /**
     * 按 index 归位，不依赖服务端返回顺序与请求顺序一致。
     */
    private List<float[]> parse(String responseBody, int expected) {
        JsonObject root;
        try {
            root = JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化服务返回的内容无法解析");
        }

        JsonArray data = root.getAsJsonArray("data");
        if (data == null || data.size() != expected) {
            throw new AppException(
                    ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "向量化服务返回的条数与请求不一致，期望 " + expected + " 条");
        }

        List<float[]> ordered = new ArrayList<>(java.util.Collections.nCopies(expected, null));
        int cursor = 0;
        for (JsonElement element : data) {
            JsonObject item = element.getAsJsonObject();
            int position = item.has("index") ? item.get("index").getAsInt() : cursor;
            if (position < 0 || position >= expected) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化服务返回的序号越界");
            }
            ordered.set(position, toVector(item.getAsJsonArray("embedding")));
            cursor++;
        }
        for (float[] vector : ordered) {
            if (vector == null) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化服务返回的数据不完整");
            }
        }
        return ordered;
    }

    private float[] toVector(JsonArray embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化服务返回了空向量");
        }
        float[] vector = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i).getAsFloat();
        }
        return vector;
    }

    private String endpoint(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "向量化缺少服务地址");
        }
        return normalized + "/embeddings";
    }

    private String rootMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300);
    }
}
