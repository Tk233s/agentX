package org.example.domain.rag.service;

import org.example.domain.rag.model.valobj.ChunkMatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reciprocal Rank Fusion（RRF）融合器。
 *
 * 只比较两个召回列表中的名次，不要求向量分和关键词分处于同一量纲，
 * 因而适合把余弦相似度与 pg_trgm 相似度合在一起排序。
 */
public final class RrfFusion {

    private static final int RANK_CONSTANT = 60;
    private static final int MAX_CANDIDATES = 50;

    private RrfFusion() {
    }

    public static List<ChunkMatch> fuse(
            List<ChunkMatch> vectorMatches,
            List<ChunkMatch> keywordMatches,
            int topK) {
        int limit = topK <= 0 ? 3 : Math.min(topK, MAX_CANDIDATES);
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        Map<String, String> contentAliases = new HashMap<>();

        collect(vectorMatches, true, candidates, contentAliases);
        collect(keywordMatches, false, candidates, contentAliases);

        return candidates.values().stream()
                .map(candidate -> candidate.toMatch())
                .sorted(Comparator
                        .comparingDouble(ChunkMatch::fusedScore).reversed()
                        .thenComparing(Comparator.comparingDouble(ChunkMatch::vectorScore).reversed())
                        .thenComparing(ChunkMatch::chunkId))
                .limit(limit)
                .toList();
    }

    /**
     * 合并多个知识库的结果时再次按 chunkId / 正文去重，并限制总数。
     */
    public static List<ChunkMatch> distinctAndLimit(List<ChunkMatch> matches, int limit) {
        if (matches == null || matches.isEmpty() || limit <= 0) {
            return List.of();
        }
        Set<String> chunkIds = new HashSet<>();
        Set<String> contents = new HashSet<>();
        List<ChunkMatch> result = new ArrayList<>();
        List<ChunkMatch> sorted = new ArrayList<>(matches);
        sorted.sort(Comparator
                .comparingDouble(ChunkMatch::fusedScore).reversed()
                .thenComparing(Comparator.comparingDouble(ChunkMatch::vectorScore).reversed()));
        for (ChunkMatch match : sorted) {
            String contentKey = contentKey(match.content());
            if (!chunkIds.add(match.chunkId()) || !contents.add(contentKey)) {
                continue;
            }
            result.add(match);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private static void collect(
            List<ChunkMatch> matches,
            boolean vectorSource,
            Map<String, Candidate> candidates,
            Map<String, String> contentAliases) {
        if (matches == null || matches.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>();
        int rank = 0;
        for (ChunkMatch match : matches) {
            if (match == null || match.chunkId() == null) {
                continue;
            }
            String contentKey = contentKey(match.content());
            if (!seen.add(contentKey)) {
                continue;
            }
            rank++;
            String existingKey = candidates.containsKey(match.chunkId())
                    ? match.chunkId()
                    : contentAliases.get(contentKey);
            Candidate candidate = existingKey == null ? null : candidates.get(existingKey);
            if (candidate == null) {
                candidate = new Candidate(match);
                candidates.put(match.chunkId(), candidate);
                contentAliases.put(contentKey, match.chunkId());
            } else {
                candidate.merge(match);
                contentAliases.putIfAbsent(contentKey, existingKey);
            }
            candidate.addSource(vectorSource, rank);
        }
    }

    private static String contentKey(String content) {
        return content == null
                ? ""
                : content.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static final class Candidate {
        private final String knowledgeBaseId;
        private final String chunkId;
        private final String documentId;
        private String documentName;
        private String content;
        private int chunkIndex;
        private Integer pageNo;
        private String sectionTitle;
        private double vectorScore;
        private double keywordScore;
        private double fusedScore;
        private boolean vectorMatched;
        private boolean keywordMatched;

        private Candidate(ChunkMatch match) {
            this.knowledgeBaseId = match.knowledgeBaseId();
            this.chunkId = match.chunkId();
            this.documentId = match.documentId();
            merge(match);
        }

        private void merge(ChunkMatch match) {
            if (match.documentName() != null) {
                documentName = match.documentName();
            }
            if (match.content() != null) {
                content = match.content();
            }
            chunkIndex = Math.max(chunkIndex, match.chunkIndex());
            if (match.pageNo() != null) {
                pageNo = match.pageNo();
            }
            if (match.sectionTitle() != null && !match.sectionTitle().isBlank()) {
                sectionTitle = match.sectionTitle();
            }
            vectorScore = Math.max(vectorScore, match.vectorScore());
            keywordScore = Math.max(keywordScore, match.keywordScore());
        }

        private void addSource(boolean vectorSource, int rank) {
            fusedScore += 1D / (RANK_CONSTANT + rank);
            if (vectorSource) {
                vectorMatched = true;
            } else {
                keywordMatched = true;
            }
        }

        private ChunkMatch toMatch() {
            String matchedBy = vectorMatched && keywordMatched
                    ? "HYBRID"
                    : vectorMatched ? "VECTOR" : "KEYWORD";
            return new ChunkMatch(
                    knowledgeBaseId,
                    chunkId,
                    documentId,
                    documentName,
                    content,
                    chunkIndex,
                    pageNo,
                    sectionTitle,
                    vectorScore,
                    keywordScore,
                    fusedScore,
                    matchedBy);
        }
    }
}
