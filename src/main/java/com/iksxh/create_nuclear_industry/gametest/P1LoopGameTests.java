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

/**
 * 正式 P1 服务端反应堆循环的 GameTest 回归夹具。
 *
 * <p>测试使用与结构扫描器相同的本地坐标和方块 ID，在真实 GameTest 服务端中验证无效结构拒绝
 * 推进、裂变—冷却—燃耗—持久化顺序，以及 SCRAM 对相邻和非相邻燃料列的边界影响。</p>
 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1LoopGameTests {
    /** 提供空世界的最小模板；正式结构由测试方法按契约坐标显式放置。 */
    private static final String TEMPLATE = "p0_probe_empty";
    /** 仪表端口在 5×5×5 结构中的本地锚点，也是权威快照的读取入口。 */
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    /** 用于单列回归的堆芯坐标，采用固定 3×3 内部坐标系。 */
    private static final CoreColumnPosition TEST_COLUMN = new CoreColumnPosition(0, 0);

    private P1LoopGameTests() {
    }

    /** 无效结构不得推进服务端权威快照，避免未成形反应堆产生热量或燃耗。 */
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

    /** 验证完整结构的一次 tick 会结算裂变、冷却、燃耗、损伤并可经 NBT 恢复。 */
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

    /** 验证 SCRAM 只抑制相邻控制棒列影响范围内的裂变，不会全局清零非相邻燃料。 */
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

    /** 从固定仪表坐标取得方块实体，并把夹具错误转换为 GameTest 失败。 */
    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "expected reactor instrument port block entity");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 使用结构定义的标准模板，避免 GameTest 自己维护第二份结构坐标。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.canonicalTemplate());
    }

    /** 按结构定义的本地坐标逐项放置方块；空方块由模板 ID 显式表示并保持为空气。 */
    private static void buildStructure(
            GameTestHelper helper,
            Map<ReactorStructureDefinition.LocalPosition, String> template
    ) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry : template.entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 将结构契约中的命名空间 ID 映射为已注册方块，未知 ID 必须立即暴露夹具错误。 */
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

    /** 统一使用 GameTest 的失败通道，确保异步回调中的失败不会被普通断言吞掉。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
