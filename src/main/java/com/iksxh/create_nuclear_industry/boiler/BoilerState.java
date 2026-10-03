package com.iksxh.create_nuclear_industry.boiler;

import net.minecraft.nbt.CompoundTag;

/**
 * 高压锅炉服务端唯一账本。水汽单位为 mB，炉体与加工尾量单位为 HU，时间为世界 tick。
 * 暖炉热与加工热互斥，模拟流体事务不改变库存或共享流量预算。
 */
public final class BoilerState {
    public static final int CAPACITY = 16000;
    public static final int FLOW_LIMIT = 256;
    public static final double WARM_HU_PER_SECTION = 3600;
    private int water, steam, fillUsed, drainUsed, produced, vented;
    private long flowTick = Long.MIN_VALUE, lastTick = Long.MIN_VALUE, totalVented;
    private double warmHu, processHu;
    private boolean ready, valveOpen, valveBlocked;
    private long preparedTick = Long.MIN_VALUE;
    private boolean rollbackPrepared;

    public int water() { return water; }
    public int steam() { return steam; }
    public double warmHu() { return warmHu; }
    public double processHu() { return processHu; }
    public boolean ready() { return ready; }
    public boolean valveOpen() { return valveOpen; }
    public boolean valveBlocked() { return valveBlocked; }
    public long totalVented() { return totalVented; }
    public int produced() { return produced; }
    public int vented() { return vented; }

    /** 同一控制器的所有水口共用每 tick 256mB 额度；simulate 仅返回可接受量。 */
    public int fillWater(int amount, boolean simulate, long now) {
        int used = now == flowTick ? fillUsed : 0;
        int accepted = Math.min(Math.max(0, amount), Math.min(CAPACITY - water, FLOW_LIMIT - used));
        if (!simulate && accepted > 0) {
            advanceFlow(now);
            water += accepted;
            fillUsed += accepted;
        }
        return accepted;
    }

    /** 汽口共享每 tick 抽取上限；阀门排放单独计数，不占玩家抽汽额度。 */
    public int drainSteam(int amount, boolean simulate, long now) {
        int used = now == flowTick ? drainUsed : 0;
        int taken = Math.min(Math.max(0, amount), Math.min(steam, FLOW_LIMIT - used));
        if (!simulate && taken > 0) {
            advanceFlow(now);
            steam -= taken;
            drainUsed += taken;
        }
        return taken;
    }

    public int remainingFill(long now) { return Math.min(CAPACITY - water, FLOW_LIMIT - (now == flowTick ? fillUsed : 0)); }
    public int remainingDrain(long now) { return Math.min(steam, FLOW_LIMIT - (now == flowTick ? drainUsed : 0)); }

    private void advanceFlow(long now) {
        if (flowTick == now) return;
        flowTick = now;
        fillUsed = drainUsed = 0;
    }

    /** 返回本 tick 最多能完成的实际收热需求；无水、无汽空间时停止换热。 */
    public double demand(int sections) {
        if (sections < 1 || water < 1 || steam >= CAPACITY) return 0;
        double rating = sections * 18.0;
        double ceiling = sections * WARM_HU_PER_SECTION;
        if (!ready) return Math.min(rating, Math.max(0, ceiling - warmHu));
        return Math.min(rating, Math.max(0, Math.min(water, CAPACITY - steam) - processHu));
    }

    /** 在申请本 tick 热之前先结算卸载/停 tick 期间的散热，避免恢复首 tick 跳过暖炉。 */
    public void prepare(long now, int sections) {
        if (preparedTick == now) return;
        preparedTick = now;
        rollbackPrepared = lastTick != Long.MIN_VALUE && now < lastTick;
        double ceiling = Math.max(0, sections) * WARM_HU_PER_SECTION;
        if (rollbackPrepared) {
            warmHu = processHu = 0;
            ready = false;
        } else {
            long missing = lastTick == Long.MIN_VALUE || now <= lastTick ? 0 : now - lastTick - 1;
            warmHu = Math.max(0, Math.min(warmHu, ceiling) - missing * Math.max(0, sections) * 0.9);
            if (warmHu <= ceiling * 0.25) ready = false;
            if (ceiling > 0 && warmHu >= ceiling) ready = true;
        }
    }

