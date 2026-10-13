package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.turbine.display.TurbineRuntimeSample;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端专用累计相位；RPM输入只来自可靠显示接口或Create真实网速，输出为局部旋转度数。
 * 不持有世界、方块实体或服务器状态，不预测动力、惯性及耗汽；身份撤销后不得沿用旧角度。
 */
public final class TurbineAnimationVisualState {
    private static final double DEGREES_PER_RPM_TICK = .3;

    private TurbineAnimationVisualState() {}

    /** 自有客户端时钟以tick计时；暂停既不增加tick，也不重新采样partial，重载可整体撤销。 */
    public static final class Clock {
        private long ticks;
        private double lastRenderTime;

        public void tick(boolean paused) { if (!paused) ticks++; }

        /** partial以0..1计；同一tick内偶发较小的partial不能让已经提交的相位回退。 */
        public double time(float partial, boolean paused) {
            if (!paused) {
                double fraction = Float.isFinite(partial) ? Math.clamp(partial, 0, 1) : 0;
                lastRenderTime = Math.max(lastRenderTime, ticks + fraction);
            }
            return lastRenderTime;
        }

        public long ticks() { return ticks; }

        public void clear() { ticks = 0; lastRenderTime = 0; }
    }

    /**
     * 每个不可变显示身份共享一个累计相位，调用者负责按tick采样与撤销身份。
     * key只能使用坐标/会话/代次等值对象，不得以强引用方块实体或世界作为key。
     */
    public static final class Phases<K> {
        private final Map<K, Phase> values = new HashMap<>();

        /** 先以旧速度推进到样本时间，再修改速度；换速不能用新RPM重算已经过去的整个时间。 */
        public double sample(K key, float signedRpm, double time) {
            if (!Double.isFinite(time)) {
                values.remove(key);
                return 0;
            }
            Phase phase = values.computeIfAbsent(key, unused -> new Phase(time));
            if (time < phase.sampledAt) {
                phase = new Phase(time);
                values.put(key, phase);
            }
            phase.degrees = phase.angle(time);
            phase.sampledAt = time;
            phase.rpm = Float.isFinite(signedRpm) ? signedRpm : 0;
            return phase.degrees;
        }

        /** 只插值当前可靠样本，不修改累计基准；缺失身份返回静止fallback角度0。 */
        public double angle(K key, double time) {
            Phase phase = values.get(key);
            return phase == null ? 0 : phase.angle(time);
        }

        public void remove(K key) { values.remove(key); }

        public void clear() { values.clear(); }
    }

    /** 整机显示身份；世界会话在换世界/重载时重置，成员UUID和index不得分裂各列共享相位。 */
    public record RotorIdentity(long session, String dimension, long owner, UUID generation, int epoch) {}

    public static RotorIdentity identity(long session, TurbineRuntimeSample sample) {
        return new RotorIdentity(session, sample.dimension(), sample.owner().asLong(), sample.generation(), sample.epoch());
    }

    /**
     * 已可靠整包的整机累计相位；serverTick仅比较包的新旧，绝不参与客户端时间或租约年龄。
     * 缺全部成员时撤角度但保留数值排序水位，避免另一列较旧的可信包复活已撤销旋转。
     */
    public static final class RotorPhases {
        private final Map<RotorIdentity, Group> groups = new HashMap<>();
        private final Phases<RotorIdentity> phases = new Phases<>();

        public double sample(RotorIdentity identity, TurbineRuntimeSample sample, double time) {
            Group group = groups.computeIfAbsent(identity, unused -> new Group());
            if (group.latest == null || sample.serverTick() > group.latest.serverTick()) {
                group.latest = sample;
                group.active = true;
            } else if (sample.serverTick() == group.latest.serverTick()
                    && !sameGroupSample(group.latest, sample)) {
                // 同tick跨列共享字段冲突不可由绘制顺序裁决；保持静止直到更晚的完整样本。
                revoke(identity);
            }
            if (!group.active) return 0;
            return phases.sample(identity, group.latest.rpm(), time);
        }

        /** 仅撤销角度，排序水位保留到当前世界会话结束；旧同tick或更早的别列包不能重新激活。 */
        public void revoke(RotorIdentity identity) {
            Group group = groups.get(identity);
            if (group != null) group.active = false;
            phases.remove(identity);
        }

        /** 调用方每tick只提交仍可靠成员的身份集合，空组立即撤销；这里不判断BE或续租。 */
        public void retain(Set<RotorIdentity> reliable) {
            for (RotorIdentity identity : groups.keySet()) if (!reliable.contains(identity)) revoke(identity);
        }

        public void clear() { groups.clear(); phases.clear(); }

        private static boolean sameGroupSample(TurbineRuntimeSample first, TurbineRuntimeSample second) {
            return first.front().equals(second.front()) && first.facing() == second.facing()
                    && first.diameter() == second.diameter() && first.rotors() == second.rotors()
                    && Float.compare(first.rpm(), second.rpm()) == 0;
        }

        /** 全部字段都是整包值对象；不持BE、世界、查询器或反向强引用闭包。 */
        private static final class Group {
            private TurbineRuntimeSample latest;
            private boolean active;
        }
    }

    private static final class Phase {
        private double degrees, sampledAt;
        private float rpm;

        private Phase(double time) { sampledAt = time; }

        private double angle(double time) {
            double elapsed = Double.isFinite(time) ? Math.max(0, time - sampledAt) : 0;
            double result = (degrees + rpm * DEGREES_PER_RPM_TICK * elapsed) % 360;
            return result < 0 ? result + 360 : result;
        }
    }
}
