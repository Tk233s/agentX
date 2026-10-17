package org.example.rag;

import org.example.domain.rag.model.valobj.EmbeddingCodec;
import org.example.types.exception.AppException;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * 向量文本编解码测试。
 *
 * 落库用的是 JSON 数组文本，往返精度必须无损，否则检索分数会被悄悄改变。
 */
public class EmbeddingCodecTest {

    @Test
    public void roundTripsWithoutLosingPrecision() {
        float[] vector = {0.125f, -0.5f, 1.0f, 0f, 3.1415927f, Float.MIN_VALUE, Float.MAX_VALUE};

        float[] decoded = EmbeddingCodec.decode(EmbeddingCodec.encode(vector));

        assertArrayEquals(vector, decoded, 0f);
    }

    @Test
    public void encodesAsJsonArrayText() {
        String text = EmbeddingCodec.encode(new float[]{0.5f, -0.25f});

        assertEquals("[0.5,-0.25]", text);
    }

    @Test
    public void rejectsEmptyVector() {
        assertAppException(() -> EmbeddingCodec.encode(new float[0]));
        assertAppException(() -> EmbeddingCodec.encode(null));
    }

    @Test
    public void rejectsMalformedText() {
        assertAppException(() -> EmbeddingCodec.decode(null));
        assertAppException(() -> EmbeddingCodec.decode("  "));
        assertAppException(() -> EmbeddingCodec.decode("[]"));
        assertAppException(() -> EmbeddingCodec.decode("[0.1,abc]"));
    }

    private void assertAppException(Runnable action) {
        try {
            action.run();
            fail("Expected AppException");
        } catch (AppException expected) {
            // 预期路径
        }
    }
}