    /**
     * 每个世界 tick 最多结算一次。已支付热先补暖炉，暖满后仅剩余热可参与产汽；
     * 未收到热时按实际流逝 tick 冷却，时间回退清除无法证明有效的余温和尾量。
     */
    public void tick(long now, int sections, double paidHu, boolean ventClear, boolean redstoneStop) {
        if (now == lastTick) return;
        prepare(now, sections);
        produced = vented = 0;
        double ceiling = Math.max(0, sections) * WARM_HU_PER_SECTION;
        lastTick = now;
        warmHu = Math.min(warmHu, ceiling);
        if (ceiling > 0 && warmHu >= ceiling) ready = true;
        if (warmHu <= ceiling * 0.25) ready = false;
        double paid = redstoneStop || rollbackPrepared ? 0 : Math.max(0, Math.min(sections * 18.0, Double.isFinite(paidHu) ? paidHu : 0));
        if (paid == 0) {
            warmHu = Math.max(0, warmHu - Math.max(0, sections) * 0.9);
            if (warmHu <= ceiling * 0.25) ready = false;
        }
        if (paid > 0 && !ready) {
            double warming = Math.min(paid, ceiling - warmHu);
            warmHu += warming;
            paid -= warming;
            if (warmHu >= ceiling && ceiling > 0) ready = true;
        }
        if (ready && paid > 0 && water > 0 && steam < CAPACITY) {
            processHu += paid;
            produced = Math.min(Math.min(water, CAPACITY - steam), (int) Math.floor(processHu));
            water -= produced;
            steam += produced;
            processHu -= produced;
        }
        if (steam >= CAPACITY * 0.9) valveOpen = true;
        if (steam <= CAPACITY * 0.8) valveOpen = false;
        if (valveOpen && ventClear) {
            vented = Math.max(0, Math.min(steam - (int) (CAPACITY * 0.8), FLOW_LIMIT));
            if (vented > 0) {
                steam -= vented;
                totalVented += vented;
            }
        }
        valveBlocked = valveOpen && !ventClear;
        if (steam <= CAPACITY * 0.8) valveOpen = false;
    }

    /** 保存绝对 tick 和有限 HU；载入不能刷新冷却期限或扩充库存。 */
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("Water", water); tag.putInt("Steam", steam);
        tag.putDouble("WarmHu", warmHu); tag.putDouble("ProcessHu", processHu);
        tag.putBoolean("Ready", ready); tag.putBoolean("ValveOpen", valveOpen);
        tag.putLong("TotalVented", totalVented); tag.putLong("LastTick", lastTick);
        tag.putLong("FlowTick", flowTick); tag.putInt("FillUsed", fillUsed); tag.putInt("DrainUsed", drainUsed);
        return tag;
    }

    public void load(CompoundTag tag) {
        preparedTick = Long.MIN_VALUE;
        rollbackPrepared = false;
        water = Math.clamp(tag.getInt("Water"), 0, CAPACITY);
        steam = Math.clamp(tag.getInt("Steam"), 0, CAPACITY);
        warmHu = finite(tag.getDouble("WarmHu"), 8 * WARM_HU_PER_SECTION);
        processHu = finite(tag.getDouble("ProcessHu"), Math.nextDown(1));
        ready = tag.getBoolean("Ready"); valveOpen = tag.getBoolean("ValveOpen");
        totalVented = Math.max(0, tag.getLong("TotalVented"));
        lastTick = tag.contains("LastTick") ? tag.getLong("LastTick") : Long.MIN_VALUE;
        flowTick = tag.contains("FlowTick") ? tag.getLong("FlowTick") : Long.MIN_VALUE;
        fillUsed = Math.clamp(tag.getInt("FillUsed"), 0, FLOW_LIMIT);
        drainUsed = Math.clamp(tag.getInt("DrainUsed"), 0, FLOW_LIMIT);
    }

    private static double finite(double value, double max) {
        return Double.isFinite(value) ? Math.clamp(value, 0, max) : 0;
    }
}
