package org.example.rag;

import org.example.infrastructure.rag.VectorMath;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * 余弦相似度测试。
 * 覆盖返回值范围、零向量的退化处理，以及两类非法入参。
 */
public class VectorMathTest {

    private static final double DELTA = 1e-6;

    @Test
    public void identicalDirectionScoresOne() {
        assertEquals(1.0, VectorMath.cosine(
                new float[]{1, 2, 3},
                new float[]{2, 4, 6}), DELTA);
    }

    @Test
    public void orthogonalVectorsScoreZero() {
        assertEquals(0.0, VectorMath.cosine(
                new float[]{1, 0},
                new float[]{0, 1}), DELTA);
    }

    @Test
    public void oppositeDirectionScoresMinusOne() {
        assertEquals(-1.0, VectorMath.cosine(
                new float[]{1, 1},
                new float[]{-1, -1}), DELTA);
    }

    @Test
    public void zeroVectorScoresZeroInsteadOfNaN() {
        assertEquals(0.0, VectorMath.cosine(
                new float[]{0, 0},
                new float[]{1, 1}), DELTA);
    }

    @Test
    public void dimensionMismatchIsRejected() {
        assertIllegalArgument(() -> VectorMath.cosine(new float[]{1, 2}, new float[]{1}));
    }

    @Test
    public void nullOrEmptyIsRejected() {
        assertIllegalArgument(() -> VectorMath.cosine(null, new float[]{1}));
        assertIllegalArgument(() -> VectorMath.cosine(new float[]{1}, null));
        assertIllegalArgument(() -> VectorMath.cosine(new float[0], new float[0]));
    }

    private void assertIllegalArgument(Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // 预期路径
        }
    }
}
