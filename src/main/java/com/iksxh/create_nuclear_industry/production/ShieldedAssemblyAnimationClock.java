package com.iksxh.create_nuclear_industry.production;

/** 客户端每台装配室独立的纯时钟；工时单位与服务端同步账本一致，不修改或发送状态。 */
public final class ShieldedAssemblyAnimationClock {
    private static final int SYNC_WORK = 256;
    private static final double MAX_RENDER_GAP_TICKS = 20;
    private String operation = "";
    private int work;
    private int receivedProgress;
    private double visualProgress;
    private double lastTick = Double.NaN;
    private boolean initialized;
    private boolean moving;
    /** 当前同步快照；RPM带符号，progress/work以工时计，output优先于可能滞后一tick的WORKING。 */
    public record Sample(String operation, int progress, int work, float rpm, boolean current,
                         boolean expanded, boolean working, boolean overloaded, boolean ready, boolean output) {}

    /** 视觉工时和归一化阶段；完成仅由同步输出/清零判定，不能靠插帧生成成品。 */
    public record Frame(double progress, double phase) {}

    /**
     * 接收客户端游戏tick及partialTick；暂停由调用方真实Minecraft状态提供。
     * 同一批次最多外推一份256工时包，且不触及完成值；短停冻住视觉工时，恢复不追停机时间。
     * 缺料、产出、工序/工时变化、权威清零与超过20tick的渲染空档重新建立本机基准。
     */
    public Frame update(Sample sample, double tick, boolean paused) {
        // 完成tick中WORKING可能仍为true，输出槽必须先于动力与插帧判定。
        if (!sample.current() || !sample.expanded() || !sample.ready() || sample.output()
                || !("manufacture".equals(sample.operation()) || "sealing".equals(sample.operation()))
                || sample.work() < 1 || sample.progress() < 0 || sample.progress() >= sample.work()) {
            initialized = false;
            moving = false;
            lastTick = tick;
            visualProgress = 0;
            return new Frame(0, 0);
        }
        double elapsed = tick - lastTick;
        boolean running = !paused && sample.working() && !sample.overloaded()
                && Float.isFinite(sample.rpm()) && Math.abs(sample.rpm()) >= 32;
        boolean reset = !initialized || !operation.equals(sample.operation()) || work != sample.work()
                || sample.progress() < receivedProgress
                || (!paused && Double.isFinite(tick)
                    && (!Double.isFinite(lastTick) || elapsed < 0 || elapsed > MAX_RENDER_GAP_TICKS));
        if (reset) {
            visualProgress = sample.progress();
        } else {
            // 先推进上一帧视觉工时，再和新样本校正；样本已含本间隔，不能先取max后重复计费。
            // 反转仍按abs计工时，分数RPM与服务端一样取整数。
            if (running && moving && Double.isFinite(elapsed) && elapsed > 0)
                visualProgress += elapsed * (int) Math.min(Math.abs(sample.rpm()), 256);
            visualProgress = Math.max(visualProgress, sample.progress());
            visualProgress = Math.min(visualProgress, Math.min((long) sample.progress() + SYNC_WORK,
                    sample.work() - 1));
        }
        initialized = true;
        operation = sample.operation();
        work = sample.work();
        receivedProgress = sample.progress();
        lastTick = tick;
        moving = running && Double.isFinite(tick);
        return new Frame(visualProgress, visualProgress / work);
    }
}
