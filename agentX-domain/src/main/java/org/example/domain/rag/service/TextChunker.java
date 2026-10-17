package org.example.domain.rag.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档分片。
 *
 * 思路是滑动窗口加边界回退：先按 chunkSize 取一段，再往前找一个自然断点收尾，
 * 避免把一个句子劈成两半；下一段回退 overlap 个字符，让切开的语义在相邻分片里都有上下文。
 */
public final class TextChunker {

    /** 收尾时优先选择的断点，越靠前优先级越高。 */
    private static final String STRONG_BOUNDARIES = "。！？!?";
    private static final String WEAK_BOUNDARIES = "\n；;，,";

    /** 回退找断点时最多往回看的字符数，避免为找一个句号而丢掉太多内容。 */
    private static final int LOOKBACK_LIMIT = 120;

    private TextChunker() {
    }

    public static List<String> chunk(String text, int chunkSize, int overlap) {
        String normalized = normalize(text);
        if (normalized.isEmpty() || chunkSize <= 0) {
            return List.of();
        }

        // 重叠长度最多取分片长度的一半：否则每次只前进一两个字符，
        // 一篇几百字的文档会被切出成百上千个分片，向量化成本成倍放大。
        int safeOverlap = Math.min(Math.max(overlap, 0), chunkSize / 2);

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = normalized.length();
        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            if (end < length) {
                end = retreatToBoundary(normalized, start, end);
            }
            String piece = normalized.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= length) {
                break;
            }
            // 至少前进一个字符，重叠长度配错时也不会死循环
            start = Math.max(end - safeOverlap, start + 1);
        }
        return chunks;
    }

    /**
     * 从候选结束位置往前找一个更自然的断点，找不到就用原位置。
     */
    private static int retreatToBoundary(String text, int start, int end) {
        int floor = Math.max(start + 1, end - LOOKBACK_LIMIT);
        for (int i = end - 1; i >= floor; i--) {
            if (STRONG_BOUNDARIES.indexOf(text.charAt(i)) >= 0) {
                return i + 1;
            }
        }
        for (int i = end - 1; i >= floor; i--) {
            if (WEAK_BOUNDARIES.indexOf(text.charAt(i)) >= 0) {
                return i + 1;
            }
        }
        return end;
    }

    /**
     * 统一换行并压掉连续空行，避免分片里塞满无意义的空白。
     */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String unified = text.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder builder = new StringBuilder(unified.length());
        boolean lastWasNewline = false;
        for (int i = 0; i < unified.length(); i++) {
            char current = unified.charAt(i);
            if (current == '\n') {
                if (lastWasNewline) {
                    continue;
                }
                lastWasNewline = true;
            } else {
                lastWasNewline = false;
            }
            builder.append(current);
        }
        return builder.toString().trim();
    }
}
