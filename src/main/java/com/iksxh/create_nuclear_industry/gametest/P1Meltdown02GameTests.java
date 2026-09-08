package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorMeltdownEvent;
import com.iksxh.create_nuclear_industry.reactor.ReactorMeltdownEvents;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshotNbtCodec;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 验证正式服务端融毁完成事件的唯一发布、幂等持久化和零世界副作用。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Meltdown02GameTests {
    /** 使用空世界模板；测试方法显式放置固定 5×5×5 结构。 */
    private static final String TEMPLATE = "p0_probe_empty";
    /** 标准结构中的仪表端口，也是服务端反应堆权威 tick 入口。 */
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    /** 作为融毁源的固定燃料列。 */
    private static final CoreColumnPosition SOURCE = new CoreColumnPosition(0, 0);

    private P1Meltdown02GameTests() {
    }

    /** 倒计时跨入 COMPLETE 只发布一次，重复 tick 与方块实体 NBT 重载均不重发。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void formalTickPublishesCompletionOnceAcrossReload(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "canonical reactor did not form");
            List<ReactorMeltdownEvent> events = listenFor(instrument, helper);

            int countdownTicks = Math.max(1, P1ServerConfig.VALUES.meltdownCountdownTicks.get());
            instrument.setSnapshot(new ReactorSnapshot(
                    Map.of(SOURCE, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, 0), 0.0D, 0.0D)),
                    Map.of(),
                    0L,
                    0L,
                    Math.max(0L, countdownTicks - 1L),
                    true
            ));

            require(helper, instrument.tickReactor(), "formal tick did not commit completion");
            require(helper, events.size() == 1,
                    "countdown completion did not publish exactly one event");
            ReactorMeltdownEvent event = events.get(0);
            require(helper, event.reason() == ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE,
                    "completion event carried the wrong reason");
            require(helper, event.level() == helper.getLevel(),
                    "completion event did not carry the server level");
            require(helper, event.dimension().equals(helper.getLevel().dimension()),
                    "completion event carried the wrong dimension");
            require(helper, event.structureOrigin().equals(helper.absolutePos(BlockPos.ZERO)),
                    "completion event carried the wrong structure origin");
            require(helper, event.instrumentPort().equals(helper.absolutePos(INSTRUMENT)),
                    "completion event carried the wrong instrument position");
            require(helper, event.snapshot().meltdownProgressTicks() == countdownTicks,
                    "completion event carried a pre-completion snapshot");
            require(helper, event.snapshot().meltdownEventPublished(),
                    "completion event snapshot did not carry the persisted dedupe marker");
            require(helper, instrument.snapshot().equals(event.snapshot()),
                    "event snapshot was not the submitted authoritative snapshot");

            CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
            require(helper, ReactorSnapshotNbtCodec.decode(
                            saved.getCompound("ReactorSnapshot")).meltdownEventPublished(),
                    "completed snapshot did not persist the event dedupe marker");

            instrument.tickReactor();
            require(helper, events.size() == 1,
                    "repeated completed tick published a duplicate event");

            instrument.loadForServerTest(saved, helper.getLevel().registryAccess());
            require(helper, instrument.snapshot().meltdownEventPublished(),
                    "completed snapshot reload lost the event dedupe marker");
            instrument.tickReactor();
            require(helper, events.size() == 1,
                    "reloaded completed snapshot published a duplicate event");
            helper.succeed();
        });
    }

    /** 正式 tick 尚未进入 COMPLETE 时不得投递占位事件。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void formalTickBeforeCompletionDoesNotPublish(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            List<ReactorMeltdownEvent> events = listenFor(instrument, helper);
            instrument.setSnapshot(new ReactorSnapshot(
                    Map.of(SOURCE, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, 0), 0.9D, 0.0D)),
                    Map.of(),
                    0L,
                    0L,
                    0L,
                    true
            ));

            instrument.tickReactor();
            require(helper, events.isEmpty(),
                    "formal tick before COMPLETE unexpectedly published a meltdown event");
            require(helper, instrument.snapshot().meltdownProgressTicks() == 0L,
                    "formal tick before danger unexpectedly advanced meltdown progress");
            helper.succeed();
        });
    }

    /** 直接调用唯一发布入口时，事件前后不改变世界、实体、燃料和冷/热库存。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void placeholderEventHasNoWorldSideEffects(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            List<ReactorMeltdownEvent> events = listenFor(instrument, helper);
            ReactorSnapshot beforeSnapshot = new ReactorSnapshot(
                    Map.of(SOURCE, new FuelColumnState(
                            FuelAssemblyState.installed(216_000, 100), 0.0D, 8.0D)),
                    Map.of(),
                    321L,
                    654L,
                    20L,
                    true,
                    Map.of(),
                    false,
                    true
            );
            instrument.setSnapshot(beforeSnapshot);

            BlockPos origin = helper.absolutePos(BlockPos.ZERO);
            BlockPos instrumentPos = helper.absolutePos(INSTRUMENT);
            Block beforeOrigin = helper.getLevel().getBlockState(origin).getBlock();
            Block beforeInstrument = helper.getLevel().getBlockState(instrumentPos).getBlock();
            int beforeEntityCount = countEntities(helper);
            ReactorPortBlockEntity refuelingPort = instrument.boundPorts(
                            ReactorPortBlockEntity.BindingType.REFUELING).stream()
                    .filter(port -> SOURCE.equals(port.boundColumn()))
                    .findFirst()
                    .orElseThrow();
            ItemStack beforeFuel = refuelingPort.fuelAssembly();

            require(helper, ReactorMeltdownEvents.publish(
                            ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE,
                            helper.getLevel(),
                            origin,
                            instrumentPos,
                            beforeSnapshot),
                    "server placeholder event was not published");
            require(helper, events.size() == 1, "placeholder event was not observed");
            require(helper, helper.getLevel().getBlockState(origin).getBlock() == beforeOrigin,
                    "placeholder event changed a reactor world block");
            require(helper, helper.getLevel().getBlockState(instrumentPos).getBlock() == beforeInstrument,
                    "placeholder event changed the instrument block");
            require(helper, countEntities(helper) == beforeEntityCount,
                    "placeholder event changed the world entity set");
            require(helper, instrument.snapshot().equals(beforeSnapshot),
                    "placeholder event changed the authoritative snapshot");
            require(helper, instrument.snapshot().coldCoolantMb() == 321L
                            && instrument.snapshot().hotCoolantMb() == 654L,
                    "placeholder event changed the coolant inventories");
            require(helper, ItemStack.matches(beforeFuel, refuelingPort.fuelAssembly()),
                    "placeholder event changed the refueling port fuel item");
            helper.succeed();
        });
    }

    /** 为当前仪表端口过滤事件，避免其他 GameTest 世界的事件影响本测试断言。 */
    private static List<ReactorMeltdownEvent> listenFor(
            ReactorInstrumentPortBlockEntity instrument,
            GameTestHelper helper
    ) {
        List<ReactorMeltdownEvent> events = new ArrayList<>();
        NeoForge.EVENT_BUS.addListener(ReactorMeltdownEvent.class, event -> {
            if (event.level() == helper.getLevel()
                    && event.instrumentPort().equals(instrument.getBlockPos())) {
                events.add(event);
            }
        });
        return events;
    }

    /** 放置结构契约规定的固定 5×5×5 方块。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            BlockPos position = new BlockPos(
                    entry.getKey().x(), entry.getKey().y(), entry.getKey().z());
            helper.setBlock(position, blockForId(entry.getValue()).defaultBlockState());
        }
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

    /** 游戏测试断言失败时立即终止当前测试。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    /** 统计事件前后的服务端实体集合，避免把 Iterable 误当作可变列表。 */
    private static int countEntities(GameTestHelper helper) {
        int count = 0;
        for (Object ignored : helper.getLevel().getEntities().getAll()) {
            count++;
        }
        return count;
    }

}
