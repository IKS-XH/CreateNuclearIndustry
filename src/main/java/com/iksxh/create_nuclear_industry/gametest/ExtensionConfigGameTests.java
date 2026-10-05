package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerPartBlock;
import com.iksxh.create_nuclear_industry.config.BoilerConfig;
import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 隔离世界读取非默认 SERVER 文件后，用真实机器、能力和 Create 管压核对配置接线。 */
@GameTestHolder("create_nuclear_industry_config")
@PrefixGameTestTemplate(false)
public final class ExtensionConfigGameTests {
    private static final BlockPos CENTER = new BlockPos(10, 2, 3);
    private static final BlockPos CONTROL = CENTER.offset(0, 1, -2);
    private static final BlockPos WATER = CENTER.offset(2, 1, 0);
    private static final BlockPos STEAM = CENTER.offset(-2, 3, 0);
    private static final BlockPos SECTION = CENTER.offset(1, 0, 0);
    private static final BlockPos SOURCE = SECTION.below();
    private static final BlockPos PIPE = STEAM.west();

    private ExtensionConfigGameTests() {}

    /** 使用本测试域的空模板，在隔离世界搭建真实锅炉与换热器。 */
    @GameTest(template = "boiler_empty", timeoutTicks = 100)
    public static void nonDefaultServerSettingsReachPhysicalMachine(GameTestHelper helper) {
        var boiler = BoilerConfig.settings();
        var heat = HeatExchangerConfig.settings();
        require(helper, boiler.valid() && heat.valid()
                        && boiler.waterCapacityMb() == 400 && boiler.steamCapacityMb() == 500
                        && boiler.portFlowMbPerTick() == 128 && boiler.sectionHeatHuPerTick() == 36
                        && boiler.steamHuPerMb() == 2 && boiler.warmHuPerSection() == 72
                        && heat.hotCapacityMb() == 2000 && heat.coldCapacityMb() == 3000
                        && heat.maxLineLength() == 8 && heat.rate() == 36 && heat.bufferTicks() == 20,
                "隔离世界未加载非默认锅炉/换热器 SERVER 配置");
        build(helper);
        final boolean[] highRateObserved = {false};
        helper.onEachTick(() -> {
            var machine = source(helper);
            if (machine == null || !machine.current() || !machine.canTick()) return;
            IFluidHandler hot = machine.fluidPort(Direction.EAST);
            IFluidHandler cold = machine.fluidPort(Direction.WEST);
            IFluidHandler water = handler(helper, WATER, Direction.EAST);
            if (hot == null || cold == null || water == null) return;
            hot.fill(new FluidStack(ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 100),
                    IFluidHandler.FluidAction.EXECUTE);
            cold.drain(3000, IFluidHandler.FluidAction.EXECUTE);
            water.fill(new FluidStack(Fluids.WATER, 128), IFluidHandler.FluidAction.EXECUTE);
            highRateObserved[0] |= owner(helper).ledger().produced() > 9;
        });
        helper.runAfterDelay(40, () -> {
            var water = handler(helper, WATER, Direction.EAST);
            var steam = handler(helper, STEAM, Direction.WEST);
            var hot = source(helper).fluidPort(Direction.EAST);
            var cold = source(helper).fluidPort(Direction.WEST);
            require(helper, water != null && steam != null && hot != null && cold != null
                            && water.getTankCapacity(0) == 400 && steam.getTankCapacity(0) == 500
                            && hot.getTankCapacity(0) == 2000 && cold.getTankCapacity(0) == 3000,
                    "配置容量未进入四个真实能力");
            require(helper, highRateObserved[0] && owner(helper).ledger().steam() > 0,
                    "非默认36HU/t供热与2HU/mB产汽没有真实执行");
            var pipe = FluidPropagator.getPipe(helper.getLevel(), helper.absolutePos(PIPE));
            var entry = pipe == null ? null : pipe.getConnection(Direction.EAST);
            require(helper, entry != null && Math.abs(entry.getPressure().getFirst() - 256f) < .01f,
                    "Create 汽管压力未跟随128mB/t物理口配置");
            helper.succeed();
        });
    }

    private static void build(GameTestHelper helper) {
        for (int y = 0; y < 5; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos part = CENTER.offset(x, y, z);
            helper.setBlock(part, y >= 1 && y <= 3 && Math.abs(x) <= 1 && Math.abs(z) <= 1
                    ? Blocks.AIR : BoilerContent.CASING.get());
        }
        helper.setBlock(CONTROL, BoilerContent.CONTROLLER.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.NORTH));
        helper.setBlock(WATER, BoilerContent.WATER_PORT.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.EAST));
        helper.setBlock(STEAM, BoilerContent.STEAM_PORT.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.WEST));
        helper.setBlock(CENTER.above(4), BoilerContent.SAFETY_VALVE.get());
        helper.setBlock(SECTION, BoilerContent.HEAT_SECTION.get());
        helper.setBlock(SOURCE, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                .setValue(NuclearHeatExchangerBlock.FACING, Direction.WEST));
        helper.setBlock(PIPE, AllBlocks.FLUID_PIPE.get());
    }

    private static BoilerControllerBlockEntity owner(GameTestHelper helper) {
        return (BoilerControllerBlockEntity) helper.getBlockEntity(CONTROL);
    }

    private static NuclearHeatExchangerBlockEntity source(GameTestHelper helper) {
        return (NuclearHeatExchangerBlockEntity) helper.getBlockEntity(SOURCE);
    }

    private static IFluidHandler handler(GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), side);
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
