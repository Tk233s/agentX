package org.example.infrastructure.rag;

/**
 * 向量数学工具。只依赖 JDK 数组，不引第三方向量库：
 * 分片量在几千条以内时，一次全量点乘比部署一个向量数据库便宜得多。
 */
public final class VectorMath {

    private VectorMath() {
    }

    /**
     * 余弦相似度：cos(a, b) = (a · b) / (|a| * |b|)。
     *
     * 线上检索已交给 pgvector 的 &lt;=&gt;，这里保留一份纯 Java 实现，用于对照原理和边界处理。
     *
     * 契约（与 pgvector 的 vector_cosine_ops 一致）：
     * 返回值范围 [-1, 1]，越大越相近；
     * 维度不一致、入参为 null 或长度为 0 时抛 IllegalArgumentException；
     * 全零向量返回 0，不产生 NaN 或 Infinity。
     */
    public static double cosine(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0) {
            throw new IllegalArgumentException("向量不能为 null 或空");
        }

        if (left.length != right.length) {
            throw new IllegalArgumentException(
                    "向量维度不一致: left.length=" + left.length + ", right.length=" + right.length);
        }

        double dot = 0.0;        // 点乘
        double normLeft = 0.0;   // |left|²
        double normRight = 0.0;  // |right|²

        for (int i = 0; i < left.length; i++) {
            // 先提升到 double 再相乘，避免 float 乘法先截断精度
            double a = left[i];
            double b = right[i];
            dot += a * b;
            normLeft += a * a;
            normRight += b * b;
        }

        if (normLeft == 0.0 || normRight == 0.0) {
            return 0.0;
        }

        double score = dot / (Math.sqrt(normLeft) * Math.sqrt(normRight));
        // 浮点舍入可能让结果略微越界，钳回契约声明的 [-1, 1]
        return Math.max(-1.0, Math.min(1.0, score));
    }
}
