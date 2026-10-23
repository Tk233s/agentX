package org.example.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.apikey.adapter.repository.ApiKeyRepository;
import org.example.domain.apikey.model.entity.ApiKeyEntity;
import org.example.domain.rag.adapter.port.KeywordSearchPort;
import org.example.domain.rag.adapter.repository.KnowledgeChunkRepository;
import org.example.domain.rag.model.entity.KnowledgeBaseEntity;
import org.example.domain.rag.model.entity.KnowledgeDocumentEntity;
import org.example.domain.rag.model.valobj.ChunkMatch;
import org.example.domain.rag.service.IRagDomainService;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 检索质量评估运行器。
 *
 * <p>与单元测试的区别：这里走真实链路——真实 MySQL/PG、真实 embedding 接口、真实分片。
 * 回答的是「检索效果好不好」，而不是「代码逻辑对不对」。</p>
 *
 * <p>默认不执行，避免 CI 和本地 {@code mvn test} 时误触发外部调用。手动运行：</p>
 * <pre>
 * mvn -pl agentX-app test -Dtest=RetrievalEvalRunner -Drag.eval=true
 * </pre>
 * <p>加 {@code -Drag.eval.reset=true} 会先删掉评测知识库重建，用于语料变更后重跑。</p>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RetrievalEvalRunner {

    /**
     * 直接以 java 命令启动（不依赖 maven surefire），便于在 CLI 里反复跑。
     *
     * <pre>java -cp "&lt;test-classes&gt;;&lt;classes&gt;;&lt;deps&gt;" org.example.rag.RetrievalEvalRunner</pre>
     */
    public static void main(String[] args) {
        System.setProperty("rag.eval", "true");
        JUnitCore.runClasses(RetrievalEvalRunner.class)
                .getFailures()
                .forEach(failure -> System.err.println("[评估失败] " + failure));
        // 文档入库线程池是非 daemon 线程，Spring 上下文关不掉它，不显式退出进程会一直挂着
        System.exit(0);
    }

    /** 评测统一用 admin 账号，语料只与这个身份关联。 */
    private static final String USER_ID = "0f2f1a26b89d4b63a6cbcc470a48f4a2";
    private static final String KB_NAME = "[EVAL] 检索评测语料库";
    private static final String EMBEDDING_MODEL = "BAAI/bge-m3";
    private static final int DIMENSION = 1024;
    private static final int TOP_K = 3;

    /**
     * 待评估的配置组。
     *
     * <p>前两组是参照系：纯向量等价 v1.1，1:1 + kw≥0.20 等价 v1.2 现状。
     * 其余组沿两个可调项做扫描——关键词路阈值（0.20→0.80）与关键词路权重（1→0.5），
     * 目的是回答「拒答失效到底该调哪个旋钮」，而不是靠猜。</p>
     *
     * <p>{@code detail} 为 true 的组才会输出分类明细与失败明细，否则报告会膨胀。</p>
     */
    private static final List<EvalConfig> CONFIGS = List.of(
            new EvalConfig("v1.1 纯向量", 1D, 0D, 0.20D, true),
            new EvalConfig("v1.2 现状 1:1 · kw≥0.20", 1D, 1D, 0.20D, true),
            new EvalConfig("1:1 · kw≥0.40", 1D, 1D, 0.40D, false),
            new EvalConfig("1:1 · kw≥0.60", 1D, 1D, 0.60D, false),
            new EvalConfig("1:1 · kw≥0.80", 1D, 1D, 0.80D, false),
            new EvalConfig("1:0.5 · kw≥0.20", 1D, 0.5D, 0.20D, false));

    /**
     * 关键词路候选覆盖面诊断使用的阈值阶梯。
     *
     * 起点不能低于召回阈值 {@link KeywordSearchPort#MIN_KEYWORD_SCORE}：
     * 适配器已按该阈值过滤过一遍，再量更低的档位拿到的仍是同一批候选，结论会失真。
     */
    private static final List<Double> COVERAGE_THRESHOLDS = List.of(0.40D, 0.50D, 0.60D, 0.70D, 0.80D);

    @Autowired
    private IRagDomainService ragDomainService;

    @Autowired
    private KeywordSearchPort keywordSearchPort;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void runEvaluation() throws Exception {
        Assume.assumeTrue(
                "检索评估会调用外部 embedding 接口，需显式开启：-Drag.eval=true",
                Boolean.getBoolean("rag.eval"));

        Path evalDir = locateEvalDir();
        List<GoldenCase> cases = loadGoldenCases(evalDir.resolve("retrieval-golden.jsonl"));
        System.out.printf("[评估] 载入标注 %d 条%n", cases.size());

        String apiKeyId = resolveEmbeddingApiKeyId();
        KnowledgeBaseEntity knowledgeBase = prepareKnowledgeBase(evalDir.resolve("corpus"), apiKeyId);
        int chunkCount = knowledgeChunkRepository.countByKnowledgeBaseId(knowledgeBase.getId());
        System.out.printf("[评估] 知识库 %s，分片 %d 个%n", knowledgeBase.getId(), chunkCount);

        Map<String, List<CaseResult>> resultsByConfig = new LinkedHashMap<>();
        for (EvalConfig config : CONFIGS) {
            List<CaseResult> results = runConfig(knowledgeBase, cases, config);
            resultsByConfig.put(config.name(), results);
            System.out.printf("[评估] %s：Hit@3=%.3f 拒答=%.3f%n", config.name(),
                    summarize(results).hitRate(), summarize(results).rejectRate());
        }

        List<Coverage> coverage = measureKeywordCoverage(knowledgeBase, cases);

        String report = buildReport(cases, chunkCount, resultsByConfig, coverage);
        Path reportPath = evalDir.resolve("report-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".md");
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
        System.out.println(report);
        System.out.println("[评估] 报告已写入 " + reportPath.toAbsolutePath());
    }

    // ---------------------------------------------------------------- 执行

    private List<CaseResult> runConfig(KnowledgeBaseEntity kb, List<GoldenCase> cases, EvalConfig config) {
        List<CaseResult> results = new ArrayList<>();
        for (GoldenCase golden : cases) {
            long startedAt = System.nanoTime();
            List<ChunkMatch> matches;
            try {
                matches = ragDomainService.search(
                        kb.getId(), USER_ID, golden.query(), TOP_K,
                        config.vectorWeight(), config.keywordWeight(), config.keywordThreshold());
            } catch (RuntimeException e) {
                results.add(CaseResult.error(golden, e.getMessage()));
                continue;
            }
            long costMs = (System.nanoTime() - startedAt) / 1_000_000L;
            results.add(evaluate(golden, matches, costMs));
        }
        return results;
    }

    /**
     * 诊断关键词路的候选覆盖面。
     *
     * <p>回答的是「关键词路为什么让拒答失效」：如果它在每条 query 上都能返回候选，
     * 那么「融合结果为空」这个拒答条件就永远不会成立。这里不走向量、不花 embedding 费用，
     * 只统计不同阈值下有多少条 query 仍有候选、平均候选数是多少。</p>
     */
    private List<Coverage> measureKeywordCoverage(KnowledgeBaseEntity kb, List<GoldenCase> cases) {
        // 与检索一致：candidateLimit = min(50, max(topK*3, 10))
        int candidateLimit = Math.min(50, Math.max(TOP_K * 3, 10));
        Map<String, List<Double>> scoresByCase = new LinkedHashMap<>();
        for (GoldenCase golden : cases) {
            List<Double> scores = keywordSearchPort
                    .search(kb.getId(), USER_ID, golden.query(), candidateLimit)
                    .stream()
                    .map(ChunkMatch::keywordScore)
                    .filter(Double::isFinite)
                    .sorted((a, b) -> Double.compare(b, a))
                    .toList();
            scoresByCase.put(golden.id(), scores);
        }

        List<Coverage> coverages = new ArrayList<>();
        for (double threshold : COVERAGE_THRESHOLDS) {
            long answeredWithCandidates = 0;
            long answeredTotal = 0;
            long rejectWithCandidates = 0;
            long rejectTotal = 0;
            long totalCandidates = 0;
            for (GoldenCase golden : cases) {
                long count = scoresByCase.getOrDefault(golden.id(), List.of()).stream()
                        .filter(score -> score >= threshold)
                        .count();
                totalCandidates += count;
                if (golden.reject()) {
                    rejectTotal++;
                    if (count > 0) {
                        rejectWithCandidates++;
                    }
                } else {
                    answeredTotal++;
                    if (count > 0) {
                        answeredWithCandidates++;
                    }
                }
            }
            coverages.add(new Coverage(threshold,
                    answeredWithCandidates, answeredTotal,
                    rejectWithCandidates, rejectTotal,
                    totalCandidates / (double) cases.size()));
        }
        return coverages;
    }

    /**
     * 判定单条 query。
     *
     * <p>命中用「返回分片正文包含期望片段」判断，两侧都去掉全部空白再比，
     * 这样不受 markdown 换行与分片截断位置影响；不依赖 chunkId，文档重传也不会失效。</p>
     */
    private CaseResult evaluate(GoldenCase golden, List<ChunkMatch> matches, long costMs) {
        if (golden.reject()) {
            boolean correctlyRejected = matches.isEmpty();
            return new CaseResult(golden, correctlyRejected, correctlyRejected ? 1D : 0D,
                    0D, matches.size(), costMs, correctlyRejected ? null : "应拒答但召回了 " + matches.size() + " 条");
        }
        if (matches.isEmpty()) {
            return new CaseResult(golden, false, 0D, 0D, 0, costMs, "无结果");
        }
        List<String> normalizedContents = matches.stream()
                .map(match -> normalize(match.content()))
                .toList();
        int hitCount = 0;
        int firstHitRank = -1;
        List<String> missed = new ArrayList<>();
        for (ExpectedSnippet expected : golden.expect()) {
            int rank = -1;
            for (int i = 0; i < normalizedContents.size(); i++) {
                if (normalizedContents.get(i).contains(normalize(expected.snippet()))) {
                    rank = i + 1;
                    break;
                }
            }
            if (rank > 0) {
                hitCount++;
                if (firstHitRank < 0 || rank < firstHitRank) {
                    firstHitRank = rank;
                }
            } else {
                missed.add(expected.file());
            }
        }
        double recall = (double) hitCount / golden.expect().size();
        double reciprocalRank = firstHitRank > 0 ? 1D / firstHitRank : 0D;
        String failure = hitCount > 0 ? null : "未命中，召回来自 " + documentNames(matches);
        return new CaseResult(golden, hitCount > 0, recall, reciprocalRank,
                matches.size(), costMs, failure);
    }

    private String documentNames(List<ChunkMatch> matches) {
        return matches.stream()
                .map(match -> match.documentName() == null ? "?" : match.documentName())
                .distinct()
                .toList()
                .toString();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------ 环境准备

    private Path locateEvalDir() {
        Path cursor = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (cursor != null) {
            Path candidate = cursor.resolve("docs").resolve("eval");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("未找到 docs/eval 目录，当前工作目录："
                + System.getProperty("user.dir"));
    }

    private List<GoldenCase> loadGoldenCases(Path goldenFile) throws Exception {
        List<GoldenCase> cases = new ArrayList<>();
        for (String line : Files.readAllLines(goldenFile, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode node = objectMapper.readTree(line);
            List<ExpectedSnippet> expect = new ArrayList<>();
            for (JsonNode item : node.path("expect")) {
                expect.add(new ExpectedSnippet(item.path("file").asText(), item.path("snippet").asText()));
            }
            cases.add(new GoldenCase(
                    node.path("id").asText(),
                    node.path("category").asText(),
                    node.path("query").asText(),
                    expect,
                    node.path("reject").asBoolean(false)));
        }
        return cases;
    }

    /**
     * 找用于向量化的密钥。
     *
     * <p>DeepSeek 不提供 embedding 接口，因此优先选 baseUrl 指向硅基流动的密钥；
     * 找不到就退回第一条启用的密钥，让调用失败暴露在日志里，而不是静默跳过。</p>
     */
    private String resolveEmbeddingApiKeyId() {
        List<ApiKeyEntity> keys = apiKeyRepository.queryByUserId(USER_ID);
        return keys.stream()
                .filter(key -> Boolean.TRUE.equals(key.getEnabled()))
                .filter(key -> key.getBaseUrl() != null
                        && key.getBaseUrl().toLowerCase(Locale.ROOT).contains("siliconflow"))
                .map(ApiKeyEntity::getId)
                .findFirst()
                .orElseGet(() -> keys.stream()
                        .filter(key -> Boolean.TRUE.equals(key.getEnabled()))
                        .map(ApiKeyEntity::getId)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("没有可用的 API 密钥，无法向量化")));
    }

    private KnowledgeBaseEntity prepareKnowledgeBase(Path corpusDir, String apiKeyId) throws Exception {
        if (Boolean.getBoolean("rag.eval.reset")) {
            existingKnowledgeBase().ifPresent(kb -> {
                System.out.println("[评估] reset=true，删除既有评测知识库 " + kb.getId());
                ragDomainService.deleteKnowledgeBase(kb.getId(), USER_ID);
            });
        }
        KnowledgeBaseEntity kb = existingKnowledgeBase().orElseGet(() -> {
            KnowledgeBaseEntity created = ragDomainService.createKnowledgeBase(
                    KnowledgeBaseEntity.createNew(USER_ID, KB_NAME,
                            "检索质量评估专用，可随时删除重建；不参与业务对话",
                            apiKeyId, EMBEDDING_MODEL, DIMENSION, null, null, null, TOP_K));
            System.out.println("[评估] 新建知识库 " + created.getId());
            return created;
        });

        List<String> existingNames = ragDomainService.listDocuments(kb.getId(), USER_ID).stream()
                .map(KnowledgeDocumentEntity::getFileName)
                .toList();

        List<Path> files;
        try (Stream<Path> stream = Files.list(corpusDir)) {
            files = stream.filter(path -> path.toString().endsWith(".md")).sorted().toList();
        }
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            if (existingNames.contains(fileName)) {
                continue;
            }
            ragDomainService.ingestDocument(kb.getId(), USER_ID, fileName, Files.readAllBytes(file));
            System.out.println("[评估] 已提交入库 " + fileName);
        }

        awaitAllReady(kb.getId());
        return kb;
    }

    private java.util.Optional<KnowledgeBaseEntity> existingKnowledgeBase() {
        return ragDomainService.listKnowledgeBases(USER_ID).stream()
                .filter(kb -> KB_NAME.equals(kb.getName()))
                .findFirst();
    }

    /** 文档入库是异步的，必须等到全部 READY 才能评估，否则会拿不到分片。 */
    private void awaitAllReady(String knowledgeBaseId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 300_000L;
        while (true) {
            List<KnowledgeDocumentEntity> documents = ragDomainService.listDocuments(knowledgeBaseId, USER_ID);
            long processing = documents.stream()
                    .filter(doc -> !KnowledgeDocumentEntity.STATUS_READY.equals(doc.getStatus())
                            && !KnowledgeDocumentEntity.STATUS_FAILED.equals(doc.getStatus()))
                    .count();
            long failed = documents.stream()
                    .filter(doc -> KnowledgeDocumentEntity.STATUS_FAILED.equals(doc.getStatus()))
                    .count();
            if (processing == 0) {
                if (failed > 0) {
                    documents.stream()
                            .filter(doc -> KnowledgeDocumentEntity.STATUS_FAILED.equals(doc.getStatus()))
                            .forEach(doc -> System.out.println(
                                    "[评估] 入库失败 " + doc.getFileName() + "：" + doc.getErrorMessage()));
                }
                System.out.printf("[评估] 文档就绪 %d 份，失败 %d 份%n", documents.size() - failed, failed);
                return;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("等待文档入库超时，仍有 " + processing + " 份在处理中");
            }
            Thread.sleep(1_000L);
        }
    }

    // ---------------------------------------------------------------- 报告

    private String buildReport(List<GoldenCase> cases, int chunkCount,
                               Map<String, List<CaseResult>> resultsByConfig,
                               List<Coverage> coverage) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 检索质量评估报告\n\n");
        sb.append("| 项目 | 值 |\n| --- | --- |\n");
        sb.append("| 生成时间 | ").append(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append(" |\n");
        sb.append("| 语料分片数 | ").append(chunkCount).append(" |\n");
        sb.append("| 标注条数 | ").append(cases.size()).append(" |\n");
        sb.append("| TopK | ").append(TOP_K).append(" |\n");
        sb.append("| 向量模型 | ").append(EMBEDDING_MODEL).append(" |\n\n");

        sb.append("## 总体对照\n\n");
        sb.append("| 配置 | Hit@3 | Recall@3 | MRR | 拒答正确率 | 平均延迟 | 失败数 |\n");
        sb.append("| --- | --- | --- | --- | --- | --- | --- |\n");
        for (Map.Entry<String, List<CaseResult>> entry : resultsByConfig.entrySet()) {
            Summary summary = summarize(entry.getValue());
            sb.append(String.format("| %s | %.3f | %.3f | %.3f | %.3f | %d ms | %d |%n",
                    entry.getKey(), summary.hitRate(), summary.recall(), summary.mrr(),
                    summary.rejectRate(), summary.avgLatencyMs(), summary.failures()));
        }

        sb.append("\n## 关键词路候选覆盖面（诊断）\n\n");
        sb.append("关键词路在多少条 query 上仍能返回候选。候选集已按当前召回阈值"
                + "（`KeywordSearchPort.MIN_KEYWORD_SCORE`）过滤，故阈值阶梯从该值起步。"
                + "**这个数字越接近全量，「融合结果为空」这个拒答条件就越不可能成立。**\n\n");
        sb.append("| 关键词阈值 | 可答题有候选 | 应拒答题有候选 | 平均候选数 |\n");
        sb.append("| --- | --- | --- | --- |\n");
        for (Coverage item : coverage) {
            sb.append(String.format("| %.2f | %d/%d | **%d/%d** | %.1f |%n",
                    item.threshold(), item.answeredWithCandidates(), item.answeredTotal(),
                    item.rejectWithCandidates(), item.rejectTotal(), item.avgCandidates()));
        }

        sb.append("\n## 分类明细\n\n");
        sb.append("| 配置 | 类别 | 条数 | Hit@3 | Recall@3 | MRR |\n| --- | --- | --- | --- | --- | --- |\n");
        for (Map.Entry<String, List<CaseResult>> entry : detailOnly(resultsByConfig).entrySet()) {
            Map<String, List<CaseResult>> byCategory = groupByCategory(entry.getValue());
            for (Map.Entry<String, List<CaseResult>> category : byCategory.entrySet()) {
                Summary summary = summarize(category.getValue());
                sb.append(String.format("| %s | %s | %d | %.3f | %.3f | %.3f |%n",
                        entry.getKey(), category.getKey(), category.getValue().size(),
                        summary.hitRate(), summary.recall(), summary.mrr()));
            }
        }

        sb.append("\n## 失败明细\n\n");
        for (Map.Entry<String, List<CaseResult>> entry : detailOnly(resultsByConfig).entrySet()) {
            List<CaseResult> failed = entry.getValue().stream()
                    .filter(result -> !result.golden().reject() && result.recall() < 1D)
                    .toList();
            sb.append("### ").append(entry.getKey()).append("\n\n");
            if (failed.isEmpty()) {
                sb.append("无失败用例。\n\n");
                continue;
            }
            sb.append("| 编号 | 类别 | 问题 | Recall | 说明 |\n| --- | --- | --- | --- | --- |\n");
            for (CaseResult result : failed) {
                sb.append(String.format("| %s | %s | %s | %.2f | %s |%n",
                        result.golden().id(), result.golden().category(),
                        escape(result.golden().query()), result.recall(),
                        result.failure() == null ? "-" : escape(result.failure())));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** 只有标记为 detail 的配置才展开分类与失败明细，避免扫描组把报告撑爆。 */
    private Map<String, List<CaseResult>> detailOnly(Map<String, List<CaseResult>> resultsByConfig) {
        Set<String> detailNames = CONFIGS.stream()
                .filter(EvalConfig::detail)
                .map(EvalConfig::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return resultsByConfig.entrySet().stream()
                .filter(entry -> detailNames.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Summary summarize(List<CaseResult> results) {
        List<CaseResult> scored = results.stream()
                .filter(result -> !result.golden().reject())
                .toList();
        List<CaseResult> rejectCases = results.stream()
                .filter(result -> result.golden().reject())
                .toList();
        double hitRate = scored.isEmpty() ? 0D
                : scored.stream().filter(CaseResult::hit).count() / (double) scored.size();
        double recall = scored.isEmpty() ? 0D
                : scored.stream().mapToDouble(CaseResult::recall).average().orElse(0D);
        double mrr = scored.isEmpty() ? 0D
                : scored.stream().mapToDouble(CaseResult::reciprocalRank).average().orElse(0D);
        double rejectRate = rejectCases.isEmpty() ? 0D
                : rejectCases.stream().filter(CaseResult::hit).count() / (double) rejectCases.size();
        long avgLatency = results.isEmpty() ? 0L
                : Math.round(results.stream().mapToLong(CaseResult::costMs).average().orElse(0D));
        long failures = scored.stream().filter(result -> result.recall() < 1D).count()
                + rejectCases.stream().filter(result -> !result.hit()).count();
        return new Summary(hitRate, recall, mrr, rejectRate, avgLatency, failures);
    }

    private Map<String, List<CaseResult>> groupByCategory(List<CaseResult> results) {
        Map<String, List<CaseResult>> grouped = new LinkedHashMap<>();
        for (CaseResult result : results) {
            grouped.computeIfAbsent(result.golden().category(), key -> new ArrayList<>()).add(result);
        }
        return grouped;
    }

    private String escape(String text) {
        return text == null ? "" : text.replace("|", "\\|");
    }

    // ---------------------------------------------------------------- 结构

    /** 一组待评估的检索参数。 */
    private record EvalConfig(String name, double vectorWeight, double keywordWeight,
                              double keywordThreshold, boolean detail) {
    }

    /** 关键词路在某个阈值下的候选覆盖面。 */
    private record Coverage(double threshold, long answeredWithCandidates, long answeredTotal,
                            long rejectWithCandidates, long rejectTotal, double avgCandidates) {
    }

    private record ExpectedSnippet(String file, String snippet) {
    }

    private record GoldenCase(String id, String category, String query,
                              List<ExpectedSnippet> expect, boolean reject) {
    }

    private record CaseResult(GoldenCase golden, boolean hit, double recall, double reciprocalRank,
                              int returned, long costMs, String failure) {
        static CaseResult error(GoldenCase golden, String message) {
            return new CaseResult(golden, false, 0D, 0D, 0, 0L, "调用异常：" + message);
        }
    }

    private record Summary(double hitRate, double recall, double mrr, double rejectRate,
                           long avgLatencyMs, long failures) {
    }
}
