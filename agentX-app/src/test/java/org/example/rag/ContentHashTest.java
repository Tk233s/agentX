package org.example.rag;

import org.example.domain.rag.model.valobj.ContentHash;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ContentHashTest {

    @Test
    public void producesStableSha256ForFileDeduplication() {
        String first = ContentHash.sha256("同一份文档".getBytes(StandardCharsets.UTF_8));
        String second = ContentHash.sha256("同一份文档".getBytes(StandardCharsets.UTF_8));
        String other = ContentHash.sha256("另一份文档".getBytes(StandardCharsets.UTF_8));

        assertEquals(64, first.length());
        assertEquals(first, second);
        assertNotEquals(first, other);
    }
}
