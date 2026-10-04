package com.iksxh.create_nuclear_industry.boiler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;

/**
 * 高压锅炉服务端唯一账本。水汽单位为 mB，炉体与加工尾量单位为 HU，时间为世界 tick。
 * 暖炉热与加工热互斥；同一物理口共享每tick流量预算，水汽库存仍由全部端口共享。
 */
public final class BoilerState {
    public static final int CAPACITY = 16000;
    public static final int FLOW_LIMIT = 256;
    public static final double WARM_HU_PER_SECTION = 3600;
    private int water, steam, legacyFillUsed, legacyDrainUsed, produced, vented;
    private long flowTick = Long.MIN_VALUE, lastTick = Long.MIN_VALUE, totalVented;
    private final Map<Long, Integer> fillUsedByPort = new HashMap<>();
    private final Map<Long, Integer> drainUsedByPort = new HashMap<>();
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

    /** 每个物理水口独立限流，所有口仍写入控制器共享水量；模拟不预留预算。 */
    public int fillWater(BlockPos port, int amount, boolean simulate, long now) {
        long key = port.asLong();
        int used = now == flowTick ? fillUsedByPort.getOrDefault(key, 0) + legacyFillUsed : 0;
        int accepted = Math.min(Math.max(0, amount), Math.min(CAPACITY - water, Math.max(0, FLOW_LIMIT - used)));
        if (!simulate && accepted > 0) {
            advanceFlow(now);
            water += accepted;
            fillUsedByPort.merge(key, accepted, Integer::sum);
        }
        return accepted;
    }

    /** 每个物理汽口独立限流，所有口仍从控制器共享汽量扣除；模拟不预留预算。 */
    public int drainSteam(BlockPos port, int amount, boolean simulate, long now) {
        long key = port.asLong();
        int used = now == flowTick ? drainUsedByPort.getOrDefault(key, 0) + legacyDrainUsed : 0;
        int taken = Math.min(Math.max(0, amount), Math.min(steam, Math.max(0, FLOW_LIMIT - used)));
        if (!simulate && taken > 0) {
            advanceFlow(now);
            steam -= taken;
            drainUsedByPort.merge(key, taken, Integer::sum);
        }
        return taken;
    }

    public int remainingFill(BlockPos port, long now) {
        int used = now == flowTick ? fillUsedByPort.getOrDefault(port.asLong(), 0) + legacyFillUsed : 0;
        return Math.min(CAPACITY - water, Math.max(0, FLOW_LIMIT - used));
    }
    public int remainingDrain(BlockPos port, long now) {
        int used = now == flowTick ? drainUsedByPort.getOrDefault(port.asLong(), 0) + legacyDrainUsed : 0;
        return Math.min(steam, Math.max(0, FLOW_LIMIT - used));
    }

    /** 结构段数改变时保留已付 HU，但新增段必须补足新暖炉上限后才恢复就绪。 */
    public void sectionsChanged(int sections) {
        double ceiling = Math.max(0, sections) * WARM_HU_PER_SECTION;
        warmHu = Math.min(warmHu, ceiling);
        if (warmHu < ceiling) ready = false;
    }

    private void advanceFlow(long now) {
        if (flowTick == now) return;
        flowTick = now;
        fillUsedByPort.clear();
        drainUsedByPort.clear();
        legacyFillUsed = legacyDrainUsed = 0;
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
        tag.putLong("FlowTick", flowTick);
        tag.putInt("FillUsed", 0); tag.putInt("DrainUsed", 0);
        tag.putInt("LegacyFillUsed", legacyFillUsed); tag.putInt("LegacyDrainUsed", legacyDrainUsed);
        tag.put("FillBudgets", saveBudgets(fillUsedByPort));
        tag.put("DrainBudgets", saveBudgets(drainUsedByPort));
        return tag;
    }

    public void load(CompoundTag tag) {
        preparedTick = Long.MIN_VALUE;
        rollbackPrepared = false;
        water = Math.clamp(tag.getInt("Water"), 0, CAPACITY);
        steam = Math.clamp(tag.getInt("Steam"), 0, CAPACITY);
        warmHu = finite(tag.getDouble("WarmHu"), 9 * WARM_HU_PER_SECTION);
        processHu = finite(tag.getDouble("ProcessHu"), Math.nextDown(1));
        ready = tag.getBoolean("Ready"); valveOpen = tag.getBoolean("ValveOpen");
        totalVented = Math.max(0, tag.getLong("TotalVented"));
        lastTick = tag.contains("LastTick") ? tag.getLong("LastTick") : Long.MIN_VALUE;
        flowTick = tag.contains("FlowTick") ? tag.getLong("FlowTick") : Long.MIN_VALUE;
        legacyFillUsed = legacyDrainUsed = 0;
        fillUsedByPort.clear();
        drainUsedByPort.clear();
        // 旧存档只有整炉预算；在原tick余下时间继续保守占用，tick前进后自然转为逐口额度。
        if (tag.contains("FillBudgets", Tag.TAG_LIST)) {
            loadBudgets(tag.getList("FillBudgets", Tag.TAG_COMPOUND), fillUsedByPort);
            legacyFillUsed = Math.clamp(tag.getInt("LegacyFillUsed"), 0, FLOW_LIMIT);
        }
        else legacyFillUsed = Math.clamp(tag.getInt("FillUsed"), 0, FLOW_LIMIT);
        if (tag.contains("DrainBudgets", Tag.TAG_LIST)) {
            loadBudgets(tag.getList("DrainBudgets", Tag.TAG_COMPOUND), drainUsedByPort);
            legacyDrainUsed = Math.clamp(tag.getInt("LegacyDrainUsed"), 0, FLOW_LIMIT);
        }
        else legacyDrainUsed = Math.clamp(tag.getInt("DrainUsed"), 0, FLOW_LIMIT);
    }

    private static ListTag saveBudgets(Map<Long, Integer> budgets) {
        ListTag result = new ListTag();
        budgets.forEach((port, used) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Port", port);
            entry.putInt("Used", Math.clamp(used, 0, FLOW_LIMIT));
            result.add(entry);
        });
        return result;
    }

    private static void loadBudgets(ListTag saved, Map<Long, Integer> budgets) {
        for (int index = 0; index < saved.size(); index++) {
            CompoundTag entry = saved.getCompound(index);
            int used = Math.clamp(entry.getInt("Used"), 0, FLOW_LIMIT);
            if (used > 0) budgets.put(entry.getLong("Port"), used);
        }
    }

    private static double finite(double value, double max) {
        return Double.isFinite(value) ? Math.clamp(value, 0, max) : 0;
    }
}
