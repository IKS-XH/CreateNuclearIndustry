package com.iksxh.create_nuclear_industry.radiation;

import com.iksxh.create_nuclear_industry.config.RadiationConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** 非玩家活体的服务端剂量及维度调度；接收目标来自实体 tick，采样使用玩家结算后的预算。 */
public final class RadiationLivingEntities {
    private static final int STARTS_PER_TICK = 4;
    private static final int RESUMES_PER_TICK = 4;
    private static final int MAX_PENDING = 16;
    private static final int MAX_SAMPLE_AGE_TICKS = 120;
    private static final int MAX_TARGET_DRIFT_BLOCKS = 8;

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "create_nuclear_industry");
    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Exposure>> EXPOSURE =
            ATTACHMENTS.register("radiation_dose", () -> AttachmentType.serializable(Exposure::new).build());
    private static final Map<ServerLevel, RadiationLivingEntities> LEVELS = new IdentityHashMap<>();

    private final LivingEntity[] candidates = new LivingEntity[STARTS_PER_TICK];
    private final Int2ObjectLinkedOpenHashMap<Work> pending = new Int2ObjectLinkedOpenHashMap<>(MAX_PENDING);
    private int candidateCount;
    private int lastStarted;

    private RadiationLivingEntities() {}

    /** 在模组事件总线上注册实体原生 NBT 附件，剂量和计时仅由服务端使用。 */
    public static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
    }

    /** 只保存剂量 D 和连续安全/伤害 tick；周期采样时钟属于本次实体在线生命周期。 */
    private static final class Exposure implements INBTSerializable<CompoundTag> {
        RadiationDoseState state = RadiationDoseState.ZERO;
        int elapsedTicks;

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider registries) {
            return state.dose() > 0 ? state.save() : null;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
            state = RadiationDoseState.load(tag, RadiationConfig.parameters());
        }
    }

    private static final class Work {
        final LivingEntity entity;
        final RadiationSourceIndex.LivingTarget key;
        final Vec3 target;
        final BlockPos cell;
        final RadiationParameters parameters;
        final int startTick;
        int seenTick;

        Work(LivingEntity entity, RadiationParameters parameters, int now) {
            this.entity = entity;
            this.key = new RadiationSourceIndex.LivingTarget(entity.getId());
            this.target = entity.position();
            this.cell = entity.blockPosition();
            this.parameters = parameters;
            this.startTick = now;
            this.seenTick = now;
        }

        boolean current(ServerLevel level, RadiationParameters parameters, int now) {
            // 续接的是开工位置的一次观测。正常走动有游标连续性，大位移或区块离开丢弃旧观测。
            int drift = Math.min(parameters.detectionRadiusBlocks(), MAX_TARGET_DRIFT_BLOCKS);
            return seenTick == now && now - startTick < MAX_SAMPLE_AGE_TICKS && this.parameters.equals(parameters)
                    && entity.isAlive() && entity.level() == level && entity.position().distanceToSqr(target) <= drift * drift
                    && loaded(level, cell) && loaded(level, entity.blockPosition());
        }

        private static boolean loaded(ServerLevel level, BlockPos pos) {
            return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
        }
    }

    static void tick(ServerLevel level, LivingEntity entity) {
        RadiationParameters parameters = RadiationConfig.parameters();
        Exposure exposure = entity.getExistingDataOrNull(EXPOSURE);
        int interval = parameters.sampleIntervalTicks();
        if (exposure != null) {
            RadiationDoseState before = exposure.state;
            if (before.dose() > parameters.maxDose())
                before = new RadiationDoseState(parameters.maxDose(), before.safeTicks(), before.damageTicks());
            exposure.state = RadiationServerEvents.advanceDamage(entity, before, parameters, true);
            RadiationServerEvents.applySymptoms(entity, exposure.state, parameters, true);
            if (exposure.elapsedTicks < interval)
                exposure.elapsedTicks++;
        }
        if (!entity.isAlive())
            return;

        if (!parameters.enabled()) {
            if (exposure != null && exposure.elapsedTicks >= interval)
                settle(entity, exposure, 0, parameters);
            return;
        }
        // 无剂量、尚未采样的实体只查现有附件；采样实际开工时才分配时钟。
        if ((exposure == null ? entity.tickCount : exposure.elapsedTicks) >= interval)
            LEVELS.computeIfAbsent(level, ignored -> new RadiationLivingEntities()).offer(level, entity);
    }

    private void offer(ServerLevel level, LivingEntity entity) {
        Work work = pending.get(entity.getId());
        if (work != null) {
            work.seenTick = level.getServer().getTickCount();
            return;
        }

        // 从上次开工 ID 的下一位循环选取。即使实体 tick 的遍历顺序固定，早出现的实体也不能独占名额。
        int offset = entity.getId() - lastStarted - 1;
        int insert = candidateCount;
        while (insert > 0) {
            LivingEntity preceding = candidates[insert - 1];
            if (preceding == entity)
                return;
            if (Integer.compareUnsigned(offset, preceding.getId() - lastStarted - 1) >= 0)
                break;
            insert--;
        }
        if (insert == candidates.length)
            return;
        int move = Math.min(candidateCount, candidates.length - 1) - insert;
        System.arraycopy(candidates, insert, candidates, insert + 1, move);
        candidates[insert] = entity;
        candidateCount = Math.min(candidateCount + 1, candidates.length);
    }

    static void finishTick(ServerLevel level) {
        RadiationLivingEntities scheduler = LEVELS.get(level);
        if (scheduler != null)
            scheduler.finish(level, RadiationConfig.parameters());
    }

    private void finish(ServerLevel level, RadiationParameters parameters) {
        if (candidateCount == 0 && pending.isEmpty())
            return;
        int now = level.getServer().getTickCount();
        var iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            Work work = iterator.next();
            if (!parameters.enabled() || !work.current(level, parameters, now)) {
                RadiationSourceIndex.cancelLiving(level, work.key);
                iterator.remove();
            }
        }
        if (parameters.enabled() && (candidateCount > 0 || !pending.isEmpty())) {
            RadiationSourceIndex index = RadiationSourceIndex.get(level);
            // 一次续接可能消耗整帧余量，将其转到队尾使其他游标有机会在下一帧推进。
            int resumes = Math.min(RESUMES_PER_TICK, pending.size());
            for (int i = 0; i < resumes && index.livingWorkAvailable(level); i++) {
                Work work = pending.removeFirst();
                // 库存交接等事件可能已撤销旧任务。重建必须重新参加开工轮转，避免绕过开工上限。
                if (index.hasLivingWork(work.key) && !advance(level, index, work))
                    pending.put(work.key.entityId(), work);
            }
            for (int i = 0; i < candidateCount && pending.size() < MAX_PENDING && index.livingWorkAvailable(level); i++) {
                LivingEntity entity = candidates[i];
                if (!entity.isAlive() || entity.level() != level)
                    continue;
                lastStarted = entity.getId();
                Exposure exposure = entity.getExistingDataOrNull(EXPOSURE);
                if (exposure == null) {
                    exposure = entity.getData(EXPOSURE);
                    exposure.elapsedTicks = parameters.sampleIntervalTicks();
                }
                Work work = new Work(entity, parameters, now);
                if (!advance(level, index, work))
                    pending.put(work.key.entityId(), work);
            }
        }
        Arrays.fill(candidates, 0, candidateCount, null);
        candidateCount = 0;
    }

    private static boolean advance(ServerLevel level, RadiationSourceIndex index, Work work) {
        var sample = index.sampleLiving(level, work.key, work.target, work.parameters);
        if (!sample.complete())
            return false;
        double absorbed = RadiationServerEvents.absorption(work.entity, sample, work.parameters, true);
        settle(work.entity, work.entity.getData(EXPOSURE), absorbed, work.parameters);
        return true;
    }

    private static void settle(LivingEntity entity, Exposure exposure, double absorbed, RadiationParameters parameters) {
        // 积压期间未知的场强不能当作安全。完成结果最多结算一个正常采样周期，避免移动后用新场回填长时间积压。
        int elapsed = Math.min(exposure.elapsedTicks, parameters.sampleIntervalTicks());
        exposure.elapsedTicks = 0;
        RadiationDoseState before = exposure.state;
        if (before.dose() == 0 && absorbed <= parameters.safeAbsorptionPerSecond())
            return;
        exposure.state = RadiationServerEvents.advanceDose(before, absorbed, elapsed, parameters);
        if (before.dose() < parameters.symptomDose())
            RadiationServerEvents.applySymptoms(entity, exposure.state, parameters, true);
    }

    static void leave(ServerLevel level, LivingEntity entity) {
        Exposure exposure = entity.getExistingDataOrNull(EXPOSURE);
        if (exposure != null)
            exposure.elapsedTicks = 0;
        RadiationLivingEntities scheduler = LEVELS.get(level);
        if (scheduler == null)
            return;
        Work work = scheduler.pending.remove(entity.getId());
        if (work != null)
            RadiationSourceIndex.cancelLiving(level, work.key);
        for (int i = 0; i < scheduler.candidateCount; i++) {
            if (scheduler.candidates[i] == entity) {
                int remaining = --scheduler.candidateCount - i;
                System.arraycopy(scheduler.candidates, i + 1, scheduler.candidates, i, remaining);
                scheduler.candidates[scheduler.candidateCount] = null;
                break;
            }
        }
    }

    static void close(ServerLevel level) {
        LEVELS.remove(level);
    }
}
