package org.example.domain.conversation.model.valobj;

/**
 * 一次回答引用的知识库片段，只带展示需要的字段。
 *
 * snippet 是截断后的摘要，完整正文只进 Prompt，不回传给前端。
 */
public record KnowledgeCitation(
        String knowledgeBaseId,
        String documentName,
        String snippet,
        double score) {
}
