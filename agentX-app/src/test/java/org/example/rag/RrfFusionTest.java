package org.example.rag;

import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.service.RrfFusion;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RrfFusionTest {

    @Test
    public void fusionRewardsChunksMatchedByBothChannels() {
        ChunkMatch vectorFirst = match("chunk-1", "向量第一", 0.91, 0.10);
        ChunkMatch vectorSecond = match("chunk-2", "双路命中", 0.88, 0.42);
        ChunkMatch keywordFirst = match("chunk-2", "双路命中", 0D, 0.42);

        List<ChunkMatch> fused = RrfFusion.fuse(
                List.of(vectorFirst, vectorSecond),
                List.of(keywordFirst),
                2);

        assertEquals("chunk-2", fused.get(0).chunkId());
        assertEquals("HYBRID", fused.get(0).matchedBy());
        assertTrue(fused.get(0).fusedScore() > fused.get(1).fusedScore());
    }

    @Test
    public void keepsKeywordOnlyCandidateWhenVectorThresholdRejectedIt() {
        ChunkMatch keywordOnly = match("chunk-keyword", "唯一关键词命中", 0D, 0.73);

        List<ChunkMatch> fused = RrfFusion.fuse(List.of(), List.of(keywordOnly), 3);

        assertEquals(1, fused.size());
        assertEquals("KEYWORD", fused.get(0).matchedBy());
        assertEquals(0D, fused.get(0).vectorScore(), 0.000001);
    }

    @Test
    public void deduplicatesSameContentAcrossRetrievalChannels() {
        ChunkMatch vector = match("chunk-1", "同一段正文", 0.77, 0D);
        ChunkMatch keyword = match("chunk-2", "同一段正文", 0D, 0.66);

        List<ChunkMatch> fused = RrfFusion.fuse(List.of(vector), List.of(keyword), 5);

        assertEquals(1, fused.size());
        assertEquals("HYBRID", fused.get(0).matchedBy());
    }

    @Test
    public void appliesFusedTopKAfterBothChannels() {
        List<ChunkMatch> vector = List.of(
                match("chunk-1", "甲", 0.90, 0D),
                match("chunk-2", "乙", 0.80, 0D));
        List<ChunkMatch> keyword = List.of(
                match("chunk-2", "乙", 0D, 0.60),
                match("chunk-3", "丙", 0D, 0.50));

        assertEquals(2, RrfFusion.fuse(vector, keyword, 2).size());
    }

    private ChunkMatch match(String chunkId, String content, double vectorScore, double keywordScore) {
        return new ChunkMatch(
                "kb-1",
                chunkId,
                "doc-1",
                "测试文档.txt",
                content,
                0,
                null,
                "测试章节",
                vectorScore,
                keywordScore,
                0D,
                vectorScore > 0 ? "VECTOR" : "KEYWORD");
    }
}
