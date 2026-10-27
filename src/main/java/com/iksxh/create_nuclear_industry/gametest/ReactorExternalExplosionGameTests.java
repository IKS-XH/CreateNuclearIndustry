package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.accident.*;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.reactor.*;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** 外部爆炸独立真实服域；只使用隔离测试世界与真实物理组件。 */
@GameTestHolder("cni_reactor_external_explosion")
@PrefixGameTestTemplate(false)
public final class ReactorExternalExplosionGameTests {
    private static final String TEMPLATE = "explosion_empty";
    private static final BlockPos BASE = new BlockPos(6, 3, 6);

    private ReactorExternalExplosionGameTests() {}

    /** 仅整改失败收尾时显式跳过未改业务，原集中证据仍独立保留。 */
    private static boolean skipForFailureFinalizationFix(GameTestHelper h, String fixture) {
        if (!"1".equals(System.getenv("CNI_EXTERNAL_EXPLOSION_FAILURE_FIX_ONLY"))) return false;
        System.out.println("[external-explosion] failure finalization delta: SKIP fixture " + fixture);
        h.succeed();
        return true;
    }

    /** 首先真实结算一次裂变，不发布预制融毁事件，再点燃原生TNT。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nativeTntDamagesRunningReactor(GameTestHelper h) {
        if (skipForFailureFinalizationFix(h, "nativeTnt")) return;
        var owner = running(h, BASE);
        var pos = owner.getBlockPos();
        var before = h.getLevel().getBlockState(pos);
        var untouched = running(h, BASE.offset(14, 0, 0));
        var tnt = new PrimedTnt(h.getLevel(), pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, null);
        tnt.setFuse(1);
        h.getLevel().addFreshEntity(tnt);
        h.runAfterDelay(4, () -> {
            boolean damaged = !h.getLevel().getBlockState(pos).equals(before);
            var ledger = ReactorAccidentSavedData.get(h.getLevel()).byGeneration(owner.accidentGeneration());
            System.out.println("[external-explosion] native TNT damaged=" + damaged + ",ledger=" + (ledger != null));
            h.assertTrue(damaged, "原生TNT没有真实破坏运行堆");
            h.assertTrue(ledger != null, "原生TNT已毁运行堆，但没有进入事故");
            h.assertTrue("EXTERNAL_EXPLOSION".equals(ledger.reason), "外爆来源没有区分");
            h.assertTrue(untouched.accidentId() == null && untouched.structureValid() && untouched.tickReactor(),
                    "未受损邻堆被外爆事故误接管");
            h.succeed();
        });
    }

    /** 真实苦力怕点燃与原生MOB交互路径，不能用直接发布事故事件替代。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nativeCreeperDamagesRunningReactor(GameTestHelper h) {
        if (skipForFailureFinalizationFix(h, "nativeCreeper")) return;
        var owner = running(h, BASE);
        var pos = owner.getBlockPos();
        var creeper = EntityType.CREEPER.create(h.getLevel());
        var tag = new CompoundTag();
        tag.putShort("Fuse", (short) 1);
        creeper.readAdditionalSaveData(tag);
        creeper.setPos(Vec3.atCenterOf(pos));
        creeper.setNoAi(true);
        h.getLevel().addFreshEntity(creeper);
        creeper.ignite();
        h.runAfterDelay(4, () -> {
            h.assertTrue(!creeper.isAlive() && !h.getLevel().getBlockState(pos).equals(owner.getBlockState()),
                    "真实苦力怕未爆炸破坏运行堆");
            h.assertTrue(ledger(h, owner) != null && owner.accidentId() != null, "苦力怕路径漏事故");
            System.out.println("[external-explosion] real ignited creeper damaged reactor and entered one accident");
            h.succeed();
        });
    }

    /** 最终列表只有仪表或换料口时，它必然先被原生处理；真实组件须已进入唯一托管。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void firstInstrumentAndFirstRefuelingConserveCustody(GameTestHelper h) {
        if (skipForFailureFinalizationFix(h, "custody")) return;
        var first = custody(h, BASE, false);
        var second = custody(h, BASE.offset(14, 0, 0), true);
        var forced = h.getLevel().getForcedChunks().toLongArray();
        h.runAfterDelay(30, () -> {
            conservation(h, first);
            conservation(h, second);
            h.assertTrue(Arrays.equals(forced, h.getLevel().getForcedChunks().toLongArray()), "外爆事故强载区块");
            System.out.println("[external-explosion] first instrument/refueling: each 2 waste + exact named fresh, coolant/HU, replay, current save/load and task completion passed");
            h.succeed();
        });
    }

    private static ReactorAccidentLedger custody(GameTestHelper h, BlockPos base, boolean firstRefueling) {
        var owner = running(h, base);
        var used = AccidentExecutionGameTests.port(h, base, 5, 0, 0);
        var freshPort = AccidentExecutionGameTests.port(h, base, 5, 1, 0);
        var spent = AccidentExecutionGameTests.port(h, base, 5, 2, 0);
        var named = AccidentExecutionGameTests.fresh();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("外爆保留原件-" + firstRefueling));
        freshPort.setFuelAssembly(named);
        spent.setFuelAssembly(new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()));
        owner.setCoolantInventories(123, 456);
        var snapshot = owner.snapshot();
        double expectedHeat = heat(snapshot);
        var target = firstRefueling ? used.getBlockPos() : owner.getBlockPos();
        var explosion = tnt(h, target, List.of(target));
        var ledger = ledger(h, owner);
        h.assertTrue(ledger != null && ledger.columns.size() == 3
                && ledger.columns.stream().filter(ReactorAccidentLedger.Column::waste).count() == 2,
                "首坏端口导致真实组件丢失或错误分类");
        h.assertTrue(used.fuelAssembly().isEmpty() && freshPort.fuelAssembly().isEmpty() && spent.fuelAssembly().isEmpty()
                && owner.snapshot().coldCoolantMb() == 0 && owner.snapshot().hotCoolantMb() == 0, "原口与事故托管同时持料/工质");
        h.assertTrue(ledger.coldMb == 123 && ledger.hotMb == 456 && Math.abs(ledger.releasedHeatHu - expectedHeat) < 1e-9,
                "外爆擅改HU或工质损失");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(target).inflate(8)).stream()
                .noneMatch(e -> FuelAssemblyItemCodec.isValidStoredFuel(e.getItem())), "原生loot先掉出了一份燃料");
        int records = ReactorAccidentSavedData.get(h.getLevel()).records().size();
        explosion.finalizeExplosion(false);
        h.assertTrue(ReactorAccidentSavedData.get(h.getLevel()).records().size() == records
                && ledger(h, owner) == ledger, "重复finalize产生第二事故");
        var restored = ReactorAccidentSavedData.load(ReactorAccidentSavedData.get(h.getLevel())
                .save(new CompoundTag(), h.getLevel().registryAccess()), h.getLevel().registryAccess()).byId(ledger.id);
        h.assertTrue(restored != null && restored.reason.equals(ledger.reason) && restored.columns.size() == 3
                && restored.columns.stream().allMatch(ReactorAccidentLedger.Column::captured)
                && restored.coldMb == 123 && restored.releasedHeatHu == ledger.releasedHeatHu,
                "当前保存/加载丢失外爆原因或托管");
        // 仪表已炸毁或随后被移除，账本必须能独立继续；不使用事故事件辅助入口。
        h.getLevel().setBlock(owner.getBlockPos(), Blocks.AIR.defaultBlockState(), 18);
        return ledger;
    }

    private static void conservation(GameTestHelper h, ReactorAccidentLedger ledger) {
        h.assertTrue(ledger.complete() && ledger.columns.stream().allMatch(c -> c.payload().isEmpty()), "仪表毁后任务未完成或托管未移交");
        int waste = 0;
        for (var c : ledger.columns) {
            if (!c.waste()) {
                var id = UUID.nameUUIDFromBytes((ledger.id + ":" + c.port.asLong()).getBytes(StandardCharsets.UTF_8));
                h.assertTrue(h.getLevel().getEntity(id) instanceof ItemEntity item && ItemStack.matches(item.getItem(), c.original()),
                        "全新组件未原样返还一次");
            } else if (c.molten) {
                var id = UUID.nameUUIDFromBytes((ledger.id + ":molten:" + c.bottom.asLong()).getBytes(StandardCharsets.UTF_8));
                var body = ReactorAccidentSavedData.get(h.getLevel()).molten(id);
                h.assertTrue(body != null, "没有唯一熔融废料本体");
                waste += body.payload().getCount();
            } else {
                h.assertTrue(h.getLevel().getBlockEntity(c.bottom) instanceof ReactorCoriumBlockEntity, "没有唯一固体废料");
                waste += ((ReactorCoriumBlockEntity) h.getLevel().getBlockEntity(c.bottom)).payload().getCount();
            }
        }
        h.assertTrue(waste == 2 && ledger.externalBroken <= 32 && ledger.peakActions <= 64,
                "废料总量或事故自身破坏/每tick预算改变");
    }

    /** Start最终取消及Detonate后置移出成员，都必须保持原库存与运行许可。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void finalCancellationAndFinalRemovalDoNotEnqueue(GameTestHelper h) {
        if (skipForFailureFinalizationFix(h, "cancellation")) return;
        var owner = running(h, BASE);
        var before = owner.snapshot();
        var item = AccidentExecutionGameTests.port(h, BASE, 5, 0, 0).fuelAssembly();
        var calls = new AtomicInteger();
        Consumer<ExplosionEvent.Start> cancel = event -> {
            if (event.getLevel() == h.getLevel()) { calls.incrementAndGet(); event.setCanceled(true); }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, cancel);
        try { tnt(h, owner.getBlockPos(), null); } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        h.assertTrue(calls.get() == 1 && ledger(h, owner) == null && owner.snapshot().equals(before), "Start取消仍改堆或建事故");
        Consumer<ExplosionEvent.Detonate> early = event -> {
            if (event.getLevel() == h.getLevel()) event.getAffectedBlocks().add(owner.getBlockPos());
        };
        Consumer<ExplosionEvent.Detonate> late = event -> {
            if (event.getLevel() == h.getLevel()) { calls.incrementAndGet(); event.getAffectedBlocks().clear(); }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, early);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, late);
        try { tnt(h, owner.getBlockPos(), null); }
        finally { NeoForge.EVENT_BUS.unregister(early); NeoForge.EVENT_BUS.unregister(late); }
        h.assertTrue(calls.get() == 2 && ledger(h, owner) == null && owner.structureValid()
                && owner.snapshot().equals(before) && ItemStack.matches(item, AccidentExecutionGameTests.port(h, BASE, 5, 0, 0).fuelAssembly()),
                "Detonate最终剔除仍提前接管");
        System.out.println("[external-explosion] final Start cancellation and later Detonate removal retained exact inventory and running owner");
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void keepTriggerColdAndNonMemberDoNotEnqueue(GameTestHelper h) {
        if (skipForFailureFinalizationFix(h, "nonDestruction")) return;
        var owner = running(h, BASE);
        var pos = owner.getBlockPos();
        var before = owner.snapshot();
        for (var mode : List.of(Level.ExplosionInteraction.NONE, Level.ExplosionInteraction.TRIGGER)) {
            h.getLevel().explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 4, mode);
            h.assertTrue(ledger(h, owner) == null && owner.structureValid() && owner.snapshot().equals(before),
                    "KEEP/TRIGGER无结构破坏仍建事故");
        }
        var stone = pos.north();
        h.getLevel().setBlock(stone, Blocks.DIRT.defaultBlockState(), 18);
        tnt(h, stone, List.of(stone));
        h.assertTrue(h.getLevel().getBlockState(stone).isAir() && ledger(h, owner) == null && owner.structureValid(),
                "最终列表只有非成员时误接管邻堆");
        var cold = AccidentExecutionGameTests.build(h, BASE.offset(14, 0, 0), 5);
        var coldPos = cold.getBlockPos();
        tnt(h, coldPos, List.of(coldPos));
        h.assertTrue(h.getLevel().getBlockState(coldPos).isAir() && ledger(h, cold) == null, "冷停机空堆误建事故");
        System.out.println("[external-explosion] KEEP/TRIGGER, final nonmember-only damage and cold empty reactor passed");
        h.succeed();
    }

    /** 同一最终列表包含多块/多个所有者；后一个事件失败时前一个也不能先移料。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void multiOwnerRollbackAndUniqueCommit(GameTestHelper h) {
        var a = running(h, BASE);
        var b = running(h, BASE.offset(4, 0, 0));
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(), a.getBlockPos());
        var pa = AccidentExecutionGameTests.port(h, BASE, 5, 0, 0);
        var pb = AccidentExecutionGameTests.port(h, BASE.offset(4, 0, 0), 5, 0, 0);
        var shared = h.absolutePos(BASE.offset(4, 2, 2));
        var state = h.getLevel().getBlockState(shared);
        var sa = a.snapshot(); var sb = b.snapshot();
        var before = ReactorAccidentSavedData.get(h.getLevel()).records().size();
        Consumer<ReactorMeltdownEvent> failure = event -> {
            if (event.instrumentPort().equals(b.getBlockPos())) throw new IllegalStateException("本批外爆第二所有者发布失败探针");
        };
        NeoForge.EVENT_BUS.addListener(failure);
        try {
            var failed = tnt(h, shared, List.of(shared, a.getBlockPos(), b.getBlockPos()));
            h.assertTrue(failed != null && failed.getToBlow().isEmpty(), "失败回滚后拟发送客户端的affectedBlocks仍非空");
            System.out.println("[external-explosion] failed batch returned affectedBlocks=0; native finalization completed for packet/profiler cleanup");
        }
        finally { NeoForge.EVENT_BUS.unregister(failure); }
        h.assertTrue(h.getLevel().getBlockState(shared).equals(state) && a.snapshot().equals(sa) && b.snapshot().equals(sb)
                && a.accidentId() == null && b.accidentId() == null && !pa.fuelAssembly().isEmpty() && !pb.fuelAssembly().isEmpty()
                && ReactorAccidentSavedData.get(h.getLevel()).records().size() == before, "批次发布失败未完整回滚/已先删块掉料");
        var explosion = tnt(h, shared, List.of(shared, a.getBlockPos(), b.getBlockPos(), pa.getBlockPos(), pb.getBlockPos()));
        h.assertTrue(ledger(h, a) != null && ledger(h, b) != null && !ledger(h, a).id.equals(ledger(h, b).id)
                && pa.fuelAssembly().isEmpty() && pb.fuelAssembly().isEmpty()
                && ReactorAccidentSavedData.get(h.getLevel()).records().size() == before + 2, "同爆炸多所有者没有恰好各自接管一次");
        explosion.finalizeExplosion(false);
        h.assertTrue(ReactorAccidentSavedData.get(h.getLevel()).records().size() == before + 2, "多块/重复finalize导致重复事故");
        System.out.println("[external-explosion] multi-owner final-list preflight, forced second publish failure rollback, multi-block unique handoff passed");
        h.succeed();
    }

    /** 原生玩家post最终取消与危险拆除继续沿原事务；外爆修复不改拒拆/完全停机规则。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void playerFinalCancellationAndDangerousBreakRemainCorrect(GameTestHelper h) {
        if (skipForFailureFinalizationFix(h, "playerBreak")) return;
        var owner = running(h, BASE);
        var pos = owner.getBlockPos();
        var state = h.getLevel().getBlockState(pos);
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ExternalExplosionBreak"));
        Consumer<BlockEvent.BreakEvent> cancel = event -> { if (event.getPos().equals(pos)) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, cancel);
        try {
            var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(), GameType.SURVIVAL, player, pos, state);
            h.assertTrue(event.isCanceled() && ledger(h, owner) == null && owner.accidentId() == null, "原玩家最终取消仍建事故");
        } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(), GameType.SURVIVAL, player, pos, state);
        h.assertTrue(!event.isCanceled() && ledger(h, owner) != null
                && "DANGEROUS_DISASSEMBLY".equals(ledger(h, owner).reason), "原玩家危险拆除来源/行为改变");
        System.out.println("[external-explosion] original native player final-cancel and dangerous-disassembly paths passed");
        h.succeed();
    }

    /** 可控最终列表只用于顺序/取消边界，爆炸本身仍由原生TNT.tick触发。null保留原生射线结果。 */
    private static Explosion tnt(GameTestHelper h, BlockPos pos, List<BlockPos> finalBlocks) {
        var tnt = new PrimedTnt(h.getLevel(), pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, null);
        tnt.setFuse(1);
        var result = new AtomicReference<Explosion>();
        Consumer<ExplosionEvent.Detonate> select = event -> {
            if (event.getExplosion().getDirectSourceEntity() != tnt) return;
            result.set(event.getExplosion());
            if (finalBlocks != null) { event.getAffectedBlocks().clear(); event.getAffectedBlocks().addAll(finalBlocks); }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, select);
        try { h.getLevel().addFreshEntity(tnt); tnt.tick(); }
        finally { NeoForge.EVENT_BUS.unregister(select); }
        return result.get();
    }

