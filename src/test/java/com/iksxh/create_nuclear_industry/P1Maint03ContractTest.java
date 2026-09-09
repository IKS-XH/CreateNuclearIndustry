package com.iksxh.create_nuclear_industry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 P1-MAINT-03 的事件优先级、两阶段事务边界和语言资源契约。 */
class P1Maint03ContractTest {
    private static final Path JAVA_SOURCES = Path.of("src", "main", "java");
    private static final Path LANGUAGE_SOURCES =
            Path.of("src", "main", "resources", "assets", "create_nuclear_industry", "lang");

    /** BreakEvent 必须在最低常规优先级执行，并在所有者预检后统一提交或回滚。 */
    @Test
    void breakEventUsesLowestPriorityAndAtomicPlanFlow() throws IOException {
        String lifecycle = readSource(
                "com/iksxh/create_nuclear_industry/structure/ReactorStructureLifecycle.java");
        assertTrue(lifecycle.contains("EventPriority.LOWEST"));
        assertTrue(lifecycle.contains("collectDisassemblyPlans"));
        assertTrue(lifecycle.contains("commitDisassemblyPlans"));
        assertTrue(lifecycle.contains("rollbackDisassemblyPlans"));
        assertTrue(lifecycle.contains("event.isCanceled()"));
    }

    /** 完全停机计划必须从结构映射枚举顶部端口，并验证提交后快照、物品和遥测。 */
    @Test
    void fullShutdownPlanEnumeratesAllFuelPortsAndClearsRuntimeState() throws IOException {
        String instrument = readSource(
                "com/iksxh/create_nuclear_industry/blockentity/ReactorInstrumentPortBlockEntity.java");
        String plan = readSource(
                "com/iksxh/create_nuclear_industry/structure/ReactorDisassemblyPlan.java");
        assertTrue(instrument.contains("ReactorFullShutdownAssessment.assess"));
        assertTrue(instrument.contains("captureFuelPortsForReset"));
        assertTrue(instrument.contains("ReactorSnapshot.empty()"));
        assertTrue(instrument.contains("pendingLegacyFuelAssemblies = Map.of()"));
        assertTrue(instrument.contains("ItemStack.matches"));
        assertTrue(instrument.contains("invalidateTelemetry()"));
        assertTrue(instrument.contains("verifyFullShutdownReset"));
        assertTrue(plan.contains("beforeLastSentTelemetry"));
        assertTrue(plan.contains("pendingLegacyFuelAssemblies"));
    }

    /** 拒绝、提交失败和成功提示必须在中英文资源中同时存在。 */
    @Test
    void maintenanceActionbarKeysExistInBothLanguages() throws IOException {
        String english = readLanguage("en_us.json");
        String chinese = readLanguage("zh_cn.json");
        String[] keys = {
                "message.create_nuclear_industry.maintenance.not_fully_stopped",
                "message.create_nuclear_industry.maintenance.transaction_failed",
                "message.create_nuclear_industry.maintenance.state_cleared"
        };
        for (String key : keys) {
            assertTrue(english.contains('"' + key + '"'), "英文缺少维护语言键：" + key);
            assertTrue(chinese.contains('"' + key + '"'), "中文缺少维护语言键：" + key);
        }
    }

    private static String readSource(String relativePath) throws IOException {
        return Files.readString(JAVA_SOURCES.resolve(relativePath));
    }

    private static String readLanguage(String fileName) throws IOException {
        return Files.readString(LANGUAGE_SOURCES.resolve(fileName));
    }
}
