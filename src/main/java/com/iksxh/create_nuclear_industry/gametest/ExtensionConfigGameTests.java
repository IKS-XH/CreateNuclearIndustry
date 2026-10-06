package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.config.BoilerConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;

/** 非默认 SERVER 配置通过实际配置对象进入几何与能力；改值和恢复均在同一服务端 tick。 */
@GameTestHolder("create_nuclear_industry_config")
@PrefixGameTestTemplate(false)
public final class ExtensionConfigGameTests {
    private ExtensionConfigGameTests() {}
    @GameTest(template = "boiler_empty", timeoutTicks = 100)
    public static void nonDefaultServerSettingsReachPhysicalMachine(GameTestHelper helper) {
        ExtensionBoilerGameTests.build(helper, 6, 6, 8, 2);
        helper.runAfterDelay(3, () -> {
            var range = BoilerConfig.DIMENSION_RANGE.get(); int wc = BoilerConfig.WATER_CAPACITY_PER_CELL_MB.get();
            int sc = BoilerConfig.STEAM_CAPACITY_PER_CELL_MB.get(), flow = BoilerConfig.PORT_FLOW_MB_PER_TICK.get();
            try {
                BoilerConfig.DIMENSION_RANGE.set(List.of(6, 12)); BoilerConfig.WATER_CAPACITY_PER_CELL_MB.set(500);
                BoilerConfig.STEAM_CAPACITY_PER_CELL_MB.set(600); BoilerConfig.PORT_FLOW_MB_PER_TICK.set(128);
                var owner = ExtensionBoilerGameTests.owner(helper); owner.invalidateForm(); owner.tick();
                var water = ExtensionBoilerGameTests.water(helper); var steam = ExtensionBoilerGameTests.steam(helper);
                helper.assertTrue(owner.currentForm() != null && water != null && steam != null, "非默认范围拒绝合法偶数长方体");
                helper.assertTrue(water.getTankCapacity(0) == 12000 && steam.getTankCapacity(0) == 28800, "按分区格数的非默认容量未进入能力");
                helper.assertTrue(water.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 256),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 128, "非默认每口限流未生效");
                BoilerConfig.DIMENSION_RANGE.set(List.of(12, 6)); owner.invalidateForm();
                helper.assertTrue(owner.currentForm() == null, "反向范围未拒绝运行");
            } finally {
                BoilerConfig.DIMENSION_RANGE.set(range); BoilerConfig.WATER_CAPACITY_PER_CELL_MB.set(wc);
                BoilerConfig.STEAM_CAPACITY_PER_CELL_MB.set(sc); BoilerConfig.PORT_FLOW_MB_PER_TICK.set(flow);
            }
            helper.succeed();
        });
    }
}
