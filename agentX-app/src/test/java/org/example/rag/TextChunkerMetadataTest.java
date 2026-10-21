package org.example.rag;

import org.example.domain.rag.model.valobj.TextChunk;
import org.example.domain.rag.service.TextChunker;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TextChunkerMetadataTest {

    @Test
    public void recordsChunkIndexAndNearestMarkdownSection() {
        String markdown = """
                # 售后政策

                普通文本。

                ## 换新条件

                亮点达到三个可以换新。
                """;

        List<TextChunk> chunks = TextChunker.chunkWithMetadata(markdown, 18, 2);

        assertEquals(0, chunks.get(0).chunkIndex());
        assertEquals("售后政策", chunks.get(0).sectionTitle());
        assertNull(chunks.get(0).pageNo());
        assertEquals(chunks.size() - 1, chunks.get(chunks.size() - 1).chunkIndex());
        assertEquals("换新条件", chunks.get(chunks.size() - 1).sectionTitle());
    }

    @Test
    public void plainTextDoesNotInventPageNumbers() {
        List<TextChunk> chunks = TextChunker.chunkWithMetadata("没有分页的纯文本", 5, 0);

        assertEquals(null, chunks.get(0).pageNo());
    }
}
