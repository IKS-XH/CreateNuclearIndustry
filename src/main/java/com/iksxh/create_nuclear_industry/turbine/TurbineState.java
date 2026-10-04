package com.iksxh.create_nuclear_industry.turbine;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 服务端控制器唯一流体与动力账本：两库存单位 mB，流量单位 mB/t，应力容量单位 SU。
 * 不持有世界或 Create 网络；结构、红石及区块有效性由控制器作为 tick 的运行许可传入。
 */
public final class TurbineState {
    /** 一个合法档位的长度为转子数加两端面，直径限定已建模的三种奇数规格。 */
    public record Tier(int rotorCount, int ratedFlowMbPerTick, int inputCapacityMb, int exhaustCapacityMb,
                       int diameter) {
        /** 旧测试及调用入口的四参数兼容构造；正式配置始终传入独立直径键。 */
        public Tier(int rotorCount, int ratedFlowMbPerTick, int inputCapacityMb, int exhaustCapacityMb) {
            this(rotorCount, ratedFlowMbPerTick, inputCapacityMb, exhaustCapacityMb,
                    rotorCount == 6 ? 5 : rotorCount == 9 ? 7 : 3);
        }
        public int length() { return rotorCount + 2; }
        public boolean valid() {
            return rotorCount >= 3 && rotorCount <= 16 && TurbineGeometry.supportedDiameter(diameter)
                    && ratedFlowMbPerTick >= 1
                    && ratedFlowMbPerTick <= 10_000 && inputCapacityMb >= 1 && inputCapacityMb <= 1_000_000
                    && exhaustCapacityMb >= 1 && exhaustCapacityMb <= 1_000_000;
        }
    }

    /**
     * 配置快照不访问全局 TOML；运行前还要传入当前 Create 服务端实际转速上限。
     * 三档仅按配置中的转子数匹配，不能以未列出的中间长度形成第四档。
     */
    public record Settings(Tier shortTier, Tier mediumTier, Tier longTier, int rpm,
                           double suPerMbPerTick, int smoothingTicks, int inletPortFlowMbPerTick,
                           int exhaustPortFlowMbPerTick, double frontShare) {
        public static final Settings DEFAULT = new Settings(new Tier(3, 54, 4000, 4000, 3),
                new Tier(6, 108, 8000, 8000, 5), new Tier(9, 162, 12000, 12000, 7),
                256, 32768, 40, 256, 256, .5);

        public Tier tierForRotors(int rotorCount) {
            if (shortTier != null && shortTier.rotorCount() == rotorCount) return shortTier;
            if (mediumTier != null && mediumTier.rotorCount() == rotorCount) return mediumTier;
            if (longTier != null && longTier.rotorCount() == rotorCount) return longTier;
            return null;
        }

        /** 非法、重复或超过 Create 当前转速上限的设置须停机，不能偷偷退回默认档。 */
        public boolean valid(int createMaxRpm) {
            return shortTier != null && mediumTier != null && longTier != null
                    && shortTier.valid() && mediumTier.valid() && longTier.valid()
                    && shortTier.rotorCount() != mediumTier.rotorCount()
                    && shortTier.rotorCount() != longTier.rotorCount()
                    && mediumTier.rotorCount() != longTier.rotorCount()
                    && rpm >= 1 && rpm <= 65_536 && createMaxRpm >= rpm
                    && Double.isFinite(suPerMbPerTick) && suPerMbPerTick > 0 && suPerMbPerTick <= 1_000_000
                    && smoothingTicks >= 1 && smoothingTicks <= 1200
                    && inletPortFlowMbPerTick >= 1 && inletPortFlowMbPerTick <= 1_000_000
                    && exhaustPortFlowMbPerTick >= 1 && exhaustPortFlowMbPerTick <= 1_000_000
                    && Double.isFinite(frontShare) && frontShare >= 0 && frontShare <= 1
                    && Double.isFinite(10_000D * suPerMbPerTick);
        }
    }

