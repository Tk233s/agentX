package org.example;

import org.example.domain.tool.adapter.repository.ToolCallRecordRepository;
import org.example.domain.tool.model.entity.ToolCallRecordEntity;
import org.example.domain.tool.model.valobj.ToolDefinition;
import org.example.infrastructure.tool.FileReadTool;
import org.example.infrastructure.tool.FileWriteTool;
import org.example.infrastructure.tool.ToolRegistry;
import org.example.infrastructure.tool.WeatherTool;
import org.example.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.ai.tool.ToolCallback;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * 工具解析规则测试。
 *
 * 覆盖文件工具拆分后的关键行为：能力模式自带的默认工具、
 * 显式覆盖项的合并规则，以及"只读模式只给读取组"这一条。
 */
public class ToolRegistryTest {

    private ToolRegistry registry;
    private AnnotationConfigApplicationContext context;

    @Before
    public void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(WeatherTool.class, FileReadTool.class, FileWriteTool.class);
        context.refresh();

        registry = new ToolRegistry(context, new InMemoryRecordRepository());
        registry.init();
    }

    /** CODE_READONLY 只挂读取组，写入组不出现。 */
    @Test
    public void codeReadonlyOnlyExposesReadTool() {
        assertEquals(List.of("file-read"), registry.resolveToolIds("CODE_READONLY", List.of(), null));
    }

    /** CODE 同时挂读取组与写入组。 */
    @Test
    public void codeExposesBothFileTools() {
        assertEquals(List.of("file-read", "file-write"), registry.resolveToolIds("CODE", List.of(), null));
    }

    /** CHAT 不挂任何工具。 */
    @Test
    public void chatExposesNothing() {
        assertEquals(List.of(), registry.resolveToolIds("CHAT", List.of(), null));
    }

    /** 显式覆盖项追加在模式默认工具之后。 */
    @Test
    public void explicitToolsAppendAfterDefaults() {
        assertEquals(
                List.of("file-read", "file-write", "weather"),
                registry.resolveToolIds("CODE", List.of("weather"), null));
    }

    /** 空模式值沿用旧行为，按 CUSTOM 处理。 */
    @Test
    public void blankProfileFallsBackToCustom() {
        assertEquals(List.of("weather"), registry.resolveToolIds(" ", List.of("weather"), null));
    }

    /** 只读会话剔除所有声明了副作用的工具，读取组保留。 */
    @Test
    public void readOnlySessionDropsMutatingTools() {
        assertEquals(
                List.of("file-read", "weather"),
                registry.resolveToolIds("CODE", List.of("weather"), "READ_ONLY"));
    }

    /** 只读是收窄，不能反过来被显式覆盖项放宽。 */
    @Test
    public void readOnlySessionCannotEscalate() {
        assertEquals(List.of(), registry.resolveToolIds("CHAT", List.of("file-write"), "READ_ONLY"));
    }

    /** 空权限值沿用旧行为，按放行处理。 */
    @Test
    public void blankPermissionKeepsFullAccess() {
        assertEquals(
                List.of("file-read", "file-write"),
                registry.resolveToolIds("CODE", List.of(), null));
    }

    /** 拆分前的旧工具 ID 已不存在，绑定校验会拦下来。 */
    @Test
    public void legacyFileToolIdIsRejected() {
        assertThrows(AppException.class, () -> registry.validateBindings(List.of("file")));
    }

    /** 运行时同时注册读取工具与写入工具。 */
    @Test
    public void registryContainsBothFileTools() {
        List<String> ids = registry.listDefinitions().stream()
                .map(ToolDefinition::id)
                .sorted()
                .toList();
        assertEquals(List.of("file-read", "file-write", "weather"), ids);
    }

    /**
     * 只读会话即便把带副作用的工具交给运行时，执行点也会拒绝。
     *
     * 这是清单层收窄之外的第二道防线：调用方漏剔除时，写入仍然不会发生。
     */
    @Test
    public void readOnlySessionRejectsMutationAtExecutionPoint() {
        List<ToolCallback> callbacks = registry.getToolCallbacks(List.of("file-write"), "READ_ONLY");
        assertFalse(callbacks.isEmpty());

        for (ToolCallback callback : callbacks) {
            String result = callback.call(
                    "{\"fileName\":\"permission-guard-should-not-exist.txt\",\"content\":\"x\"}");
            assertTrue("应返回只读拒绝说明，实际：" + result, result.contains("只读"));
        }
        assertFalse(
                "只读会话不应产生文件",
                Files.exists(Paths.get("E:/Desktop/permission-guard-should-not-exist.txt")));
    }

    /** 只读收窄的是写入能力，读取类工具在执行点照常放行。 */
    @Test
    public void readOnlySessionStillAllowsReadToolAtExecutionPoint() {
        List<ToolCallback> callbacks = registry.getToolCallbacks(List.of("file-read"), "READ_ONLY");
        assertFalse(callbacks.isEmpty());

        for (ToolCallback callback : callbacks) {
            String result = callback.call("{\"fileName\":\"definitely-missing-file.txt\"}");
            assertFalse("读取工具不应被只读档拦下，实际：" + result, result.contains("只读模式"));
        }
    }

    /** 测试只关心解析规则，调用记录仓储用空实现即可。 */
    private static final class InMemoryRecordRepository implements ToolCallRecordRepository {

        @Override
        public void save(ToolCallRecordEntity record) {
        }

        @Override
        public List<ToolCallRecordEntity> queryBySessionIdAndUserId(
                String sessionId, String userId, int limit) {
            return List.of();
        }
    }
}
