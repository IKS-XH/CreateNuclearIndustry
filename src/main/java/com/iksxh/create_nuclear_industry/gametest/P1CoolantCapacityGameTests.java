package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.TreeMap;

/** 使用真实世界方块验证不同合法堆芯布局的共享冷却剂容量差异。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1CoolantCapacityGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos COLD = new BlockPos(1, 2, 4);
    private static final BlockPos HOT = new BlockPos(3, 2, 4);

    private P1CoolantCapacityGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void realLayoutsDeriveThreeAndNineThousandSharedCapacity(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.defaultColumnLayout());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "canonical structure did not form");
            long perBlock = P1ServerConfig.VALUES.coolantCapacityPerEmptyBlockMb.get().longValue();
            assertCapacity(helper, instrument, 1, 3, perBlock * 3L);
            assertBothCapabilities(helper, instrument, perBlock * 3L);

            Map<ReactorStructureDefinition.LocalPosition, String> fcf =
                    ReactorStructureDefinition.templateFor(fcfLayout());
            for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry : fcf.entrySet()) {
                helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                        blockForId(entry.getValue()).defaultBlockState());
            }
            helper.runAfterDelay(5, () -> {
                require(helper, instrument.structureValid(), "F-C-F structure did not form");
                assertCapacity(helper, instrument, 0, 9, perBlock * 9L);
                assertBothCapabilities(helper, instrument, perBlock * 9L);

                buildStructure(helper, allFuelLayout());
                helper.runAfterDelay(5, () -> {
                    require(helper, instrument.structureValid(), "all-fuel structure became invalid");
                    assertCapacity(helper, instrument, 0, 0, 0L);
                    IFluidHandler cold = fluidHandler(helper, COLD);
                    require(helper, cold.fill(new FluidStack(
                                    ModFluids.COMPOUND_COOLANT_SOURCE.get(), 1_000),
                            IFluidHandler.FluidAction.SIMULATE) == 0,
                            "zero-capacity valid layout accepted coolant");
                    helper.succeed();
                });
            });
        });
    }

    private static void assertCapacity(
            GameTestHelper helper,
            ReactorInstrumentPortBlockEntity instrument,
            int emptyColumns,
            int spaceBlocks,
            long capacity
    ) {
        var summary = instrument.structureSummary();
        require(helper, summary.valid(), "derived capacity summary was unavailable");
        require(helper, summary.emptyColumnCount() == emptyColumns
                        && summary.coolantSpaceBlockCount() == spaceBlocks
                        && summary.coolantCapacityMb() == capacity,
                "unexpected derived capacity: " + summary);
    }

    private static void assertBothCapabilities(
            GameTestHelper helper,
            ReactorInstrumentPortBlockEntity instrument,
            long capacity
    ) {
        require(helper, instrument.coolantCapacityMb() == capacity,
                "instrument capacity disagrees with summary");
        require(helper, fluidHandler(helper, COLD).getTankCapacity(0) == capacity,
                "cold capability capacity disagrees with structure-derived capacity");
        require(helper, fluidHandler(helper, HOT).getTankCapacity(0) == capacity,
                "hot capability capacity disagrees with structure-derived capacity");
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "expected instrument port block entity, got " + blockEntity);
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    private static IFluidHandler fluidHandler(GameTestHelper helper, BlockPos pos) {
        IFluidHandler handler = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(pos),
                Direction.UP);
        require(helper, handler != null, "fluid capability missing at " + pos);
        return handler;
    }

    private static void buildStructure(
            GameTestHelper helper,
            Map<com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition,
                    ReactorStructureDefinition.ColumnType> layout
    ) {
        Map<ReactorStructureDefinition.LocalPosition, String> blocks =
                ReactorStructureDefinition.templateFor(layout);
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry : blocks.entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    private static Map<com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition,
            ReactorStructureDefinition.ColumnType> fcfLayout() {
        Map<com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition,
                ReactorStructureDefinition.ColumnType> layout = new TreeMap<>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                layout.put(new com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition(x, z),
                        x == 1 ? ReactorStructureDefinition.ColumnType.CONTROL_ROD
                                : ReactorStructureDefinition.ColumnType.FUEL);
            }
        }
        return layout;
    }

    private static Map<com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition,
            ReactorStructureDefinition.ColumnType> allFuelLayout() {
        Map<com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition,
                ReactorStructureDefinition.ColumnType> layout = new TreeMap<>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                layout.put(new com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition(x, z),
                        ReactorStructureDefinition.ColumnType.FUEL);
            }
        }
        return layout;
    }

    private static Block blockForId(String id) {
        return switch (id) {
            case "minecraft:air" -> Blocks.AIR;
            case "create_nuclear_industry:reactor_casing" -> P1Blocks.REACTOR_CASING.get();
            case "create_nuclear_industry:reactor_window" -> P1Blocks.REACTOR_WINDOW.get();
            case "create_nuclear_industry:reactor_instrument_port" -> P1Blocks.REACTOR_INSTRUMENT_PORT.get();
            case "create_nuclear_industry:reactor_cold_port" -> P1Blocks.REACTOR_COLD_PORT.get();
            case "create_nuclear_industry:reactor_hot_port" -> P1Blocks.REACTOR_HOT_PORT.get();
            case "create_nuclear_industry:reactor_refueling_port" -> P1Blocks.REACTOR_REFUELING_PORT.get();
            case "create_nuclear_industry:reactor_fuel_rod" -> P1Blocks.REACTOR_FUEL_ROD.get();
            case "create_nuclear_industry:control_rod_drive" -> P1Blocks.CONTROL_ROD_DRIVE.get();
            default -> throw new IllegalArgumentException("unknown structure block " + id);
        };
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
