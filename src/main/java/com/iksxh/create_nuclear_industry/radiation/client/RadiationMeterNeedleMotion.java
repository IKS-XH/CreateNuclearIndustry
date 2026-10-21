package com.iksxh.create_nuclear_industry.radiation.client;

import java.util.Objects;

/**
 * 仅保存客户端仪表针角，不参与辐射采样、剂量或网络权威状态。
 * 输入时钟为世界gameTime（tick），场强单位为穿戴减免前D/s，输出为模型+Z旋转角（度）。
 * 世界实例身份与维度键共同隔离显示缓存；pending从不刷新最后完整样本的时钟。
 */
public final class RadiationMeterNeedleMotion {
    public static final double FIELD_SCALE = 0.01;
    public static final double MAX_FIELD = 10.0;
    public static final double MIN_ANGLE = -65.0;
    public static final double MAX_ANGLE = 65.0;
    public static final double UNREAD_ANGLE = -105.0;
    public static final double RESPONSE_SECONDS = 0.3;
    public static final double SETTLE_EXPONENT = 3.0;
    public static final double TICK_SECONDS = 0.05;
    public static final long TIMEOUT_TICKS = 60;
    public static final long MAX_FUTURE_TICKS = 40;
    private static final double ALPHA = -Math.expm1(-SETTLE_EXPONENT * TICK_SECONDS / RESPONSE_SECONDS);

    /** 原同步结果的不可变显示输入；维度键可用ResourceLocation，本类不依赖Minecraft。 */
    public record Sample(Object dimension, long gameTime, double field, boolean complete) {}

    private Object world;
    private Object dimension;
    private long clock = Long.MIN_VALUE;
    private long lastCompleteTime = Long.MIN_VALUE;
    private Sample blockedAfterRollback;
    private double displayedField = Double.NaN;
    private double previous = UNREAD_ANGLE;
    private double current = UNREAD_ANGLE;
    private double target = UNREAD_ANGLE;
    private boolean paused;

    /** 无效值不是零场强；有限非负场强限幅后按低场强灵敏的非线性刻度映射。 */
    public static double angleForField(double field) {
        if (!Double.isFinite(field) || field < 0) return UNREAD_ANGLE;
        double fraction = Math.log1p(Math.min(field, MAX_FIELD) / FIELD_SCALE) / Math.log1p(MAX_FIELD / FIELD_SCALE);
        return MIN_ANGLE + (MAX_ANGLE - MIN_ANGLE) * fraction;
    }

    /**
     * 每个新世界tick最多推进一次；暂停不接受样本、不续鲜也不推进角度。
     * 时钟回退立即回未读，并屏蔽回退瞬间仍留在原只读getter中的旧快照，直到出现不同的有效complete。
     */
    public void tick(Object nextWorld, Object nextDimension, long gameTime, boolean isPaused, Sample sample) {
        if (nextWorld == null || nextDimension == null) {
            reset();
            return;
        }
        if (world != nextWorld || !Objects.equals(dimension, nextDimension)) {
            reset();
            world = nextWorld;
            dimension = nextDimension;
        } else if (clock != Long.MIN_VALUE && gameTime < clock) {
            reset();
            world = nextWorld;
            dimension = nextDimension;
            clock = gameTime;
            blockedAfterRollback = sample;
            paused = isPaused;
            return;
        }
        paused = isPaused;
        if (paused || gameTime == clock) return;
        clock = gameTime;
        if (validComplete(sample, nextDimension, gameTime)
                && !Objects.equals(sample, blockedAfterRollback)
                && (lastCompleteTime == Long.MIN_VALUE || sample.gameTime() > lastCompleteTime)) {
            lastCompleteTime = sample.gameTime();
            displayedField = Math.min(sample.field(), MAX_FIELD);
            blockedAfterRollback = null;
        }
        if (lastCompleteTime != Long.MIN_VALUE && gameTime - lastCompleteTime > TIMEOUT_TICKS) {
            lastCompleteTime = Long.MIN_VALUE;
            displayedField = Double.NaN;
        }
        target = hasReading() ? angleForField(displayedField) : UNREAD_ANGLE;
        previous = current;
        current += (target - current) * ALPHA;
    }

    private static boolean validComplete(Sample sample, Object dimension, long gameTime) {
        if (sample == null || !sample.complete() || !Objects.equals(sample.dimension(), dimension)
                || !Double.isFinite(sample.field()) || sample.field() < 0) return false;
        long age = gameTime - sample.gameTime();
        return age >= -MAX_FUTURE_TICKS && age <= TIMEOUT_TICKS;
    }

    /** 换世界或登出清空纯显示缓存；未读角位于测量弧外，不伪装成0D/s。 */
    public void reset() {
        world = null;
        dimension = null;
        clock = Long.MIN_VALUE;
        lastCompleteTime = Long.MIN_VALUE;
        blockedAfterRollback = null;
        displayedField = Double.NaN;
        previous = current = target = UNREAD_ANGLE;
        paused = false;
    }

    /** 帧间只在相邻tick角度间插值；暂停直接返回固定当前角，不接受非法partial造成NaN。 */
    public double angle(double partialTick) {
        if (paused) return current;
        double partial = Double.isFinite(partialTick) ? Math.clamp(partialTick, 0.0, 1.0) : 0.0;
        return previous + (current - previous) * partial;
    }

    /** 渲染可能早于换世界首tick；只读身份守卫立即屏蔽旧角，暂停帧不继续插值。 */
    public double angleForRender(Object currentWorld, Object currentDimension, boolean isPaused, double partialTick) {
        if (world != currentWorld || currentWorld == null || !Objects.equals(dimension, currentDimension)) return UNREAD_ANGLE;
        return isPaused ? current : angle(partialTick);
    }

    public boolean hasReading() { return lastCompleteTime != Long.MIN_VALUE; }
    public long lastCompleteTime() { return lastCompleteTime; }
    public double previousAngle() { return previous; }
    public double currentAngle() { return current; }
    public double targetAngle() { return target; }
}