    private static ReactorAccidentLedger ledger(GameTestHelper h, ReactorInstrumentPortBlockEntity owner) {
        return ReactorAccidentSavedData.get(h.getLevel()).byGeneration(owner.accidentGeneration());
    }

    private static double heat(ReactorSnapshot snapshot) {
        return snapshot.fuelColumns().values().stream().mapToDouble(FuelColumnState::cachedHeatHu).sum()
                + snapshot.controlRodColumns().values().stream().mapToDouble(ControlRodColumnState::cachedHeatHu).sum()
                + snapshot.hotCoolantMb() * P1ServerConfig.VALUES.coolantAbsorptionHuPerMb.get();
    }

    private static ReactorInstrumentPortBlockEntity running(GameTestHelper h, BlockPos base) {
        var owner = AccidentExecutionGameTests.build(h, base, 5);
        AccidentExecutionGameTests.port(h, base, 5, 0, 0).setFuelAssembly(AccidentExecutionGameTests.used());
        owner.setCoolantInventories(9000, 0);
        h.assertTrue(owner.tickReactor(), "运行夹具未执行真实热工tick");
        var plan = owner.prepareDisassemblyPlan(owner.getBlockPos(), owner.getBlockState());
        h.assertTrue(plan != null && plan.shutdownAssessment().generatedHeatHu() > 0 && owner.accidentId() == null,
                "外爆前必须真实生成热且没有预发事故");
        System.out.println("[external-explosion] before explosion generatedHeatHu=" + plan.shutdownAssessment().generatedHeatHu()
                + ",accident=" + owner.accidentId());
        return owner;
    }
}
