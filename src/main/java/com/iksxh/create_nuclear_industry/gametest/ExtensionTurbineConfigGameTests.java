package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.config.TurbineConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 独立非默认 SERVER TOML 场景；精确检查读入值及其实际机械输出。 */
@GameTestHolder("create_nuclear_industry_turbine_config")
@PrefixGameTestTemplate(false)
public final class ExtensionTurbineConfigGameTests {
    private static final BlockPos FRONT = new BlockPos(5, 5, 1);
    private ExtensionTurbineConfigGameTests() {}

    @GameTest(template = "turbine_empty", timeoutTicks = 75)
    public static void nonDefaultSettingsDriveRealMachine(GameTestHelper helper) {
        var settings = TurbineConfig.settings();
        ExtensionTurbineGameTests.require(helper,
                settings.shortTier().rotorCount() == 4 && settings.shortTier().ratedFlowMbPerTick() == 60
                        && settings.shortTier().diameter() == 5
                        && settings.shortTier().maxEfficiencyMultiplier() == 1.3
                        && settings.mediumTier().rotorCount() == 7
                        && settings.mediumTier().maxEfficiencyMultiplier() == 1.6
                        && settings.longTier().rotorCount() == 9
                        && settings.longTier().ratedFlowMbPerTick() == 220
                        && settings.longTier().maxEfficiencyMultiplier() == 1.9
                        && settings.rpm() == 96 && settings.suPerMbPerTick() == 16384
                        && settings.smoothingTicks() == 20 && settings.inletPortFlowMbPerTick() == 300
                        && settings.exhaustPortFlowMbPerTick() == 192 && settings.turnoverTicks() == 2
                        && settings.minEfficiencyMultiplier() == .4
                        && settings.minimumOperatingFlowRatio() == .25
                        && settings.frontShare() == .25,
                "非默认 SERVER TOML 未进入实际 Settings：" + settings);
        ExtensionTurbineGameTests.build(helper, FRONT, 4, false);
        BlockPos inlet = ExtensionTurbineGameTests.inlet(FRONT, settings.shortTier());
        BlockPos exhaust = ExtensionTurbineGameTests.exhaust(FRONT, settings.shortTier());
        helper.onEachTick(() -> {
            IFluidHandler port = ExtensionTurbineGameTests.handler(helper, inlet, Direction.WEST);
            if (port != null) port.fill(new FluidStack(BoilerContent.SUPERCRITICAL_STEAM.get(), 300),
                    IFluidHandler.FluidAction.EXECUTE);
            IFluidHandler outlet = ExtensionTurbineGameTests.handler(helper, exhaust, Direction.EAST);
            if (outlet != null) outlet.drain(300, IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(35, () -> {
            var owner = ExtensionTurbineGameTests.owner(helper, FRONT);
            var rearPos = ExtensionTurbineGameTests.part(FRONT, 0, 0, 5);
            var front = ExtensionTurbineGameTests.shaft(helper, FRONT);
            var rear = ExtensionTurbineGameTests.shaft(helper, rearPos);
            ExtensionTurbineGameTests.require(helper, owner.currentForm() != null && front != null && rear != null
                            && owner.ledger().exhaustCapacity() == 120
                            && owner.currentForm().diameter() == 5
                            && owner.ledger().averageFlowMbPerTick() == 60 && front.getGeneratedSpeed() == 96,
                    "非默认档位长度/容量/流量/RPM 未进入真实设备");
            ExtensionTurbineGameTests.require(helper,
                    Math.abs(owner.ledger().totalSu() - 60D * 16384 * 1.3) < 2
                            && Math.abs(front.getOrCreateNetwork().calculateCapacity() - 60f * 16384 * 1.3f) < 2
                            && front.network.equals(rear.network),
                    "非默认窗口/效率未进入两端共享的 Create 网络");
            helper.succeed();
        });
    }
}
