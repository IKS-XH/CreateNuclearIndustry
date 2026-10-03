package com.iksxh.create_nuclear_industry.heat;

import net.minecraft.nbt.CompoundTag;

/**
 * 服务端唯一换热账本；热/冷库存单位 mB，储备单位 HU，时间单位服务端 game tick。
 * 输入转换和冷罐占位原子提交；查询与模拟不结算。客户端只读取保存快照。
 */
public final class HeatExchangerState {
    public static final int CAPACITY = 4000;
    private int hot, cold, heat = -1, converted;
    private double reserve, flowFraction, lastRate;
    private long lastTick = -1;
    private boolean running;
    private String status = "no_load";

    /** 有界配置快照；密度复用 P1 的 HU/mB，不把 Create 数值热等级当作物理单位。 */
    public record Settings(int heatLevel, double huPerLevel, int bufferTicks, double density) {
        public double rate() { return heatLevel * huPerLevel; }
        public double capacity() { return rate() * bufferTicks; }
        public boolean valid() {
            return heatLevel >= 1 && heatLevel <= 18 && Double.isFinite(huPerLevel)
                    && huPerLevel > 0 && huPerLevel <= 1_000_000 && bufferTicks >= 1 && bufferTicks <= 1200
                    && Double.isFinite(density) && density > 0 && density <= 1_000_000
                    && Double.isFinite(rate() / density);
        }
    }

    public int hot() { return hot; }
    public int cold() { return cold; }
    public int heat() { return heat; }
    public int converted() { return converted; }
    public double reserve() { return reserve; }
    public String status() { return status; }
    public int remainingTicks() { return lastRate > 0 ? (int) Math.floor(reserve / lastRate) : 0; }

    /** 只返回实际容量内的接收量；simulate 不修改数量及时间。 */
    public int fillHot(int amount, boolean simulate) {
        int accepted = Math.min(Math.max(0, amount), CAPACITY - hot);
        if (!simulate) hot += accepted;
        return accepted;
    }

    /** 仅抽取已转换的冷液，热液不向外暴露排出入口。 */
    public int drainCold(int amount, boolean simulate) {
        int taken = Math.min(Math.max(0, amount), cold);
        if (!simulate) cold -= taken;
        return taken;
    }

    /**
     * 每个世界 tick 最多执行一次；先付本 tick 热，再补实际转换的热。
     * 预热达到完整储备后的下一 tick 才可供热，避免首 tick 免费发布。
     * 分数流量仅保留不足 1mB 部分，堵塞不会积攒可突发的整数流量债务。
     */
    public void tick(long now, boolean load, Settings cfg) {
        if (now == lastTick) return;
        heat = -1;
        converted = 0;
        if (lastTick >= 0 && now > lastTick + 1 && lastRate > 0)
            reserve = Math.max(0, reserve - (now - lastTick - 1) * lastRate);
        lastTick = now;
        if (!cfg.valid()) {
            reserve = 0;
            flowFraction = 0;
            running = false;
            lastRate = 0;
            status = "invalid";
            return;
        }
        lastRate = cfg.rate();
        // 配置改变仅限缩现存 HU，不按新密度重算库存热值，更不补齐新的容量。
        reserve = Math.min(reserve, cfg.capacity());
        if (!load) {
            reserve = Math.max(0, reserve - cfg.rate());
            if (reserve == 0) running = false;
            status = "no_load";
            return;
        }
        if (running && reserve >= cfg.rate()) {
            reserve -= cfg.rate();
            heat = cfg.heatLevel();
        } else if (running) {
            running = false;
        }
        double requested = cfg.rate() / cfg.density() + flowFraction;
        flowFraction = requested - Math.floor(requested);
        int budget = (int) Math.min(CAPACITY, Math.floor(requested));
        converted = Math.min(budget, Math.min(hot, CAPACITY - cold));
        hot -= converted;
        cold += converted;
        // 整 mB 量化可能越过储备上界；多付的尾差作为散热损失，不制造或延期热量。
        reserve = Math.min(cfg.capacity(), reserve + converted * cfg.density());
        if (!running && reserve >= cfg.capacity()) running = true;
        status = heat > 0 ? (converted < budget ? "residual" : "running")
                : cold == CAPACITY ? "blocked" : hot == 0 ? "empty" : "warming";
    }

    /** 精确保存 HU、分数流量及原世界时间；存盘和携物本身均不刷新余热窗口。 */
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("Hot", hot);
        tag.putInt("Cold", cold);
        tag.putDouble("ReserveHu", reserve);
        tag.putDouble("FlowFraction", flowFraction);
        tag.putDouble("LastRate", lastRate);
        tag.putLong("LastTick", lastTick);
        tag.putBoolean("Running", running);
        return tag;
    }

    /** NBT 不可信数值全部收敛到有限范围；恢复后必须经服务端 tick 重新验证负载再发热。 */
    public void load(CompoundTag tag) {
        hot = Math.clamp(tag.getInt("Hot"), 0, CAPACITY);
        cold = Math.clamp(tag.getInt("Cold"), 0, CAPACITY);
        reserve = finite(tag.getDouble("ReserveHu"), 21_600_000_000D);
        flowFraction = finite(tag.getDouble("FlowFraction"), Math.nextDown(1D));
        lastRate = finite(tag.getDouble("LastRate"), 18_000_000D);
        lastTick = tag.contains("LastTick") ? Math.max(-1, tag.getLong("LastTick")) : -1;
        running = tag.getBoolean("Running") && reserve > 0 && lastRate > 0;
        heat = -1;
        converted = 0;
    }

    private static double finite(double value, double max) {
        return Double.isFinite(value) ? Math.clamp(value, 0, max) : 0;
    }
}