    private Settings settings = Settings.DEFAULT;
    private Tier tier;
    private int createMaxRpm, input, exhaust, processed;
    private long lastTick = Long.MIN_VALUE, flowTick = Long.MIN_VALUE, processedInWindow;
    private int historyHead;
    private int[] history = new int[Settings.DEFAULT.smoothingTicks()];
    private final Map<Long, Integer> inletUsed = new HashMap<>();
    private final Map<Long, Integer> exhaustUsed = new HashMap<>();

    public Settings settings() { return settings; }
    public Tier tier() { return tier; }
    public int input() { return input; }
    public int exhaust() { return exhaust; }
    public int processed() { return processed; }
    public long processedInWindow() { return processedInWindow; }
    public int inletCapacity() { return tier == null ? 0 : tier.inputCapacityMb(); }
    public int exhaustCapacity() { return tier == null ? 0 : tier.exhaustCapacityMb(); }
    public int ratedFlowMbPerTick() { return tier == null ? 0 : tier.ratedFlowMbPerTick(); }
    public int rpm() { return settings.rpm(); }

    /**
     * C 控制器传入服务端快照、实测 Create 转速上限与本机转子数；同规格降容只限制新输入/加工空位。
     * 新成型或换档须另调用 canFormForNewTier，不能把它用于锁死已成型旧库存。
     */
    public void applySettings(Settings next, int rotorCount, int currentCreateMaxRpm) {
        Tier nextTier = next.tierForRotors(rotorCount);
        if (settings.smoothingTicks() != next.smoothingTicks()
                || Double.compare(settings.suPerMbPerTick(), next.suPerMbPerTick()) != 0
                || settings.rpm() != next.rpm() || tier == null || nextTier == null
                || tier.rotorCount() != nextTier.rotorCount() || tier.diameter() != nextTier.diameter()
                || tier.ratedFlowMbPerTick() != nextTier.ratedFlowMbPerTick()
                || !next.valid(currentCreateMaxRpm)) {
            history = new int[Math.clamp(next.smoothingTicks(), 1, 1200)];
            clearHistory();
        }
        settings = next;
        tier = nextTier;
        createMaxRpm = currentCreateMaxRpm;
    }

    /** 仅供新成型与换档预检；已成型原档在管理员降容后仍可排出旧库存。 */
    public boolean canFormForNewTier(int rotorCount) {
        Tier candidate = settings.tierForRotors(rotorCount);
        return settings.valid(createMaxRpm) && candidate != null
                && input <= candidate.inputCapacityMb() && exhaust <= candidate.exhaustCapacityMb();
    }

    /**
     * 服务端每世界 tick 最多成交一次。先检查排汽真实空位再按 1:1 扣入汽、加排汽；
     * 跳过的世界 tick 向窗口写零，不加工离线蒸汽。false 运行许可立即撤销全部历史 SU。
     */
    public int tick(long now, boolean operational) {
        if (!operational || !settings.valid(createMaxRpm) || tier == null) {
            stop();
            lastTick = now;
            return 0;
        }
        if (now == lastTick) return 0;
        advanceHistory(now);
        int moved = Math.min(tier.ratedFlowMbPerTick(),
                Math.min(input, Math.max(0, tier.exhaustCapacityMb() - exhaust)));
        input -= moved;
        exhaust += moved;
        processed = moved;
        history[historyHead] = moved;
        processedInWindow += moved;
        lastTick = now;
        return moved;
    }

    /** 红石停机、结构失效或卸载时由 C 立即调用；仅丢弃动力尾预算，不变更库存。 */
    public void stop() { clearHistory(); }

    public double totalSu() {
        return settings.valid(createMaxRpm) && tier != null
                ? processedInWindow * settings.suPerMbPerTick() / settings.smoothingTicks() : 0;
    }
    public double frontSu() { return totalSu() * settings.frontShare(); }
    public double rearSu() { return totalSu() - frontSu(); }

    /** 按物理进汽口独立限流，所有口进入同一库存；模拟不占空间或本 tick 额度。 */
    public int fillInput(long portKey, int amount, boolean simulate, long now) {
        if (!settings.valid(createMaxRpm) || tier == null) return 0;
        int accepted = Math.min(Math.max(0, amount), remainingInput(portKey, now));
        if (!simulate && accepted > 0) {
            advanceFlow(now);
            input += accepted;
            inletUsed.merge(portKey, accepted, Integer::sum);
        }
        return accepted;
    }

