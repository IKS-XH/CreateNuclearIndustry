package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorMeltdownEvent;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** 验证危险拆除在八类固定组件上的服务端破坏前提交和边界行为。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Maint01GameTests {
    /** 使用空世界模板；每个测试显式放置需要的固定结构。 */
    private static final String TEMPLATE = "p0_probe_empty";
    /** 标准结构中的仪表端口位置。 */
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    /** 自定义结构中控制棒列位于左上角，其他列使用燃料列。 */
    private static final CoreColumnPosition CONTROL_ROD = new CoreColumnPosition(0, 0);
    /** 自定义结构中用于提供裂变热的燃料列。 */
    private static final CoreColumnPosition SOURCE_FUEL = new CoreColumnPosition(1, 1);
    private static final BlockPos CASING = new BlockPos(0, 0, 0);
    private static final BlockPos WINDOW = new BlockPos(0, 2, 2);
    private static final BlockPos COLD_PORT = new BlockPos(1, 2, 4);
    private static final BlockPos HOT_PORT = new BlockPos(3, 2, 4);
    private static final BlockPos REFUELING_PORT = new BlockPos(2, 4, 2);
    private static final BlockPos FUEL_ROD = new BlockPos(2, 1, 2);
    private static final BlockPos CONTROL_ROD_DRIVE = new BlockPos(1, 4, 1);

    private P1Maint01GameTests() {
    }

    /** 裂变运行时八类组件都在移除前提交完成状态，仪表端口本身也不取消原 BreakEvent。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void fissionRunningCoversAllEightComponents(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO, maintenanceLayout());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            require(helper, instrument.structureValid(), "maintenance structure did not form");
            AtomicReference<BlockPos> currentBroken = new AtomicReference<>();
            List<ReactorMeltdownEvent> events = listenFor(
                    instrument, helper, BlockPos.ZERO, currentBroken::get);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            Map<BlockPos, String> components = Map.of(
                    CASING, "reactor_casing",
                    WINDOW, "reactor_window",
                    INSTRUMENT, "reactor_instrument_port",
                    COLD_PORT, "reactor_cold_port",
                    HOT_PORT, "reactor_hot_port",
                    REFUELING_PORT, "reactor_refueling_port",
                    FUEL_ROD, "reactor_fuel_rod",
                    CONTROL_ROD_DRIVE, "control_rod_drive"
            );
            int beforeEntities = countEntities(helper);
            int expectedEvents = 0;

            for (Map.Entry<BlockPos, String> entry : components.entrySet()) {
                expectedEvents++;
                ReactorPortBlockEntity refueling = firstRefuelingPort(instrument);
                refueling.setFuelAssembly(FuelAssemblyItemCodec.fromLegacyState(
                        FuelAssemblyState.installed(216_000, 0)));
                ReactorSnapshot before = fissionRunningSnapshot();
                instrument.setSnapshot(before);
                BlockPos relative = entry.getKey();
                BlockPos absolute = helper.absolutePos(relative);
                currentBroken.set(absolute);
                Block beforeBlock = helper.getLevel().getBlockState(absolute).getBlock();
                var beforeFuel = refueling.fuelAssembly();
                var prepared = instrument.prepareDisassemblyPlan(absolute,
                        helper.getLevel().getBlockState(absolute));
                require(helper, prepared != null
                                && prepared.action()
                                == com.iksxh.create_nuclear_industry.structure.ReactorDisassemblyPlan.Action.DANGEROUS,
                        "component " + entry.getValue() + " prepared unexpected plan: " + prepared);

                BlockEvent.BreakEvent breakEvent = postBreak(helper, absolute, player, false);
                require(helper, !breakEvent.isCanceled(),
                        "dangerous disassembly unexpectedly canceled the original BreakEvent");
                require(helper, events.size() == expectedEvents,
                        "component " + entry.getValue() + " did not publish exactly once");
                ReactorMeltdownEvent event = events.get(events.size() - 1);
                require(helper, event.reason() == ReactorMeltdownEvent.Reason.DANGEROUS_DISASSEMBLY,
                        "component " + entry.getValue() + " published the wrong reason");
                require(helper, event.instrumentPort().equals(helper.absolutePos(INSTRUMENT)),
                        "component " + entry.getValue() + " published the wrong instrument");
                assertCommittedSnapshot(helper, before, instrument.snapshot(), event.snapshot());
                require(helper, helper.getLevel().getBlockState(absolute).getBlock() == beforeBlock,
                        "component " + entry.getValue() + " changed before the explicit removal");
                require(helper, countEntities(helper) == beforeEntities,
                        "component " + entry.getValue() + " changed the entity set");
                require(helper, sameItemStack(beforeFuel, refueling.fuelAssembly()),
                        "component " + entry.getValue() + " changed the refueling item");
            }
            require(helper, events.size() == components.size(),
                    "not every fixed component published one dangerous disassembly event");
            helper.succeed();
        });
    }

    /** 倒计时运行、SCRAM/有效冷却暂停以及重复回调都只发布一次并保持快照幂等。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void countdownAndDuplicateCallbacksAreIdempotent(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO, maintenanceLayout());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos target = helper.absolutePos(WINDOW);
            List<ReactorMeltdownEvent> events = listenFor(
                    instrument, helper, BlockPos.ZERO, () -> target);

            ReactorSnapshot running = countdownSnapshot(3L, false);
            instrument.setSnapshot(running);
            postBreak(helper, target, player, false);
            require(helper, events.size() == 1,
                    "running countdown did not publish dangerous disassembly");
            ReactorSnapshot committed = instrument.snapshot();
            postBreak(helper, target, player, false);
            require(helper, events.size() == 1,
                    "repeated callback published a duplicate dangerous disassembly");
            require(helper, instrument.snapshot().equals(committed),
                    "repeated callback changed the committed snapshot");

            ReactorSnapshot pausedByScram = countdownSnapshot(4L, true);
            instrument.setSnapshot(pausedByScram);
            postBreak(helper, target, player, false);
            require(helper, events.size() == 2,
                    "SCRAM-paused countdown did not publish dangerous disassembly");
            assertCommittedSnapshot(helper, pausedByScram, instrument.snapshot(), events.get(1).snapshot());

            instrument.setSnapshot(countdownSnapshot(5L, false));
            postBreak(helper, target, player, false);
            require(helper, events.size() == 3,
                    "cooling-paused countdown did not publish dangerous disassembly");
            helper.succeed();
        });
    }

    /** 安全停机、缓存余热、完成状态、取消事件、普通方块和近邻同 ID 均不得发布或改写。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void safeCancelledCompletedAndUnownedPathsAreInert(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO, maintenanceLayout());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, BlockPos.ZERO);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos target = helper.absolutePos(WINDOW);
            List<ReactorMeltdownEvent> events = listenFor(
                    instrument, helper, BlockPos.ZERO, () -> target);

            ReactorSnapshot safe = ReactorSnapshot.empty();
            instrument.setSnapshot(safe);
            postBreak(helper, target, player, false);
            require(helper, events.isEmpty() && instrument.snapshot().equals(safe),
                    "safe shutdown unexpectedly published or changed state");

            ReactorSnapshot cachedHeatOnly = new ReactorSnapshot(
                    Map.of(SOURCE_FUEL, new FuelColumnState(
                            FuelAssemblyState.empty(), 0.75D, 8.0D)),
                    Map.of(), 111L, 222L, 0L, false);
            instrument.setSnapshot(cachedHeatOnly);
            postBreak(helper, target, player, false);
            require(helper, events.isEmpty() && instrument.snapshot().equals(cachedHeatOnly),
                    "cached heat alone unexpectedly triggered dangerous disassembly");

            int countdownTicks = Math.max(1, P1ServerConfig.VALUES.meltdownCountdownTicks.get());
            ReactorSnapshot completed = new ReactorSnapshot(
                    Map.of(SOURCE_FUEL, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                    Map.of(), 0L, 0L, countdownTicks, true,
                    Map.of(), false, true);
            instrument.setSnapshot(completed);
            postBreak(helper, target, player, false);
            require(helper, events.isEmpty() && instrument.snapshot().equals(completed),
                    "already completed meltdown unexpectedly republished or changed state");

            ReactorSnapshot active = fissionRunningSnapshot();
            instrument.setSnapshot(active);
            BlockEvent.BreakEvent canceled = postBreak(helper, target, player, true);
            require(helper, canceled.isCanceled(), "test pre-canceled BreakEvent lost cancellation");
            require(helper, events.isEmpty() && instrument.snapshot().equals(active),
                    "pre-canceled BreakEvent published or changed state");

            instrument.setSnapshot(active);
            BlockPos ordinary = new BlockPos(8, 0, 0);
            helper.setBlock(ordinary, Blocks.STONE.defaultBlockState());
            postBreak(helper, helper.absolutePos(ordinary), player, false);
            require(helper, events.isEmpty() && instrument.snapshot().equals(active),
                    "ordinary block unexpectedly triggered dangerous disassembly");

            instrument.setSnapshot(active);
            BlockPos nearbySameId = new BlockPos(5, 2, 2);
            helper.setBlock(nearbySameId, P1Blocks.REACTOR_WINDOW.get().defaultBlockState());
            postBreak(helper, helper.absolutePos(nearbySameId), player, false);
            require(helper, events.isEmpty() && instrument.snapshot().equals(active),
                    "nearby same-ID block without a valid owner unexpectedly triggered event");
            helper.succeed();
        });
    }

    /** 相邻双堆共享破坏前候选扫描范围时，只提交真正拥有该槽位的仪表端口。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void adjacentReactorsDoNotCrossPublish(GameTestHelper helper) {
        buildStructure(helper, BlockPos.ZERO, maintenanceLayout());
        buildStructure(helper, new BlockPos(5, 0, 0), maintenanceLayout());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity first = instrument(helper, BlockPos.ZERO);
            ReactorInstrumentPortBlockEntity second = instrument(helper, new BlockPos(5, 0, 0));
            BlockPos firstEastWindow = new BlockPos(4, 2, 2);
            List<ReactorMeltdownEvent> firstEvents = listenFor(
                    first, helper, BlockPos.ZERO, () -> helper.absolutePos(firstEastWindow));
            List<ReactorMeltdownEvent> secondEvents = listenFor(
                    second, helper, new BlockPos(5, 0, 0), () -> helper.absolutePos(firstEastWindow));
            firstRefuelingPort(first).setFuelAssembly(FuelAssemblyItemCodec.fromLegacyState(
                    FuelAssemblyState.installed(216_000, 0)));
            firstRefuelingPort(second).setFuelAssembly(FuelAssemblyItemCodec.fromLegacyState(
                    FuelAssemblyState.installed(216_000, 0)));
            first.setSnapshot(fissionRunningSnapshot());
            second.setSnapshot(fissionRunningSnapshot());
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            var prepared = first.prepareDisassemblyPlan(
                    helper.absolutePos(firstEastWindow),
                    helper.getLevel().getBlockState(helper.absolutePos(firstEastWindow)));
            require(helper, prepared != null
                            && prepared.action()
                            == com.iksxh.create_nuclear_industry.structure.ReactorDisassemblyPlan.Action.DANGEROUS,
                    "owning adjacent reactor prepared unexpected plan: " + prepared);

            BlockEvent.BreakEvent breakEvent = postBreak(
                    helper, helper.absolutePos(firstEastWindow), player, false);
            require(helper, !breakEvent.isCanceled(), "adjacent reactor break was canceled");
            require(helper, firstEvents.size() == 1,
                    "the owning adjacent reactor did not publish exactly once");
            require(helper, secondEvents.isEmpty(),
                    "the neighboring reactor published for another structure's component");
            require(helper, second.snapshot().equals(fissionRunningSnapshot()),
                    "the neighboring reactor snapshot was changed");
            helper.succeed();
        });
    }

    /** 创建一个含控制棒列和燃料列的有效固定结构，覆盖八类组件。 */
    private static Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> maintenanceLayout() {
        TreeMap<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout = new TreeMap<>();
        for (int x = 0; x < CoreColumnPosition.GRID_SIZE; x++) {
            for (int z = 0; z < CoreColumnPosition.GRID_SIZE; z++) {
                layout.put(new CoreColumnPosition(x, z), ReactorStructureDefinition.ColumnType.FUEL);
            }
        }
        layout.put(CONTROL_ROD, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        return layout;
    }

    /** 在指定相对原点放置结构契约的完整方块体积。 */
    private static void buildStructure(
            GameTestHelper helper,
            BlockPos origin,
            Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout
    ) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.templateFor(layout).entrySet()) {
            BlockPos position = origin.offset(
                    entry.getKey().x(), entry.getKey().y(), entry.getKey().z());
            helper.setBlock(position, blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 返回指定相对原点的仪表端口方块实体。 */
    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper, BlockPos origin) {
        BlockPos position = origin.offset(INSTRUMENT);
        var entity = helper.getLevel().getBlockEntity(helper.absolutePos(position));
        require(helper, entity instanceof ReactorInstrumentPortBlockEntity,
                "maintenance instrument block entity was not created");
        return (ReactorInstrumentPortBlockEntity) entity;
    }

    /** 监听指定仪表端口的服务端融毁事件，不影响其他测试结构。 */
    private static List<ReactorMeltdownEvent> listenFor(
            ReactorInstrumentPortBlockEntity instrument,
            GameTestHelper helper,
            BlockPos expectedOrigin,
            Supplier<BlockPos> expectedBrokenPos
    ) {
        List<ReactorMeltdownEvent> events = new ArrayList<>();
        NeoForge.EVENT_BUS.addListener(ReactorMeltdownEvent.class, event -> {
            if (event.level() == helper.getLevel()
                    && event.instrumentPort().equals(instrument.getBlockPos())) {
                BlockPos brokenPos = expectedBrokenPos.get();
                if (brokenPos == null) {
                    helper.fail("dangerous disassembly callback did not have a current broken position");
                    return;
                }
                require(helper, event.level() == helper.getLevel(),
                        "dangerous disassembly callback carried the wrong level");
                require(helper, event.dimension().equals(helper.getLevel().dimension()),
                        "dangerous disassembly callback carried the wrong dimension");
                require(helper, event.structureOrigin().equals(helper.absolutePos(expectedOrigin)),
                        "dangerous disassembly callback carried the wrong structure origin");
                require(helper, helper.getLevel().getBlockState(brokenPos).getBlock() != Blocks.AIR,
                        "dangerous disassembly callback observed a removed block");
                require(helper, instrument.snapshot().equals(event.snapshot()),
                        "dangerous disassembly callback observed an uncommitted snapshot");
                events.add(event);
            }
        });
        return events;
    }

    /** 构造有新生裂变热的快照；缓存余热和冷却剂保持为零以隔离触发条件。 */
    private static ReactorSnapshot fissionRunningSnapshot() {
        return new ReactorSnapshot(
                Map.of(SOURCE_FUEL, new FuelColumnState(
                        FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                Map.of(CONTROL_ROD, ControlRodColumnState.fullyInserted()),
                0L, 0L, 0L, false);
    }

    /** 构造没有新生裂变热但已有运行中或暂停中倒计时的快照。 */
    private static ReactorSnapshot countdownSnapshot(long progress, boolean scram) {
        return new ReactorSnapshot(
                Map.of(SOURCE_FUEL, new FuelColumnState(
                        FuelAssemblyState.empty(), 1.0D, 0.0D)),
                Map.of(CONTROL_ROD, ControlRodColumnState.fullyInserted()),
                0L, 0L, progress, true,
                scram ? Map.of(CONTROL_ROD, 1.0D) : Map.of(), scram);
    }

    /** 发布真实 BreakEvent；测试只在取消场景预先设置取消标记，不替代原事件链。 */
    private static BlockEvent.BreakEvent postBreak(
            GameTestHelper helper,
            BlockPos absolutePos,
            Player player,
            boolean canceled
    ) {
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(
                helper.getLevel(), absolutePos, helper.getLevel().getBlockState(absolutePos), player);
        if (canceled) {
            event.setCanceled(true);
        }
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    /** 校验危险提交只改变倒计时三元组和持久化去重标记。 */
    private static void assertCommittedSnapshot(
            GameTestHelper helper,
            ReactorSnapshot before,
            ReactorSnapshot committed,
            ReactorSnapshot eventSnapshot
    ) {
        int countdownTicks = Math.max(1, P1ServerConfig.VALUES.meltdownCountdownTicks.get());
        ReactorSnapshot expected = before.withMeltdown(countdownTicks, true)
                .withMeltdownEventPublished(true);
        require(helper, committed.equals(expected),
                "dangerous disassembly changed state outside the committed meltdown fields");
        require(helper, committed.equals(eventSnapshot),
                "dangerous disassembly event did not observe the submitted snapshot");
        require(helper, committed.coldCoolantMb() == before.coldCoolantMb()
                        && committed.hotCoolantMb() == before.hotCoolantMb(),
                "dangerous disassembly changed the coolant inventories");
    }

    /** 将结构注册 ID 映射为 GameTest 中实际放置的方块。 */
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
            default -> throw new IllegalArgumentException("unknown maintenance structure block " + id);
        };
    }

    /** 返回自定义结构中任意一个已绑定换料端口，供物品不变断言使用。 */
    private static ReactorPortBlockEntity firstRefuelingPort(ReactorInstrumentPortBlockEntity instrument) {
        return instrument.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING).stream()
                .filter(port -> SOURCE_FUEL.equals(port.boundColumn()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("maintenance source fuel port is not bound"));
    }

    /** 比较方块物品而不共享测试侧可变引用。 */
    private static boolean sameItemStack(
            net.minecraft.world.item.ItemStack before,
            net.minecraft.world.item.ItemStack after
    ) {
        return net.minecraft.world.item.ItemStack.matches(before, after);
    }

    /** 统计事件前后的服务端实体集合。 */
    private static int countEntities(GameTestHelper helper) {
        int count = 0;
        for (Object ignored : helper.getLevel().getEntities().getAll()) {
            count++;
        }
        return count;
    }

    /** 游戏测试断言失败时立即终止当前测试。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
