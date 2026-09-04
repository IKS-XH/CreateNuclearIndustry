package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** 补齐 COOL-05 的多端口、双堆隔离、加载顺序和跨区块生命周期覆盖。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Coolant05CoverageGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos COLD_PORT = new BlockPos(1, 2, 4);
    private static final BlockPos HOT_PORT = new BlockPos(3, 2, 4);
    private static final BlockPos EXTRA_COLD_PORT = new BlockPos(2, 1, 4);
    private static final BlockPos EXTRA_HOT_PORT = new BlockPos(2, 3, 4);
    private static final BlockPos FORMATION_GAP = new BlockPos(0, 0, 0);
    private static final BlockPos SECOND_REACTOR_OFFSET = new BlockPos(6, 0, 0);

    private P1Coolant05CoverageGameTests() {
    }

    /** 两个冷端和两个热端同时失效、恢复，验证逐端口配额、缓存和共享库存守恒。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void multiplePortsRecoverTogetherWithIndependentBudgets(GameTestHelper helper) {
        buildStructure(helper, multiPortLayout());
        long startTick = helper.getLevel().getGameTime();

        helper.succeedWhen(() -> {
            if (helper.getLevel().getGameTime() - startTick < 5L) {
                return;
            }
            ReactorInstrumentPortBlockEntity instrument = instrument(helper, INSTRUMENT);
            require(helper, instrument.structureValid(), "多端口测试的结构未成型");

            ReactorPortBlockEntity cold = port(helper, COLD_PORT);
            ReactorPortBlockEntity extraCold = port(helper, EXTRA_COLD_PORT);
            ReactorPortBlockEntity hot = port(helper, HOT_PORT);
            ReactorPortBlockEntity extraHot = port(helper, EXTRA_HOT_PORT);
            require(helper, cold.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.COLD_COOLANT, null)
                            && extraCold.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.COLD_COOLANT, null),
                    "两个冷端没有同时绑定到当前仪表端口");
            require(helper, hot.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.HOT_COOLANT, null)
                            && extraHot.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.HOT_COOLANT, null),
                    "两个热端没有同时绑定到当前仪表端口");
            assertBindingCounts(helper, instrument, 2, 2, 12);

            BlockCapabilityCache<IFluidHandler, Direction> coldCache = cache(
                    helper, COLD_PORT, Direction.SOUTH);
            BlockCapabilityCache<IFluidHandler, Direction> extraColdCache = cache(
                    helper, EXTRA_COLD_PORT, Direction.SOUTH);
            BlockCapabilityCache<IFluidHandler, Direction> hotCache = cache(
                    helper, HOT_PORT, Direction.SOUTH);
            BlockCapabilityCache<IFluidHandler, Direction> extraHotCache = cache(
                    helper, EXTRA_HOT_PORT, Direction.SOUTH);
            IFluidHandler coldHandler = coldCache.getCapability();
            IFluidHandler extraColdHandler = extraColdCache.getCapability();
            IFluidHandler hotHandler = hotCache.getCapability();
            IFluidHandler extraHotHandler = extraHotCache.getCapability();
            require(helper, coldHandler != null && extraColdHandler != null
                            && hotHandler != null && extraHotHandler != null,
                    "多端口测试的初始世界 capability 不完整");
            require(helper, coldHandler != extraColdHandler && hotHandler != extraHotHandler,
                    "不同物理端口错误地共享同一个 capability handler");

            int configuredLimit = P1ServerConfig.VALUES.perPortFlowMbPerTick.get();
            require(helper, configuredLimit == 128,
                    "测试前提的逐端口流量上限不是 128 mB/t：" + configuredLimit);
            int coldAccepted = coldHandler.fill(
                    new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 512),
                    IFluidHandler.FluidAction.EXECUTE);
            int extraColdAccepted = extraColdHandler.fill(
                    new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 512),
                    IFluidHandler.FluidAction.EXECUTE);
            require(helper, coldAccepted == 128 && extraColdAccepted == 128,
                    "两个冷端没有分别获得 128 mB/t 配额："
                            + coldAccepted + "/" + extraColdAccepted);
            require(helper, instrument.snapshot().coldCoolantMb() == 256L,
                    "两个冷端写入共享库存的总量错误：" + instrument.snapshot().coldCoolantMb());

            instrument.setSnapshot(instrument.snapshot().withCoolantInventories(256L, 512L));
            int hotDrained = hotHandler.drain(512,
                    IFluidHandler.FluidAction.EXECUTE).getAmount();
            int extraHotDrained = extraHotHandler.drain(512,
                    IFluidHandler.FluidAction.EXECUTE).getAmount();
            require(helper, hotDrained == 128 && extraHotDrained == 128,
                    "两个热端没有分别获得 128 mB/t 配额："
                            + hotDrained + "/" + extraHotDrained);
            require(helper, instrument.snapshot().hotCoolantMb()
                            + hotDrained + extraHotDrained == 512L,
                    "热库存与两个热端输出不守恒");

            helper.setBlock(FORMATION_GAP, Blocks.AIR.defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(FORMATION_GAP));
            require(helper, !instrument.structureValid() && instrument.boundPortCount() == 0,
                    "结构失效后仍保留有效端口绑定");
            require(helper, cold.boundOwner() == null && extraCold.boundOwner() == null
                            && hot.boundOwner() == null && extraHot.boundOwner() == null,
                    "结构失效后多个端口仍保留旧所有者");
            require(helper, coldCache.getCapability() == null
                            && extraColdCache.getCapability() == null
                            && hotCache.getCapability() == null
                            && extraHotCache.getCapability() == null,
                    "结构失效后真实 BlockCapabilityCache 仍暴露旧端点");
            long coldBeforeStaleCall = instrument.snapshot().coldCoolantMb();
            require(helper, coldHandler.fill(
                    new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 128),
                    IFluidHandler.FluidAction.EXECUTE) == 0
                            && instrument.snapshot().coldCoolantMb() == coldBeforeStaleCall,
                    "失效结构中的旧冷端 handler 仍可写入共享库存");

            helper.setBlock(FORMATION_GAP, P1Blocks.REACTOR_CASING.get().defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(FORMATION_GAP));
            require(helper, instrument.structureValid(), "多端口结构恢复后仍然无效");
            require(helper, cold.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.COLD_COOLANT, null)
                            && extraCold.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.COLD_COOLANT, null)
                            && hot.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.HOT_COOLANT, null)
                            && extraHot.isBoundTo(instrument,
                            ReactorPortBlockEntity.BindingType.HOT_COOLANT, null),
                    "多个冷热端没有在同一次恢复中全部重绑");
            assertBindingCounts(helper, instrument, 2, 2, 12);
            require(helper, coldCache.getCapability() != null
                            && extraColdCache.getCapability() != null
                            && hotCache.getCapability() != null
                            && extraHotCache.getCapability() != null,
                    "多个端口恢复后真实 BlockCapabilityCache 没有全部恢复");
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(INSTRUMENT));
            assertBindingCounts(helper, instrument, 2, 2, 12);
            helper.succeed();
        });
    }

    /** 相邻两座反应堆重复重扫并替换一座仪表，端口不得被另一座有效反应堆抢占。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void adjacentReactorsKeepPortOwnershipIsolated(GameTestHelper helper) {
        buildCanonicalStructure(helper, BlockPos.ZERO);
        buildCanonicalStructure(helper, SECOND_REACTOR_OFFSET);
        long startTick = helper.getLevel().getGameTime();

        helper.succeedWhen(() -> {
            if (helper.getLevel().getGameTime() - startTick < 5L) {
                return;
            }
            ReactorInstrumentPortBlockEntity first = instrument(helper, INSTRUMENT);
            ReactorInstrumentPortBlockEntity second = instrument(
                    helper, INSTRUMENT.offset(SECOND_REACTOR_OFFSET));
            ReactorPortBlockEntity firstCold = port(helper, COLD_PORT);
            ReactorPortBlockEntity secondCold = port(helper, COLD_PORT.offset(SECOND_REACTOR_OFFSET));
            ReactorPortBlockEntity firstHot = port(helper, HOT_PORT);
            ReactorPortBlockEntity secondHot = port(helper, HOT_PORT.offset(SECOND_REACTOR_OFFSET));
            require(helper, first.structureValid() && second.structureValid(),
                    "相邻反应堆没有同时成型");
            require(helper, first != second
                            && firstCold.boundOwner() == first
                            && secondCold.boundOwner() == second
                            && firstHot.boundOwner() == first
                            && secondHot.boundOwner() == second,
                    "相邻反应堆初始所有权发生交叉");
            assertBindingCounts(helper, first, 1, 1, 10);
            assertBindingCounts(helper, second, 1, 1, 10);

            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), helper.absolutePos(INSTRUMENT));
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), helper.absolutePos(INSTRUMENT.offset(SECOND_REACTOR_OFFSET)));
            assertOwners(helper, firstCold, firstHot, first);
            assertOwners(helper, secondCold, secondHot, second);

            ReactorInstrumentPortBlockEntity oldFirst = first;
            helper.setBlock(INSTRUMENT, Blocks.AIR.defaultBlockState());
            helper.setBlock(INSTRUMENT, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
            ReactorStructureLifecycle.rescanInstrumentPortNow(
                    helper.getLevel(), helper.absolutePos(INSTRUMENT));
            ReactorInstrumentPortBlockEntity replacement = instrument(helper, INSTRUMENT);
            require(helper, replacement != oldFirst && replacement.structureValid(),
                    "第一座反应堆仪表端口没有替换并恢复有效");
            assertOwners(helper, firstCold, firstHot, replacement);
            assertOwners(helper, secondCold, secondHot, second);
            require(helper, !secondCold.bindTo(replacement,
                            ReactorPortBlockEntity.BindingType.COLD_COOLANT, null),
                    "第二座仍有效反应堆的冷端被第一座新仪表抢占");
            require(helper, secondCold.boundOwner() == second
                            && secondHot.boundOwner() == second,
                    "拒绝跨堆抢占后第二座端口所有权发生变化");
            assertBindingCounts(helper, replacement, 1, 1, 10);
            assertBindingCounts(helper, second, 1, 1, 10);
            helper.succeed();
        });
    }

    /** 端口先于结构加载、核心先于端口重绑，均不要求替换端口或手动再次扫结构。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void portFirstAndCoreFirstLoadOrderRebuildsBindings(GameTestHelper helper) {
        Map<ReactorStructureDefinition.LocalPosition, String> layout =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        layout.remove(local(COLD_PORT));
        layout.remove(local(HOT_PORT));
        helper.setBlock(COLD_PORT, P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
        helper.setBlock(HOT_PORT, P1Blocks.REACTOR_HOT_PORT.get().defaultBlockState());
        buildStructure(helper, layout);
        ReactorPortBlockEntity[] retainedCold = {port(helper, COLD_PORT)};
        ReactorPortBlockEntity[] retainedHot = {port(helper, HOT_PORT)};
        ReactorInstrumentPortBlockEntity[] oldInstrument = {null};
        IFluidHandler[] staleHandler = {null};
        int[] phase = {0};
        long[] phaseTick = {helper.getLevel().getGameTime()};

        helper.succeedWhen(() -> {
            ReactorInstrumentPortBlockEntity current = instrument(helper, INSTRUMENT);
            if (phase[0] == 0) {
                if (helper.getLevel().getGameTime() - phaseTick[0] < 5L) {
                    return;
                }
                require(helper, current.structureValid(),
                        "端口先加载后结构没有自动成型");
                require(helper, retainedCold[0].isBoundTo(current,
                                ReactorPortBlockEntity.BindingType.COLD_COOLANT, null)
                                && retainedHot[0].isBoundTo(current,
                                ReactorPortBlockEntity.BindingType.HOT_COOLANT, null),
                        "端口先加载后没有自动绑定冷热端");
                oldInstrument[0] = current;
                staleHandler[0] = helper.getLevel().getCapability(
                        Capabilities.FluidHandler.BLOCK,
                        helper.absolutePos(COLD_PORT), Direction.SOUTH);
                require(helper, staleHandler[0] != null,
                        "核心先加载测试无法取得旧冷端 handler");
                helper.setBlock(INSTRUMENT, Blocks.AIR.defaultBlockState());
                helper.setBlock(INSTRUMENT,
                        P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
                phase[0] = 1;
                phaseTick[0] = helper.getLevel().getGameTime();
                return;
            }

            if (current == oldInstrument[0] || !current.structureValid()) {
                return;
            }
            require(helper, retainedCold[0].isBoundTo(current,
                            ReactorPortBlockEntity.BindingType.COLD_COOLANT, null)
                            && retainedHot[0].isBoundTo(current,
                            ReactorPortBlockEntity.BindingType.HOT_COOLANT, null),
                    "核心先加载后保留端口没有自动接管");
            require(helper, staleHandler[0].fill(
                    new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 128),
                    IFluidHandler.FluidAction.EXECUTE) == 0,
                    "核心替换后旧冷端 handler 仍能写入");
            assertBindingCounts(helper, current, 1, 1, 10);
            helper.succeed();
        });
    }

    /** 以跨 chunk 的实际坐标执行端口先卸载、核心先卸载，再验证同一端口位置自动重载绑定。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 240)
    public static void crossChunkPortAndCoreUnloadReloadRebindsAutomatically(GameTestHelper helper) {
        int shiftX = crossingChunkShiftX(helper);
        BlockPos shiftedInstrument = INSTRUMENT.offset(shiftX, 0, 0);
        BlockPos shiftedCold = COLD_PORT.offset(shiftX, 0, 0);
        BlockPos shiftedHot = HOT_PORT.offset(shiftX, 0, 0);
        buildCanonicalStructure(helper, new BlockPos(shiftX, 0, 0));

        BlockPos absoluteInstrument = helper.absolutePos(shiftedInstrument);
        BlockPos absoluteCold = helper.absolutePos(shiftedCold);
        require(helper, new ChunkPos(absoluteInstrument).x != new ChunkPos(absoluteCold).x,
                "跨区块测试布局没有把核心和冷端分到不同 chunk");
        long startTick = helper.getLevel().getGameTime();
        int[] phase = {0};
        ReactorInstrumentPortBlockEntity[] oldInstrument = {null};
        ReactorPortBlockEntity[] retainedCold = {null};
        IFluidHandler[] staleHandler = {null};
        BlockCapabilityCache<IFluidHandler, Direction>[] cache = new BlockCapabilityCache[]{null};

        helper.succeedWhen(() -> {
            if (phase[0] == 0) {
                if (helper.getLevel().getGameTime() - startTick < 5L) {
                    return;
                }
                ReactorInstrumentPortBlockEntity instrument = instrument(helper, shiftedInstrument);
                ReactorPortBlockEntity cold = port(helper, shiftedCold);
                require(helper, instrument.structureValid() && cold.isBoundTo(instrument,
                                ReactorPortBlockEntity.BindingType.COLD_COOLANT, null),
                        "跨区块测试初始结构或冷端绑定缺失");
                oldInstrument[0] = instrument;
                retainedCold[0] = cold;
                staleHandler[0] = helper.getLevel().getCapability(
                        Capabilities.FluidHandler.BLOCK, absoluteCold, Direction.SOUTH);
                cache[0] = BlockCapabilityCache.create(
                        Capabilities.FluidHandler.BLOCK,
                        helper.getLevel(), absoluteCold, Direction.SOUTH);
                require(helper, staleHandler[0] != null && cache[0].getCapability() != null,
                        "跨区块测试初始 capability 缺失");
                unloadBlockEntity(helper, absoluteCold, cold);
                helper.setBlock(shiftedCold, Blocks.AIR.defaultBlockState());
                helper.setBlock(shiftedCold,
                        P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
                require(helper, cache[0].getCapability() == null,
                        "跨区块端口卸载后 capability cache 没有失效");
                phase[0] = 1;
                return;
            }

            ReactorInstrumentPortBlockEntity instrument = instrument(helper, shiftedInstrument);
            BlockEntity coldEntity = helper.getBlockEntity(shiftedCold);
            if (phase[0] == 1) {
                if (!(coldEntity instanceof ReactorPortBlockEntity cold)
                        || cold == retainedCold[0]
                        || !cold.isBoundTo(instrument,
                        ReactorPortBlockEntity.BindingType.COLD_COOLANT, null)) {
                    return;
                }
                require(helper, cache[0].getCapability() != null,
                        "跨区块端口重载后 capability cache 没有恢复");
                require(helper, staleHandler[0].fill(
                        new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 128),
                        IFluidHandler.FluidAction.EXECUTE) == 0,
                        "跨区块端口重载后旧 handler 仍能写入");
                unloadBlockEntity(helper, absoluteInstrument, instrument);
                helper.setBlock(shiftedInstrument, Blocks.AIR.defaultBlockState());
                helper.setBlock(shiftedInstrument,
                        P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
                phase[0] = 2;
                return;
            }

            BlockEntity replacementEntity = helper.getBlockEntity(shiftedInstrument);
            if (!(replacementEntity instanceof ReactorInstrumentPortBlockEntity replacement)
                    || replacement == oldInstrument[0] || !replacement.structureValid()) {
                return;
            }
            require(helper, coldEntity instanceof ReactorPortBlockEntity cold
                            && cold == retainedCold[0]
                            && cold.isBoundTo(replacement,
                            ReactorPortBlockEntity.BindingType.COLD_COOLANT, null),
                    "跨区块核心重载后原冷端没有绑定到新核心");
            ReactorPortBlockEntity hot = port(helper, shiftedHot);
            require(helper, hot.isBoundTo(replacement,
                            ReactorPortBlockEntity.BindingType.HOT_COOLANT, null),
                    "跨区块核心重载后热端没有绑定到新核心");
            require(helper, cache[0].getCapability() != null,
                    "跨区块核心重载后 capability cache 没有再次恢复");
            require(helper, staleHandler[0].fill(
                    new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 128),
                    IFluidHandler.FluidAction.EXECUTE) == 0,
                    "跨区块核心重载后最初旧 handler 仍能写入");
            assertBindingCounts(helper, replacement, 1, 1, 10);
            helper.succeed();
        });
    }

    /** 生成包含两个冷端、两个热端的合法结构布局。 */
    private static Map<ReactorStructureDefinition.LocalPosition, String> multiPortLayout() {
        Map<ReactorStructureDefinition.LocalPosition, String> layout =
                new HashMap<>(ReactorStructureDefinition.canonicalTemplate());
        layout.put(local(EXTRA_COLD_PORT), "create_nuclear_industry:reactor_cold_port");
        layout.put(local(EXTRA_HOT_PORT), "create_nuclear_industry:reactor_hot_port");
        return layout;
    }

    /** 按局部偏移放置一座完整 canonical 反应堆。 */
    private static void buildCanonicalStructure(GameTestHelper helper, BlockPos offset) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            BlockPos localPosition = new BlockPos(
                    entry.getKey().x(), entry.getKey().y(), entry.getKey().z()).offset(offset);
            helper.setBlock(localPosition, blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 按结构契约放置任意合法测试布局。 */
    private static void buildStructure(
            GameTestHelper helper,
            Map<ReactorStructureDefinition.LocalPosition, String> layout
    ) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry : layout.entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 返回指定局部坐标的结构局部位置。 */
    private static ReactorStructureDefinition.LocalPosition local(BlockPos position) {
        return new ReactorStructureDefinition.LocalPosition(
                position.getX(), position.getY(), position.getZ());
    }

    /** 取得指定局部位置的仪表实体。 */
    private static ReactorInstrumentPortBlockEntity instrument(
            GameTestHelper helper,
            BlockPos position
    ) {
        BlockEntity entity = helper.getBlockEntity(position);
        require(helper, entity instanceof ReactorInstrumentPortBlockEntity,
                "指定坐标没有仪表端口方块实体：" + position);
        return (ReactorInstrumentPortBlockEntity) entity;
    }

    /** 取得指定局部位置的流体端口实体。 */
    private static ReactorPortBlockEntity port(GameTestHelper helper, BlockPos position) {
        BlockEntity entity = helper.getBlockEntity(position);
        require(helper, entity instanceof ReactorPortBlockEntity,
                "指定坐标没有流体端口方块实体：" + position);
        return (ReactorPortBlockEntity) entity;
    }

    /** 创建指定端口方向的真实 NeoForge capability cache。 */
    private static BlockCapabilityCache<IFluidHandler, Direction> cache(
            GameTestHelper helper,
            BlockPos position,
            Direction side
    ) {
        return BlockCapabilityCache.create(
                Capabilities.FluidHandler.BLOCK,
                helper.getLevel(), helper.absolutePos(position), side);
    }

    /** 验证一个仪表的冷/热端口数量与全部绑定数量，防止重扫造成重复条目。 */
    private static void assertBindingCounts(
            GameTestHelper helper,
            ReactorInstrumentPortBlockEntity instrument,
            int coldCount,
            int hotCount,
            int totalCount
    ) {
        List<ReactorPortBlockEntity> cold = instrument.boundPorts(
                ReactorPortBlockEntity.BindingType.COLD_COOLANT);
        List<ReactorPortBlockEntity> hot = instrument.boundPorts(
                ReactorPortBlockEntity.BindingType.HOT_COOLANT);
        require(helper, cold.size() == coldCount && hot.size() == hotCount,
                "冷热端绑定数量错误：" + cold.size() + "/" + hot.size());
        require(helper, new HashSet<>(cold).size() == cold.size()
                        && new HashSet<>(hot).size() == hot.size()
                        && instrument.boundPortCount() == totalCount,
                "重扫后出现重复端口绑定或绑定总数漂移：" + instrument.boundPortCount());
    }

    /** 验证两个端口都只归属于传入的唯一仪表对象。 */
    private static void assertOwners(
            GameTestHelper helper,
            ReactorPortBlockEntity cold,
            ReactorPortBlockEntity hot,
            ReactorInstrumentPortBlockEntity owner
    ) {
        require(helper, cold.isBoundTo(owner,
                        ReactorPortBlockEntity.BindingType.COLD_COOLANT, null)
                        && hot.isBoundTo(owner,
                        ReactorPortBlockEntity.BindingType.HOT_COOLANT, null),
                "端口没有归属于指定仪表所有者");
    }

    /** 计算让 canonical 结构跨过 X 方向 chunk 边界的局部偏移。 */
    private static int crossingChunkShiftX(GameTestHelper helper) {
        int originMod = Math.floorMod(helper.absolutePos(BlockPos.ZERO).getX(), 16);
        return Math.floorMod(14 - originMod, 16);
    }

    /** 调用真实方块实体卸载入口并短暂切换其所在 chunk 的 loaded 标志。 */
    private static void unloadBlockEntity(
            GameTestHelper helper,
            BlockPos absolutePosition,
            BlockEntity entity
    ) {
        require(helper, helper.getLevel().getBlockEntity(absolutePosition) == entity,
                "卸载前世界中的方块实体身份不一致");
        var chunk = helper.getLevel().getChunkAt(absolutePosition);
        chunk.setLoaded(false);
        entity.onChunkUnloaded();
        chunk.setLoaded(true);
        helper.getLevel().removeBlockEntity(absolutePosition);
        require(helper, helper.getLevel().getBlockEntity(absolutePosition) == null
                        && entity.isRemoved(),
                "方块实体卸载入口没有移除旧实体");
    }

    /** 将模板注册 ID 映射到实际方块。 */
    private static net.minecraft.world.level.block.Block blockForId(String id) {
        return switch (id) {
            case "minecraft:air" -> Blocks.AIR;
            case "create_nuclear_industry:reactor_casing" -> P1Blocks.REACTOR_CASING.get();
            case "create_nuclear_industry:reactor_window" -> P1Blocks.REACTOR_WINDOW.get();
            case "create_nuclear_industry:reactor_instrument_port" ->
                    P1Blocks.REACTOR_INSTRUMENT_PORT.get();
            case "create_nuclear_industry:reactor_cold_port" -> P1Blocks.REACTOR_COLD_PORT.get();
            case "create_nuclear_industry:reactor_hot_port" -> P1Blocks.REACTOR_HOT_PORT.get();
            case "create_nuclear_industry:reactor_refueling_port" ->
                    P1Blocks.REACTOR_REFUELING_PORT.get();
            case "create_nuclear_industry:reactor_fuel_rod" -> P1Blocks.REACTOR_FUEL_ROD.get();
            case "create_nuclear_industry:control_rod_drive" -> P1Blocks.CONTROL_ROD_DRIVE.get();
            default -> throw new IllegalArgumentException("未知反应堆方块 ID：" + id);
        };
    }

    /** 统一把断言失败交给 GameTest。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
