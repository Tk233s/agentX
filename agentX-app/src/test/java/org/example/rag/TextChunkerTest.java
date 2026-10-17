package org.example.rag;

import org.example.domain.rag.service.TextChunker;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 文档分片规则测试。
 *
 * 分片是 RAG 的入口，切错了后面检索再准也没用，所以边界情况都要钉住。
 */
public class TextChunkerTest {

    @Test
    public void splitsLongTextIntoOverlappingChunks() {
        String text = "第一句话。".repeat(60);

        List<String> chunks = TextChunker.chunk(text, 100, 20);

        assertTrue("长文本应切出多片", chunks.size() > 1);
        for (String chunk : chunks) {
            assertTrue("单片不应超过配置长度", chunk.length() <= 100);
        }
    }

    @Test
    public void prefersSentenceBoundaryWhenRetreating() {
        String text = "甲".repeat(60) + "。" + "乙".repeat(60);

        List<String> chunks = TextChunker.chunk(text, 80, 0);

        // 80 字处回退到最近的句号，第一片应以句号收尾而不是被劈成两半
        assertTrue("第一片应该在句号处收尾", chunks.get(0).endsWith("。"));
    }

    @Test
    public void doesNotLoopForeverWhenOverlapIsNotSmallerThanChunkSize() {
        String text = "甲".repeat(500);

        // 重叠长度配错时也必须能结束，不能死循环
        List<String> chunks = TextChunker.chunk(text, 50, 50);

        assertTrue(chunks.size() > 0);
        assertTrue("分片数不应无限膨胀", chunks.size() < 100);
    }

    @Test
    public void returnsEmptyListForBlankText() {
        assertEquals(List.of(), TextChunker.chunk(null, 100, 10));
        assertEquals(List.of(), TextChunker.chunk("", 100, 10));
        assertEquals(List.of(), TextChunker.chunk("   \n\n  ", 100, 10));
        assertEquals(List.of(), TextChunker.chunk("有内容", 0, 0));
    }

    @Test
    public void normalizesLineBreaksAndCollapsesBlankLines() {
        List<String> chunks = TextChunker.chunk("第一行\r\n\r\n\r\n第二行", 100, 0);

        assertEquals(1, chunks.size());
        assertEquals("第一行\n第二行", chunks.get(0));
    }
}
