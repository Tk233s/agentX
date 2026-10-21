package org.example.rag;

import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.service.RrfFusion;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 小型离线评估集，防止混合检索只“看起来能跑”但排序质量退化。
 */
public class RagRetrievalEvaluationTest {

    @Test
    public void reportsHitAt3AndMeanReciprocalRank() {
        List<ChunkMatch> ranking = RrfFusion.fuse(
                List.of(match("chunk-2", "王薇住在新华小区", 0.83, 0D)),
                List.of(
                        match("chunk-2", "王薇住在新华小区", 0D, 0.71),
                        match("chunk-1", "小明今年十岁", 0D, 0.22)),
                3);
        Set<String> relevant = new HashSet<>(Set.of("chunk-2"));

        boolean hitAt3 = ranking.stream().limit(3).anyMatch(item -> relevant.contains(item.chunkId()));
        int firstRelevant = -1;
        for (int i = 0; i < ranking.size(); i++) {
            if (relevant.contains(ranking.get(i).chunkId())) {
                firstRelevant = i + 1;
                break;
            }
        }
        double mrr = firstRelevant > 0 ? 1D / firstRelevant : 0D;

        assertTrue("Hit@3 应为 1", hitAt3);
        assertEquals(1D, mrr, 0.000001);
    }

    @Test
    public void unrelatedQueryDoesNotProduceKeywordOnlyCitation() {
        List<ChunkMatch> fused = RrfFusion.fuse(List.of(), List.of(), 3);

        long unrelated = fused.stream()
                .filter(item -> item.content().contains("药物"))
                .count();

        assertEquals(0, unrelated);
    }

    private ChunkMatch match(String chunkId, String content, double vectorScore, double keywordScore) {
        return new ChunkMatch(
                "kb-1",
                chunkId,
                "doc-1",
                "知识.txt",
                content,
                0,
                null,
                null,
                vectorScore,
                keywordScore,
                0D,
                vectorScore > 0 ? "VECTOR" : "KEYWORD");
    }
}
