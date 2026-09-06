package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorServerTick;
import com.iksxh.create_nuclear_industry.reactor.ReactorSimulationParameters;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshotNbtCodec;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.TreeMap;

/** 验证融毁覆盖源、20% 边界和倒计时 NBT 连续性能够经过正式服务端路径。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Meltdown01GameTests {
    /** 使用空世界模板；测试方法显式放置固定 5×5×5 结构。 */
    private static final String TEMPLATE = "p0_probe_empty";
    /** 固定结构中仪表端口的本地坐标，也是正式 tick 的权威状态入口。 */
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    /** 作为失效源的固定燃料列坐标。 */
    private static final CoreColumnPosition SOURCE = new CoreColumnPosition(0, 0);
    /** 与失效源相邻、用于验证第二个覆盖成员的燃料列坐标。 */
    private static final CoreColumnPosition TARGET = new CoreColumnPosition(0, 1);

    private P1Meltdown01GameTests() {
    }

    /** 完整度归零但组件未耗尽的孤立燃料列必须在正式 tick 中建立倒计时。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void isolatedFailedFuelStartsFormalCountdown(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "canonical reactor did not form");
            instrument.setSnapshot(ReactorSnapshot.singleFuelColumn(
                    SOURCE,
                    new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 0.0D, 0.0D)
            ));

            require(helper, instrument.tickReactor(), "formal tick did not commit isolated failed source");
            ReactorSnapshot after = instrument.snapshot();
            require(helper, after.meltdownCountdownStarted(),
                    "isolated failed source did not establish persisted meltdown countdown");
            require(helper, after.meltdownProgressTicks() == 1L,
                    "isolated failed source did not advance countdown by one tick");
            helper.succeed();
        });
    }

    /** 在正式服务端编排器中验证八根有效燃料列的 1/8 与 2/8 覆盖边界。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void eightFuelColumnsUseSourceAndTargetForCoverageBoundary(GameTestHelper helper) {
        Map<CoreColumnPosition, FuelColumnState> fuels = eightFuelColumns();
        fuels.put(SOURCE, new FuelColumnState(
                FuelAssemblyState.installed(216_000, 0), 0.0D, 4.0D));
        ReactorServerTick.Result result = ReactorServerTick.advance(
                new ReactorSnapshot(fuels, Map.of(), 0L, 0L, 0L, false),
                ReactorSimulationParameters.defaults(),
                ReactorServerTick.CoolantInput.none()
        );

        require(helper, result.meltdown().propagationCoverageFraction() == 0.25D,
                "source plus one target did not produce the expected 2/8 coverage");
        require(helper, result.meltdown().dangerThresholdReached()
                        && result.snapshot().meltdownCountdownStarted()
                        && result.snapshot().meltdownProgressTicks() == 1L,
                "source plus one target did not trigger the default 20% boundary for eight columns");
        helper.succeed();
    }

    /** 重载必须保留倒计时进度和启动标记，避免区块/存档生命周期回退事故进度。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void meltdownProgressSurvivesNbtReload(GameTestHelper helper) {
        ReactorSnapshot before = new ReactorSnapshot(
                Map.of(SOURCE, new FuelColumnState(FuelAssemblyState.empty(), 0.0D, 0.0D)),
                Map.of(),
                0L,
                0L,
                37L,
                true
        );

        helper.runAfterDelay(1, () -> {
            ReactorSnapshot restored = ReactorSnapshotNbtCodec.decode(
                    ReactorSnapshotNbtCodec.encode(before));
            require(helper, restored.meltdownCountdownStarted(),
                    "NBT reload cleared the persisted meltdown start marker");
            require(helper, restored.meltdownProgressTicks() == 37L,
                    "NBT reload changed the persisted meltdown progress");
            helper.succeed();
        });
    }

    /** 放置与结构扫描器相同的固定 5×5×5 结构，避免 GameTest 使用另一套坐标合同。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            BlockPos position = new BlockPos(
                    entry.getKey().x(), entry.getKey().y(), entry.getKey().z());
            helper.setBlock(position, blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 构造除一个非相邻列外的八根有效燃料列，使失效源仅能覆盖一个相邻目标。 */
    private static Map<CoreColumnPosition, FuelColumnState> eightFuelColumns() {
        Map<CoreColumnPosition, FuelColumnState> fuels = new TreeMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                if (x == 1 && z == 0) {
                    continue;
                }
                fuels.put(new CoreColumnPosition(x, z),
                        new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D));
            }
        }
        return fuels;
    }

    /** 将结构合同中的注册 ID 映射为 GameTest 中实际放置的方块。 */
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
            default -> throw new IllegalArgumentException("unknown canonical structure block " + id);
        };
    }

    /** 取得并校验 GameTest 中的仪表端口实体。 */
    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var entity = helper.getBlockEntity(INSTRUMENT);
        require(helper, entity instanceof ReactorInstrumentPortBlockEntity,
                "instrument port block entity was not created");
        return (ReactorInstrumentPortBlockEntity) entity;
    }

    /** 失败时立即终止当前 GameTest，保留确定性错误信息。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
