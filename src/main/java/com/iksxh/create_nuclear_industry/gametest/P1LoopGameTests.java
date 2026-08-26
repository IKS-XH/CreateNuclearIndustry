package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorFissionCalculator;
import com.iksxh.create_nuclear_industry.reactor.ReactorFissionResult;
import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1LoopGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final CoreColumnPosition TEST_COLUMN = new CoreColumnPosition(0, 0);

    private P1LoopGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void unformedReactorDoesNotRun(GameTestHelper helper) {
        helper.setBlock(INSTRUMENT, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorSnapshot before = ReactorSnapshot.singleFuelColumn(
                    TEST_COLUMN,
                    new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)
            );
            instrument.setSnapshot(before);
            require(helper, !instrument.tickReactor(), "an unformed reactor advanced its state");
            require(helper, instrument.snapshot().equals(before),
                    "an unformed reactor changed authoritative state");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void formedReactorRunsFissionCoolingBurnAndReload(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "canonical reactor did not form");
            ReactorSnapshot before = new ReactorSnapshot(
                    Map.of(TEST_COLUMN, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                    Map.of(),
                    128L,
                    0L,
                    0L,
                    false
            );
            instrument.setSnapshot(before);
            require(helper, instrument.tickReactor(), "formed reactor did not advance on the server tick");

            ReactorSnapshot after = instrument.snapshot();
            require(helper, after.fuelColumns().get(TEST_COLUMN).fuelAssembly().damage() > 0,
                    "server tick did not commit fuel burn");
            require(helper, after.hotCoolantMb() > 0L && after.coldCoolantMb() < before.coldCoolantMb(),
                    "server tick did not settle cold-to-hot coolant conversion");
            require(helper, after.fuelColumns().get(TEST_COLUMN).integrity() == 1.0D,
                    "sufficient cooling damaged a fuel column");

            CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
            ReactorInstrumentPortBlockEntity reloaded = new ReactorInstrumentPortBlockEntity(
                    helper.absolutePos(INSTRUMENT),
                    P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
            reloaded.loadForServerTest(saved, helper.getLevel().registryAccess());
            require(helper, reloaded.snapshot().equals(after),
                    "formal tick state did not survive NBT reload");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void scrammedReactorDoesNotGloballyZeroNonAdjacentFuel(GameTestHelper helper) {
        Map<ReactorStructureDefinition.LocalPosition, String> layout =
                new java.util.HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        CoreColumnPosition controlColumn = new CoreColumnPosition(1, 1);
        layout.put(new ReactorStructureDefinition.LocalPosition(2, 4, 2),
                "create_nuclear_industry:control_rod_drive");
        buildStructure(helper, layout);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "control-rod reactor did not form");
            instrument.setSnapshot(new ReactorSnapshot(
                    Map.of(TEST_COLUMN, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                    Map.of(controlColumn, new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D)),
                    0L,
                    0L,
                    0L,
                    false
            ));
            require(helper, instrument.updateRedstoneScram(true).scramActive(),
                    "instrument port did not accept SCRAM for a control-rod structure");
            instrument.tickReactor();
            require(helper, instrument.snapshot().scramActive(), "SCRAM state was not retained by the tick");
            ReactorFissionResult fission = ReactorFissionCalculator.calculate(
                    instrument.snapshot(), ReactorSimulationParameters.defaults());
            require(helper, fission.generatedHeatHu() > 0.0D
                            && fission.plannedFuelBurnUnits() > 0.0D,
                    "SCRAM globally suppressed non-adjacent fuel heat or burn");
            helper.succeed();
        });
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "expected reactor instrument port block entity");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    private static void buildCanonicalStructure(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.canonicalTemplate());
    }

    private static void buildStructure(
            GameTestHelper helper,
            Map<ReactorStructureDefinition.LocalPosition, String> template
    ) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry : template.entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    private static net.minecraft.world.level.block.Block blockForId(String id) {
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
            default -> throw new IllegalArgumentException("unknown reactor block " + id);
        };
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
