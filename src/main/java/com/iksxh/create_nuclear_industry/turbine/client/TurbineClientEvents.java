package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.CreateNuclearIndustry;
import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.turbine.TurbineOutputShaftBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineRotorBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbineShaftBlock;
import com.iksxh.create_nuclear_industry.turbine.display.TurbineRuntimeSample;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/** 客户端MOD事件只负责模型/渲染注册和重载；运行tick在独立Game总线上处理。 */
@EventBusSubscriber(modid = CreateNuclearIndustry.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public final class TurbineClientEvents {
    private static final TurbineAnimationVisualState.Clock CLOCK = new TurbineAnimationVisualState.Clock();
    private static final TurbineAnimationVisualState.Phases<Long> SHAFT_PHASES = new TurbineAnimationVisualState.Phases<>();
    private static final Map<Long, ShaftTrack> SHAFTS = new HashMap<>();
    private static final TurbineAnimationVisualState.RotorPhases ROTOR_PHASES = new TurbineAnimationVisualState.RotorPhases();
    private static final Map<Long, RotorTrack> ROTORS = new HashMap<>();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static WeakReference<ClientLevel> ended = new WeakReference<>(null);
    private static long session;

    private TurbineClientEvents() {}

    @SubscribeEvent public static void registerModels(ModelEvent.RegisterGeometryLoaders event) {
        initializePartials();
    }

    /** 同步模型事件早于首次ModelManager准备；不得延迟到FMLClientSetup才创建partial。 */
    private static void initializePartials() {
        var blades = TurbineRotorRenderer.BLADES;
        var shafts = TurbineOutputShaftRenderer.SHAFTS;
    }

    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        initializePartials();
        event.registerBlockEntityRenderer(TurbineContent.ROTOR_BE.get(), TurbineRotorRenderer::new);
        event.registerBlockEntityRenderer(TurbineContent.OUTPUT_SHAFT_BE.get(), TurbineOutputShaftRenderer::new);
    }

    @SubscribeEvent public static void reloaded(ModelEvent.BakingCompleted event) {
        resetAnimation();
    }

    static double outputAngle(TurbineOutputShaftBlockEntity shaft, float partial) {
        var minecraft = Minecraft.getInstance();
        return outputAngle(shaft, minecraft.level, partial, minecraft.isPaused());
    }

    /** 只有当前已加载世界中的同一个BE可继续相位；未知/Ponder上下文安全静止，不查询邻近机主。 */
    static double outputAngle(TurbineOutputShaftBlockEntity shaft, ClientLevel current, float partial, boolean paused) {
        current = synchronizeWorld(current);
        long key = shaft.getBlockPos().asLong();
        if (!validShaft(shaft, current)) {
            ShaftTrack previous = SHAFTS.get(key);
            // 未知/Ponder锚点或已被替换的旧BE不能按同坐标误撤下当前世界中新BE的轨道。
            if (previous != null && previous.entity.get() == shaft) removeShaft(key);
            return 0;
        }
        ShaftTrack track = SHAFTS.get(key);
        if (track == null || !track.matches(shaft)) {
            removeShaft(key);
            track = new ShaftTrack(shaft);
            SHAFTS.put(key, track);
        }
        double time = CLOCK.time(partial, paused);
        if (!paused) SHAFT_PHASES.sample(key, TurbineOutputShaftRenderer.localRpm(track.facing, shaft.getSpeed()), time);
        return SHAFT_PHASES.angle(key, time);
    }

    /** 同次唯一runtimeDisplay结果同时提供几何和角度；empty为静止fallback，可靠0保留合法几何。 */
    record RotorPose(Optional<TurbineRuntimeSample> sample, double degrees) {}

    static RotorPose rotorPose(TurbineRotorBlockEntity rotor, float partial) {
        var minecraft = Minecraft.getInstance();
        return rotorPose(rotor, minecraft.level, partial, minecraft.isPaused());
    }

    static RotorPose rotorPose(TurbineRotorBlockEntity rotor, ClientLevel current, float partial, boolean paused) {
        current = synchronizeWorld(current);
        long key = rotor.getBlockPos().asLong();
        Optional<TurbineRuntimeSample> runtime = runtimeSample(rotor, current);
        if (runtime.isEmpty()) {
            RotorTrack previous = ROTORS.get(key);
            if (previous != null && previous.entity.get() == rotor) removeRotor(key);
            return new RotorPose(Optional.empty(), 0);
        }
        TurbineRuntimeSample sample = runtime.orElseThrow();
        var identity = TurbineAnimationVisualState.identity(session, sample);
        RotorTrack track = ROTORS.get(key);
        if (track == null || !track.matches(rotor, sample, identity)) {
            removeRotor(key);
            ROTORS.put(key, new RotorTrack(rotor, sample, identity));
        }
        // sample.rpm已经是局部+Z；暂停只冻结时间，可信性仍由唯一runtimeDisplay整包负责。
        double angle = ROTOR_PHASES.sample(identity, sample, CLOCK.time(partial, paused));
        return new RotorPose(runtime, angle);
    }

    private static Optional<TurbineRuntimeSample> runtimeSample(TurbineRotorBlockEntity rotor, ClientLevel current) {
        if (current == null || rotor == null || rotor.isRemoved() || rotor.getLevel() != current
                || !current.hasChunkAt(rotor.getBlockPos()) || current.getBlockEntity(rotor.getBlockPos()) != rotor)
            return Optional.empty();
        // 不查询机主/邻轴，不调用前置的publish、accept或续租入口，不用外观为旧旋转背书。
        return rotor.runtimeDisplay();
    }

    /** 客户端Post暂停时冻结时间/角度，仍核可靠性并接纳可信Rotor整包；Output暂停不采样新网速。 */
    static void tickWorld(ClientLevel current, boolean paused) {
        current = synchronizeWorld(current);
        if (current == null) return;
        CLOCK.tick(paused);
        double time = CLOCK.time(0, paused);
        for (Iterator<Map.Entry<Long, ShaftTrack>> iterator = SHAFTS.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            ShaftTrack track = entry.getValue();
            TurbineOutputShaftBlockEntity shaft = track.entity.get();
            if (!validShaft(shaft, current) || !track.matches(shaft)) {
                SHAFT_PHASES.remove(entry.getKey());
                iterator.remove();
            } else if (!paused) {
                // 可见性不是失效：可信同BE即使长期离屏仍逐tick锚定速度和停转角，恢复时不重建相位。
                SHAFT_PHASES.sample(entry.getKey(), TurbineOutputShaftRenderer.localRpm(track.facing, shaft.getSpeed()), time);
            }
        }
        Set<TurbineAnimationVisualState.RotorIdentity> remaining = new HashSet<>();
        Map<Long, RotorTrack> replacements = new HashMap<>();
        Map<Long, TurbineRuntimeSample> samples = new HashMap<>();
        for (Iterator<Map.Entry<Long, RotorTrack>> iterator = ROTORS.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            RotorTrack track = entry.getValue();
            TurbineRotorBlockEntity rotor = track.entity.get();
            Optional<TurbineRuntimeSample> runtime = runtimeSample(rotor, current);
            if (runtime.isEmpty()) {
                iterator.remove();
                continue;
            }
            TurbineRuntimeSample sample = runtime.orElseThrow();
            var identity = TurbineAnimationVisualState.identity(session, sample);
            samples.put(entry.getKey(), sample);
            if (!track.matches(rotor, sample, identity)) {
                // 先撤旧成员，不能直接替换value让唯一成员继承旧角度；暂存value仍只弱持BE。
                iterator.remove();
                replacements.put(entry.getKey(), new RotorTrack(rotor, sample, identity));
            } else remaining.add(identity);
        }
        // 迭代结束后再绑定；与BER撤销等价，其他仍可信旧成员可保留整机共享相位。
        ROTOR_PHASES.retain(remaining);
        ROTORS.putAll(replacements);
        Set<TurbineAnimationVisualState.RotorIdentity> reliable = new HashSet<>();
        for (var entry : samples.entrySet()) {
            RotorTrack track = ROTORS.get(entry.getKey());
            reliable.add(track.identity);
            ROTOR_PHASES.sample(track.identity, entry.getValue(), time);
        }
        ROTOR_PHASES.retain(reliable);
    }

    private static boolean validShaft(TurbineOutputShaftBlockEntity shaft, ClientLevel current) {
        if (current == null || shaft == null || shaft.isRemoved() || shaft.getLevel() != current
                || !current.hasChunkAt(shaft.getBlockPos()) || current.getBlockEntity(shaft.getBlockPos()) != shaft) return false;
        var state = shaft.getBlockState();
        return state.is(TurbineContent.OUTPUT_SHAFT.get()) && state.getValue(TurbinePartBlock.LOCATED);
    }

    private static ClientLevel synchronizeWorld(ClientLevel current) {
        // Unload后Minecraft.level可能短暂仍指旧对象；弱结束标记禁止相位会话重新打开。
        if (current != null && current == ended.get()) current = null;
        if (world.get() != current) {
            resetAnimation();
            world = new WeakReference<>(current);
        }
        return current;
    }

    private static void removeShaft(long key) {
        SHAFTS.remove(key);
        SHAFT_PHASES.remove(key);
    }

    private static void resetAnimation() {
        SHAFTS.clear();
        SHAFT_PHASES.clear();
        ROTORS.clear();
        ROTOR_PHASES.clear();
        CLOCK.clear();
        session++;
    }

    static void clearWorld() {
        resetAnimation();
        world = new WeakReference<>(null);
    }

    /** 只撤销卸载区块中的本轴；坐标为值对象，清理不获取或加载其他区块。 */
    private static void chunkUnloaded(ClientLevel current, ChunkPos chunk) {
        if (current != world.get()) return;
        for (Iterator<Long> iterator = SHAFTS.keySet().iterator(); iterator.hasNext();) {
            long key = iterator.next();
            BlockPos pos = BlockPos.of(key);
            if ((pos.getX() >> 4) != chunk.x || (pos.getZ() >> 4) != chunk.z) continue;
            SHAFT_PHASES.remove(key);
            iterator.remove();
        }
        for (Iterator<Map.Entry<Long, RotorTrack>> iterator = ROTORS.entrySet().iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            if (!entry.getValue().chunks.contains(chunk)) continue;
            iterator.remove();
        }
        ROTOR_PHASES.retain(rotorIdentities());
    }

    private static Set<TurbineAnimationVisualState.RotorIdentity> rotorIdentities() {
        Set<TurbineAnimationVisualState.RotorIdentity> result = new HashSet<>();
        // 某列empty时也立即核其余已登记弱成员，不能让尚未绘制的旧空绑定保住可复活的组角度。
        for (Iterator<RotorTrack> iterator = ROTORS.values().iterator(); iterator.hasNext();) {
            RotorTrack track = iterator.next();
            TurbineRotorBlockEntity rotor = track.entity.get();
            Optional<TurbineRuntimeSample> runtime = runtimeSample(rotor, world.get());
            if (runtime.isEmpty() || !track.matches(rotor, runtime.orElseThrow(),
                    TurbineAnimationVisualState.identity(session, runtime.orElseThrow()))) iterator.remove();
            else result.add(track.identity);
        }
        return result;
    }

    private static void removeRotor(long key) {
        ROTORS.remove(key);
        ROTOR_PHASES.retain(rotorIdentities());
    }

    /** 成员/index只做本BE绑定校验，整机身份和区块均为值对象，value不强持BE或世界。 */
    private static final class RotorTrack {
        private final WeakReference<TurbineRotorBlockEntity> entity;
        private final TurbineAnimationVisualState.RotorIdentity identity;
        private final UUID member;
        private final int index;
        private final Set<ChunkPos> chunks;

        private RotorTrack(TurbineRotorBlockEntity rotor, TurbineRuntimeSample sample,
                           TurbineAnimationVisualState.RotorIdentity identity) {
            entity = new WeakReference<>(rotor);
            this.identity = identity;
            member = sample.member();
            index = sample.index();
            chunks = sample.requiredChunks();
        }

        private boolean matches(TurbineRotorBlockEntity rotor, TurbineRuntimeSample sample,
                                TurbineAnimationVisualState.RotorIdentity identity) {
            return entity.get() == rotor && this.identity.equals(identity) && member.equals(sample.member()) && index == sample.index();
        }
    }

    /** value仅弱引用BE，角度/朝向/端位不经闭包反向强持世界或BE。 */
    private static final class ShaftTrack {
        private final WeakReference<TurbineOutputShaftBlockEntity> entity;
        private final Direction facing;
        private final TurbineShaftBlock.End end;

        private ShaftTrack(TurbineOutputShaftBlockEntity shaft) {
            entity = new WeakReference<>(shaft);
            facing = shaft.getBlockState().getValue(TurbinePartBlock.MACHINE_FACING);
            end = shaft.getBlockState().getValue(TurbineShaftBlock.END);
        }

        private boolean matches(TurbineOutputShaftBlockEntity shaft) {
            return entity.get() == shaft && facing == shaft.getBlockState().getValue(TurbinePartBlock.MACHINE_FACING)
                    && end == shaft.getBlockState().getValue(TurbineShaftBlock.END);
        }
    }

    /** 物理客户端Game总线处理tick/卸载，不混入MOD总线或服务端权威状态。 */
    @EventBusSubscriber(modid = CreateNuclearIndustry.MOD_ID, value = Dist.CLIENT)
    public static final class GameEvents {
        private GameEvents() {}

        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            var minecraft = Minecraft.getInstance();
            tickWorld(minecraft.level, minecraft.isPaused());
        }

        @SubscribeEvent public static void unloaded(LevelEvent.Unload event) {
            if (event.getLevel() == world.get()) {
                ended = new WeakReference<>(world.get());
                clearWorld();
            }
        }

        @SubscribeEvent public static void chunkUnloaded(ChunkEvent.Unload event) {
            if (event.getLevel() instanceof ClientLevel current)
                TurbineClientEvents.chunkUnloaded(current, event.getChunk().getPos());
        }
    }
}
