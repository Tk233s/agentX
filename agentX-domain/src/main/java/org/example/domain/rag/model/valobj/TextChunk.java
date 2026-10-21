package org.example.domain.rag.model.valobj;

/**
 * 带来源位置信息的分片。
 *
 * 纯文本没有真实分页信息，因此 pageNo 允许为空；sectionTitle 来自 Markdown 标题，
 * 用于让引用更容易定位到原文位置。
 */
public record TextChunk(
        int chunkIndex,
        String content,
        Integer pageNo,
        String sectionTitle) {
}
