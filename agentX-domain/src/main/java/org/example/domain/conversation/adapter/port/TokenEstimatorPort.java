package org.example.domain.conversation.adapter.port;

/**
 * Token 估算端口，服务商未返回 usage 时作为兜底。
 */
public interface TokenEstimatorPort {

    int estimate(String text);
}