    /** 主动推送与被动抽取均调用这一入口，共享该物理排汽口同 tick 额度。 */
    public int drainExhaust(long portKey, int amount, boolean simulate, long now) {
        int taken = Math.min(Math.max(0, amount), remainingExhaust(portKey, now));
        if (!simulate && taken > 0) {
            advanceFlow(now);
            exhaust -= taken;
            exhaustUsed.merge(portKey, taken, Integer::sum);
        }
        return taken;
    }

    public int remainingInput(long portKey, long now) {
        if (!settings.valid(createMaxRpm) || tier == null) return 0;
        int used = now == flowTick ? inletUsed.getOrDefault(portKey, 0) : 0;
        return Math.min(Math.max(0, tier.inputCapacityMb() - input),
                Math.max(0, settings.inletPortFlowMbPerTick() - used));
    }

    public int remainingExhaust(long portKey, long now) {
        int used = now == flowTick ? exhaustUsed.getOrDefault(portKey, 0) : 0;
        return Math.min(exhaust, Math.max(0, settings.exhaustPortFlowMbPerTick() - used));
    }

    private void advanceFlow(long now) {
        if (flowTick == now) return;
        flowTick = now;
        inletUsed.clear();
        exhaustUsed.clear();
    }

    private void advanceHistory(long now) {
        if (lastTick == Long.MIN_VALUE || now <= lastTick || now - lastTick >= history.length) {
            clearHistory();
            return;
        }
        for (long step = 0; step < now - lastTick; step++) {
            historyHead = (historyHead + 1) % history.length;
            processedInWindow -= history[historyHead];
            history[historyHead] = 0;
        }
    }

    private void clearHistory() {
        java.util.Arrays.fill(history, 0);
        historyHead = 0;
        processedInWindow = 0;
        processed = 0;
    }

    /** 保存库存、最近结算 tick 与同 tick 物理口预算；已处理历史和 SU 故意不落盘。 */
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("Input", input);
        tag.putInt("Exhaust", exhaust);
        // 控制器在同一世界 tick 携物拆放后仍不得再次加工；此时间戳不是动力历史。
        tag.putLong("LastTick", lastTick);
        tag.putLong("FlowTick", flowTick);
        tag.put("InletBudgets", saveBudgets(inletUsed));
        tag.put("ExhaustBudgets", saveBudgets(exhaustUsed));
        return tag;
    }

    /** 缺失的旧字段按零处理，正数库存不按当前容量截断；C决定新规格能否成型。 */
    public void load(CompoundTag tag) {
        input = Math.max(0, tag.getInt("Input"));
        exhaust = Math.max(0, tag.getInt("Exhaust"));
        flowTick = tag.contains("FlowTick") ? tag.getLong("FlowTick") : Long.MIN_VALUE;
        inletUsed.clear();
        exhaustUsed.clear();
        if (tag.contains("InletBudgets", Tag.TAG_LIST))
            loadBudgets(tag.getList("InletBudgets", Tag.TAG_COMPOUND), inletUsed);
        if (tag.contains("ExhaustBudgets", Tag.TAG_LIST))
            loadBudgets(tag.getList("ExhaustBudgets", Tag.TAG_COMPOUND), exhaustUsed);
        // 旧NBT无该字段时保留原有首次结算语义；新格式拒绝同tick恢复后的第二笔交易。
        lastTick = tag.contains("LastTick") ? tag.getLong("LastTick") : Long.MIN_VALUE;
        clearHistory();
    }

    private static ListTag saveBudgets(Map<Long, Integer> budgets) {
        var list = new ListTag();
        budgets.forEach((port, used) -> {
            var entry = new CompoundTag();
            entry.putLong("Port", port);
            entry.putInt("Used", Math.max(0, used));
            list.add(entry);
        });
        return list;
    }

    private static void loadBudgets(ListTag list, Map<Long, Integer> budgets) {
        for (int index = 0; index < list.size(); index++) {
            var entry = list.getCompound(index);
            int used = Math.max(0, entry.getInt("Used"));
            if (used > 0) budgets.put(entry.getLong("Port"), used);
        }
    }
}
